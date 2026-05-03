/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.gui.playlist.source;

import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.channel.rotation.ChannelRotationMonitor;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Frequency editor that supports editing single or multiple frequencies.
 */
public class FrequencyEditor extends SourceConfigurationEditor<SourceConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(FrequencyEditor.class);
    private static final String NONE = "(none)           ";
    private TunerManager mTunerManager;
    private FrequencyBox mPrimaryFrequencyBox;
    private ObservableList<FrequencyBox> mFrequencyBoxes = FXCollections.observableArrayList();
    private ComboBox<String> mPreferredTunerComboBox;
    private VBox mFrequencyBoxContainer;
    private Spinner<Integer> mChannelRotationDelaySpinner;
    private FrequencyHoldSelectionListener mFrequencyHoldSelectionListener;
    private boolean mUpdatingHoldSelection;
    private boolean mAllowMultipleFrequencies = false;
    private int mFrequencyRotationDefault = ChannelRotationMonitor.CHANNEL_ROTATION_DELAY_DEFAULT;
    private int mFrequencyRotationMinimum = ChannelRotationMonitor.CHANNEL_ROTATION_DELAY_MINIMUM;
    private int mFrequencyRotationMaximum = ChannelRotationMonitor.CHANNEL_ROTATION_DELAY_MAXIMUM;

    /**
     * Constructs an instance with support for multiple frequency values.  The min and max rotation delay values
     * define constraints for the channel rotation delay monitor, how long the monitor dwells on each frequency in a
     * multi-frequency configuration.
     *
     * @param tunerManager for accessing list of tuners to select a preferred tuner
     * @param minRotationDelay in milliseconds for channel/frequency dwell
     * @param maxRotationDelay in milliseconds for channel/frequency dwell
     */
    public FrequencyEditor(TunerManager tunerManager, int minRotationDelay, int maxRotationDelay, int defaultRotationDelay)
    {
        mTunerManager = tunerManager;
        mAllowMultipleFrequencies = true;
        mFrequencyRotationMinimum = minRotationDelay;
        mFrequencyRotationMaximum = maxRotationDelay;
        mFrequencyRotationDefault = defaultRotationDelay;
        init();
    }

    /**
     * Constructs an instance with a default behavior of single frequency support only.
     * @param tunerManager for accessing list of tuners to select a preferred tuner
     */
    public FrequencyEditor(TunerManager tunerManager)
    {
        mTunerManager = tunerManager;
        init();
    }

    /**
     * Sets a listener to receive frequency hold changes as soon as the user checks or unchecks a hold box.
     */
    public void setFrequencyHoldSelectionListener(FrequencyHoldSelectionListener listener)
    {
        mFrequencyHoldSelectionListener = listener;
    }

    private void init()
    {
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(10,10,10,10));
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(mAllowMultipleFrequencies ? "Frequencies (MHz)" : "Frequency (MHz)");
        label.setAlignment(Pos.BASELINE_RIGHT);
        hBox.getChildren().addAll(label, getFrequencyBoxContainer());

        Label preferredTunerLabel = new Label("Preferred Tuner");
        HBox tunerBox = new HBox();
        tunerBox.setAlignment(Pos.CENTER_LEFT);
        tunerBox.setSpacing(10);
        tunerBox.getChildren().addAll(preferredTunerLabel, getPreferredTunerComboBox());

        VBox vbox = new VBox();
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(0,0,0,20));

        if(mAllowMultipleFrequencies)
        {
            Label frequencyRotationLabel = new Label("Frequency Rotation Delay (ms)");
            HBox frequencyBox = new HBox();
            frequencyBox.setAlignment(Pos.CENTER_LEFT);
            frequencyBox.setSpacing(10);
            frequencyBox.getChildren().addAll(frequencyRotationLabel, getFrequencyRotationDelaySpinner());
            getFrequencyRotationDelaySpinner().disableProperty().bind(Bindings.greaterThan(2, Bindings.size(mFrequencyBoxes)));

            vbox.getChildren().addAll(tunerBox, frequencyBox);
        }
        else
        {
            vbox.getChildren().addAll(tunerBox);
        }

        hBox.getChildren().add(vbox);

        setAlignment(Pos.TOP_LEFT);
        getChildren().add(hBox);
    }

    /**
     * Sets the disabled status for each of the frequency boxes and the preferred tuner combo box.
     * @param disable true to disable each of the controls or false to enable.
     */
    @Override
    public void disable(boolean disable)
    {
        for(FrequencyBox frequencyBox: mFrequencyBoxes)
        {
            frequencyBox.disable(disable);
        }

        getPreferredTunerComboBox().setDisable(disable);
    }

    /**
     * Saves the current state of the editor to the source configuration which can then be accessed by invoking
     * the getSourceConfiguration() method.
     *
     * Note: the source configuration will be a single frequency config if the editor has only a single frequency or
     * if the single frequency is not specified.  It will create/use a multiple frequency configuration if the editor
     * has more than one frequency listed.
     */
    @Override
    public void save()
    {
        String preferredTuner = getPreferredTunerComboBox().getSelectionModel().getSelectedItem();

        //Remove the preferred tuner value if it contains the default 'none' value
        if(preferredTuner != null && preferredTuner.contentEquals(NONE))
        {
            preferredTuner = null;
        }

        List<Long> frequencies = getFrequencies();

        if(frequencies.size() <= 1)
        {
            SourceConfigTuner sourceConfigTuner = null;

            if(getSourceConfiguration() instanceof SourceConfigTuner)
            {
                sourceConfigTuner = (SourceConfigTuner)getSourceConfiguration();
            }
            else
            {
                sourceConfigTuner = new SourceConfigTuner();
            }

            if(frequencies.size() == 1)
            {
                sourceConfigTuner.setFrequency(frequencies.get(0));
            }
            else
            {
                sourceConfigTuner.setFrequency(0);
            }

            sourceConfigTuner.setPreferredTuner(preferredTuner);
            setSourceConfiguration(sourceConfigTuner);
        }
        else
        {
            SourceConfigTunerMultipleFrequency sourceConfigMulti = null;

            if(getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency)
            {
                sourceConfigMulti = (SourceConfigTunerMultipleFrequency)getSourceConfiguration();
            }
            else
            {
                sourceConfigMulti = new SourceConfigTunerMultipleFrequency();
            }

            sourceConfigMulti.setFrequencies(frequencies);
            sourceConfigMulti.setFrequencyLabels(getFrequencyLabels());
            sourceConfigMulti.setPreferredTuner(preferredTuner);
            sourceConfigMulti.setFrequencyRotationDelay(getFrequencyRotationDelaySpinner().getValue());

            Long frequencyHold = getHeldFrequency();
            boolean frequencyHoldEnabled = frequencyHold != null;
            sourceConfigMulti.setFrequencyHoldEnabled(frequencyHoldEnabled && frequencyHold != null);
            sourceConfigMulti.setFrequencyHold(frequencyHold);
            setSourceConfiguration(sourceConfigMulti);
        }
    }

    /**
     * Loads the source configuration into the editor.
     * @param sourceConfiguration to load
     */
    @Override
    public void setSourceConfiguration(SourceConfiguration sourceConfiguration)
    {
        super.setSourceConfiguration(sourceConfiguration);

        String preferredTuner = null;

        disable(sourceConfiguration == null);

        getFrequencyRotationDelaySpinner().getValueFactory()
            .setValue(ChannelRotationMonitor.CHANNEL_ROTATION_DELAY_MINIMUM);

        if(sourceConfiguration == null)
        {
            setFrequencies(Collections.emptyList());
            getPreferredTunerComboBox().setDisable(true);
            getPreferredTunerComboBox().getSelectionModel().select(null);
            updateFrequencyHoldOptions(null);
        }
        else if(sourceConfiguration instanceof SourceConfigTuner)
        {
            SourceConfigTuner sourceConfigTuner = (SourceConfigTuner)sourceConfiguration;
            setFrequency(sourceConfigTuner.getFrequency());
            preferredTuner = sourceConfigTuner.getPreferredTuner();
            updateFrequencyHoldOptions(null);
        }
        else if(sourceConfiguration instanceof SourceConfigTunerMultipleFrequency)
        {
            SourceConfigTunerMultipleFrequency sourceMulti = (SourceConfigTunerMultipleFrequency)sourceConfiguration;
            setFrequencies(sourceMulti.getFrequencies());
            setFrequencyLabels(sourceMulti.getFrequencyLabels());
            preferredTuner = sourceMulti.getPreferredTuner();

            int rotationDelay = sourceMulti.getFrequencyRotationDelay();

            if(rotationDelay < mFrequencyRotationMinimum)
            {
                rotationDelay = mFrequencyRotationMinimum;

                //For backward compatibility, we'll automatically update the value in the source config
                sourceMulti.setFrequencyRotationDelay(rotationDelay);
            }
            if(rotationDelay > mFrequencyRotationMaximum)
            {
                rotationDelay = mFrequencyRotationMaximum;

                //For backward compatibility, we'll automatically update the value in the source config
                sourceMulti.setFrequencyRotationDelay(rotationDelay);
            }

            getFrequencyRotationDelaySpinner().getValueFactory().setValue(rotationDelay);
            setHeldFrequency(sourceMulti.isFrequencyHoldEnabled() ? sourceMulti.getFrequencyHold() : null);
        }
        else
        {
            setFrequencies(Collections.emptyList());
            getPreferredTunerComboBox().setDisable(false);
            getPreferredTunerComboBox().getSelectionModel().select(null);
            updateFrequencyHoldOptions(null);
        }

        updatePreferredTuners();

        if(preferredTuner != null)
        {
            if(!getPreferredTunerComboBox().getItems().contains(preferredTuner))
            {
                getPreferredTunerComboBox().getItems().add(preferredTuner);
            }

            getPreferredTunerComboBox().getSelectionModel().select(preferredTuner);
        }
        else
        {
            getPreferredTunerComboBox().getSelectionModel().select(NONE);
        }

        modifiedProperty().set(false);
    }

    /**
     * Channel rotation monitor delay value.  This dictates how long the decoder will remain on each frequency before
     * rotating to the next frequency in the list
     * @return spinner
     */
    private Spinner<Integer> getFrequencyRotationDelaySpinner()
    {
        if(mChannelRotationDelaySpinner == null)
        {
            mChannelRotationDelaySpinner = new Spinner();
            mChannelRotationDelaySpinner.setTooltip(
                new Tooltip("Delay on each frequency before rotating to next when seeking to next active channel frequency"));
            mChannelRotationDelaySpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(mFrequencyRotationMinimum,
                mFrequencyRotationMaximum, mFrequencyRotationMinimum, 50);
            mChannelRotationDelaySpinner.setValueFactory(svf);
            mChannelRotationDelaySpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mChannelRotationDelaySpinner;
    }

    /**
     * Updates available hold frequency choices from the current frequency list.
     */
    private void updateFrequencyHoldOptions(Long requestedSelection)
    {
        if(!mAllowMultipleFrequencies)
        {
            return;
        }

        setHeldFrequency(requestedSelection == null ? getHeldFrequency() : requestedSelection);
        updateFrequencyHoldControls();
    }

    /**
     * Enables or disables hold controls based on the current frequency list and checkbox state.
     */
    private void updateFrequencyHoldControls()
    {
        if(mAllowMultipleFrequencies)
        {
            boolean hasMultipleFrequencies = getFrequencies().size() > 1;

            for(FrequencyBox frequencyBox: mFrequencyBoxes)
            {
                frequencyBox.getHoldCheckBox().setDisable(!hasMultipleFrequencies || frequencyBox.getFrequencyField().get() <= 0);
            }
        }
    }

    /**
     * Gets the current held frequency.
     */
    private Long getHeldFrequency()
    {
        for(FrequencyBox frequencyBox: mFrequencyBoxes)
        {
            if(frequencyBox.getHoldCheckBox().isSelected())
            {
                long frequency = frequencyBox.getFrequencyField().get();

                if(frequency > 0)
                {
                    return frequency;
                }
            }
        }

        return null;
    }

    /**
     * Selects the hold checkbox for the matching frequency and clears all others.
     */
    private void setHeldFrequency(Long frequency)
    {
        boolean matched = false;

        for(FrequencyBox frequencyBox: mFrequencyBoxes)
        {
            boolean selected = frequency != null && frequencyBox.getFrequencyField().get() == frequency && !matched;
            mUpdatingHoldSelection = true;
            frequencyBox.setHoldSelected(selected);
            mUpdatingHoldSelection = false;
            matched |= selected;
        }
    }

    /**
     * Processes a user hold checkbox selection.
     */
    private void selectHoldFrequency(FrequencyBox selectedFrequencyBox, boolean selected)
    {
        long frequency = selectedFrequencyBox.getFrequencyField().get();

        for(FrequencyBox frequencyBox: mFrequencyBoxes)
        {
            if(frequencyBox != selectedFrequencyBox)
            {
                mUpdatingHoldSelection = true;
                frequencyBox.setHoldSelected(false);
                mUpdatingHoldSelection = false;
            }
        }

        modifiedProperty().set(true);

        if(mFrequencyHoldSelectionListener != null)
        {
            mFrequencyHoldSelectionListener.frequencyHoldSelectionChanged(selected && frequency > 0, frequency);
        }
    }

    /**
     * Updates the preferred tuner's combo box to reflect the current set of tuners.
     */
    private void updatePreferredTuners()
    {
        getPreferredTunerComboBox().getItems().clear();
        getPreferredTunerComboBox().getItems().add(NONE);

        if(mTunerManager != null)
        {
            getPreferredTunerComboBox().getItems().addAll(mTunerManager.getPreferredTunerNames());
        }
    }

    /**
     * Primary frequency editor control
     */
    private FrequencyBox getPrimaryFrequencyBox()
    {
        if(mPrimaryFrequencyBox == null)
        {
            mPrimaryFrequencyBox = new FrequencyBox(mAllowMultipleFrequencies ? Buttons.ADD : Buttons.NONE);
            mFrequencyBoxes.add(mPrimaryFrequencyBox);
        }

        return mPrimaryFrequencyBox;
    }

    /**
     * Preferred tuner combo box control
     */
    private ComboBox<String> getPreferredTunerComboBox()
    {
        if(mPreferredTunerComboBox == null)
        {
            mPreferredTunerComboBox = new ComboBox<>();
            mPreferredTunerComboBox.setDisable(true);
            mPreferredTunerComboBox.getItems().add(NONE);
            mPreferredTunerComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mPreferredTunerComboBox;
    }

    /**
     * Container for all frequency controls.
     */
    private VBox getFrequencyBoxContainer()
    {
        if(mFrequencyBoxContainer == null)
        {
            mFrequencyBoxContainer = new VBox();
            mFrequencyBoxContainer.setSpacing(5);
            mFrequencyBoxContainer.getChildren().addAll(getPrimaryFrequencyBox());
        }

        return mFrequencyBoxContainer;
    }

    /**
     * Updates the control with the list of frequencies
     */
    public void setFrequencies(List<Long> frequencies)
    {
        resetFrequencyBoxes();

        for(int x = 0; x < frequencies.size(); x++)
        {
            if(x == 0)
            {
                getPrimaryFrequencyBox().getFrequencyField().set(frequencies.get(0));
            }
            else
            {
                FrequencyBox frequencyBox = addFrequencyBox();
                frequencyBox.getFrequencyField().set(frequencies.get(x));
            }
        }

        updateFrequencyHoldOptions(null);
    }

    /**
     * Updates the control with the specified single frequency
     */
    public void setFrequency(Long frequency)
    {
        resetFrequencyBoxes();
        getPrimaryFrequencyBox().getFrequencyField().set(frequency);
        updateFrequencyHoldOptions(null);
    }

    /**
     * List of frequencies contained in this control
     */
    public List<Long> getFrequencies()
    {
        List<Long> frequencies = new ArrayList<>();
        for(FrequencyBox frequencyBox: mFrequencyBoxes)
        {
            long frequency = frequencyBox.getFrequencyField().get();

            if(frequency > 0)
            {
                frequencies.add(frequency);
            }
        }

        return frequencies;
    }

    /**
     * Updates the frequency label controls with the list of labels.
     */
    public void setFrequencyLabels(List<String> labels)
    {
        if(labels != null)
        {
            for(int x = 0; x < labels.size() && x < mFrequencyBoxes.size(); x++)
            {
                mFrequencyBoxes.get(x).getFrequencyLabelField().setText(labels.get(x));
            }
        }
    }

    /**
     * List of frequency labels contained in this control.
     */
    public List<String> getFrequencyLabels()
    {
        List<String> labels = new ArrayList<>();

        for(FrequencyBox frequencyBox: mFrequencyBoxes)
        {
            if(frequencyBox.getFrequencyField().get() > 0)
            {
                String label = frequencyBox.getFrequencyLabelField().getText();
                labels.add(label != null ? label.trim() : "");
            }
        }

        return labels;
    }

    /**
     * Resets the frequency boxes to a single frequency box with an empty frequency value.
     */
    private void resetFrequencyBoxes()
    {
        getFrequencyBoxContainer().getChildren().clear();
        mFrequencyBoxes.clear();
        getFrequencyBoxContainer().getChildren().add(getPrimaryFrequencyBox());
        mFrequencyBoxes.add(getPrimaryFrequencyBox());
        getPrimaryFrequencyBox().getFrequencyField().set(0);
        getPrimaryFrequencyBox().getFrequencyLabelField().setText(null);
    }

    /**
     * Adds a frequency box to the editor
     */
    private FrequencyBox addFrequencyBox()
    {
        FrequencyBox frequencyBox = new FrequencyBox(Buttons.REMOVE);
        frequencyBox.disable(false);
        mFrequencyBoxes.add(frequencyBox);
        getFrequencyBoxContainer().getChildren().add(frequencyBox);
        updateFrequencyHoldOptions(null);
        return frequencyBox;
    }

    /**
     * Removes the specified frequency box from this editor
     */
    private void removeFrequencyBox(FrequencyBox toRemove)
    {
        getFrequencyBoxContainer().getChildren().remove(toRemove);
        mFrequencyBoxes.remove(toRemove);
        updateFrequencyHoldOptions(null);
        modifiedProperty().set(true);
    }

    /**
     * Buttons enum used for styling FrequencyBox controls
     */
    public enum Buttons {ADD, REMOVE, NONE }

    /**
     * Listener for immediate frequency hold changes.
     */
    public interface FrequencyHoldSelectionListener
    {
        /**
         * Indicates that frequency hold selection changed.
         * @param hold true to hold, false to resume rotation
         * @param frequency selected frequency
         */
        void frequencyHoldSelectionChanged(boolean hold, long frequency);
    }

    /**
     * Frequency field with optional ADD or REMOVE button
     */
    public class FrequencyBox extends HBox
    {
        private FrequencyField mFrequencyField;
        private TextField mFrequencyLabelField;
        private CheckBox mHoldCheckBox;
        private Button mAddButton;
        private Button mRemoveButton;

        /**
         * Constructs a frequency box with the specified button styling
         */
        public FrequencyBox(Buttons buttons)
        {
            setSpacing(10);
            getChildren().add(getFrequencyField());

            if(mAllowMultipleFrequencies)
            {
                getChildren().add(getFrequencyLabelField());
                getChildren().add(getHoldCheckBox());
            }

            switch(buttons)
            {
                case ADD:
                    getChildren().add(getAddButton());
                    break;
                case REMOVE:
                    getChildren().add(getRemoveButton());
                    break;
            }
        }

        /**
         * Sets the disabled state for this frequency editor
         */
        public void disable(boolean disable)
        {
            getFrequencyField().setDisable(disable);
            getFrequencyLabelField().setDisable(disable);
            getHoldCheckBox().setDisable(disable);
            getAddButton().setDisable(disable);
            getRemoveButton().setDisable(disable);
        }

        /**
         * Frequency field control for this editor
         */
        public FrequencyField getFrequencyField()
        {
            if(mFrequencyField == null)
            {
                mFrequencyField = new FrequencyField();
                mFrequencyField.setDisable(true);
                mFrequencyField.textProperty().addListener(new ChangeListener<String>()
                {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
                    {
                        modifiedProperty().set(true);
                        updateFrequencyHoldOptions(null);
                    }
                });
            }

            return mFrequencyField;
        }

        /**
         * Frequency label control for this editor.
         */
        public TextField getFrequencyLabelField()
        {
            if(mFrequencyLabelField == null)
            {
                mFrequencyLabelField = new TextField();
                mFrequencyLabelField.setPromptText("Label");
                mFrequencyLabelField.setPrefWidth(90);
                mFrequencyLabelField.setDisable(true);
                mFrequencyLabelField.textProperty().addListener((observable, oldValue, newValue) ->
                    modifiedProperty().set(true));
            }

            return mFrequencyLabelField;
        }

        /**
         * Hold checkbox control for this frequency.
         */
        private CheckBox getHoldCheckBox()
        {
            if(mHoldCheckBox == null)
            {
                mHoldCheckBox = new CheckBox("Hold");
                mHoldCheckBox.setDisable(true);
                mHoldCheckBox.setTooltip(new Tooltip("Hold on this frequency instead of rotating"));
                mHoldCheckBox.selectedProperty().addListener((observable, oldValue, selected) -> {
                    if(!mUpdatingHoldSelection && (selected || oldValue))
                    {
                        selectHoldFrequency(FrequencyBox.this, selected);
                    }
                });
            }

            return mHoldCheckBox;
        }

        /**
         * Sets the hold selected state without triggering the selection listener.
         */
        private void setHoldSelected(boolean selected)
        {
            getHoldCheckBox().setSelected(selected);
        }

        /**
         * Add button
         */
        private Button getAddButton()
        {
            if(mAddButton == null)
            {
                mAddButton = new Button("Add");
                mAddButton.setDisable(true);
                mAddButton.setOnAction(event -> addFrequencyBox());
            }

            return mAddButton;
        }

        /**
         * Remove button
         */
        private Button getRemoveButton()
        {
            if(mRemoveButton == null)
            {
                mRemoveButton = new Button("Remove");
                mRemoveButton.setDisable(true);
                mRemoveButton.setOnAction(event -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Do you want to remove this frequency?", ButtonType.YES, ButtonType.NO);
                    confirm.setTitle("Remove Frequency");
                    confirm.setHeaderText("Remove frequency?");
                    confirm.initOwner(((Node)event.getTarget()).getScene().getWindow());
                    confirm.showAndWait().ifPresent(buttonType -> {
                        if(buttonType == ButtonType.YES)
                        {
                            removeFrequencyBox(FrequencyBox.this);
                        }
                    });
                });
            }

            return mRemoveButton;
        }
    }
}
