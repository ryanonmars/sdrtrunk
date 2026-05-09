/*
 * *****************************************************************************
 * Copyright (C) 2026 Dennis Sheirer
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

package io.github.dsheirer.playlist.csv;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.talkgroup.P25FullyQualifiedTalkgroup;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

/**
 * Imports and exports a practical, Excel-friendly CSV representation of playlist channels and aliases.
 */
public class PlaylistCsvService
{
    public static final String RECORD_TYPE = "record_type";
    public static final String ALIAS_LIST = "alias_list";
    public static final String ALIAS_NAME = "alias_name";
    public static final String ALIAS_GROUP = "alias_group";
    public static final String ALIAS_COLOR = "alias_color";
    public static final String ICON_NAME = "icon_name";
    public static final String CALL_PRIORITY = "call_priority";
    public static final String RECORDABLE = "recordable";
    public static final String IDENTIFIER_TYPE = "identifier_type";
    public static final String PROTOCOL = "protocol";
    public static final String TALKGROUP = "talkgroup";
    public static final String TALKGROUP_MIN = "talkgroup_min";
    public static final String TALKGROUP_MAX = "talkgroup_max";
    public static final String RADIO_ID = "radio_id";
    public static final String RADIO_ID_MIN = "radio_id_min";
    public static final String RADIO_ID_MAX = "radio_id_max";
    public static final String WACN = "wacn";
    public static final String SYSTEM_ID = "system_id";
    public static final String CHANNEL_SYSTEM = "channel_system";
    public static final String CHANNEL_SITE = "channel_site";
    public static final String CHANNEL_NAME = "channel_name";
    public static final String DECODER_TYPE = "decoder_type";
    public static final String FREQUENCY_MHZ = "frequency_mhz";
    public static final String FREQUENCY_LABEL = "frequency_label";
    public static final String PREFERRED_TUNER = "preferred_tuner";
    public static final String AUTO_START = "auto_start";
    public static final String AUTO_START_ORDER = "auto_start_order";
    public static final String FREQUENCY_ROTATION_DELAY_MS = "frequency_rotation_delay_ms";

    public static final String RECORD_TYPE_CHANNEL = "CHANNEL";
    public static final String RECORD_TYPE_ALIAS_ID = "ALIAS_ID";

    public static final String[] HEADERS = {RECORD_TYPE, ALIAS_LIST, ALIAS_NAME, ALIAS_GROUP, ALIAS_COLOR, ICON_NAME,
        CALL_PRIORITY, RECORDABLE, IDENTIFIER_TYPE, PROTOCOL, TALKGROUP, TALKGROUP_MIN, TALKGROUP_MAX, RADIO_ID,
        RADIO_ID_MIN, RADIO_ID_MAX, WACN, SYSTEM_ID, CHANNEL_SYSTEM, CHANNEL_SITE, CHANNEL_NAME, DECODER_TYPE,
        FREQUENCY_MHZ, FREQUENCY_LABEL, PREFERRED_TUNER, AUTO_START, AUTO_START_ORDER,
        FREQUENCY_ROTATION_DELAY_MS};

    private static final EnumSet<AliasIDType> SUPPORTED_ALIAS_ID_TYPES = EnumSet.of(AliasIDType.TALKGROUP,
        AliasIDType.TALKGROUP_RANGE, AliasIDType.RADIO_ID, AliasIDType.RADIO_ID_RANGE,
        AliasIDType.P25_FULLY_QUALIFIED_TALKGROUP, AliasIDType.P25_FULLY_QUALIFIED_RADIO_ID);

    private static final CSVFormat CSV_EXPORT_FORMAT = CSVFormat.Builder.create(CSVFormat.DEFAULT)
        .setHeader(HEADERS)
        .setQuoteMode(QuoteMode.MINIMAL)
        .build();
    private static final CSVFormat CSV_IMPORT_FORMAT = CSVFormat.Builder.create(CSVFormat.DEFAULT)
        .setHeader()
        .setSkipHeaderRecord(true)
        .setIgnoreSurroundingSpaces(true)
        .setTrim(true)
        .build();
    private static final DecimalFormat FREQUENCY_FORMAT =
        new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(Locale.US));

    /**
     * Exports the channels and supported alias identifiers to CSV.
     */
    public PlaylistCsvResult export(Writer writer, ChannelModel channelModel, AliasModel aliasModel) throws IOException
    {
        PlaylistCsvResult result = new PlaylistCsvResult();

        try(CSVPrinter printer = new CSVPrinter(writer, CSV_EXPORT_FORMAT))
        {
            for(Channel channel: channelModel.getChannels())
            {
                for(List<String> record: channelRecords(channel))
                {
                    printer.printRecord(record);
                    result.incrementChannelRows();
                }
            }

            for(Alias alias: aliasModel.getAliases())
            {
                for(AliasID aliasID: alias.getAliasIdentifiers())
                {
                    if(SUPPORTED_ALIAS_ID_TYPES.contains(aliasID.getType()))
                    {
                        printer.printRecord(aliasRecord(alias, aliasID));
                        result.incrementAliasIdentifierRows();
                    }
                }
            }
        }

        return result;
    }

    /**
     * Imports CSV rows into the channel and alias models.
     */
    public PlaylistCsvResult importCsv(Reader reader, ChannelModel channelModel, AliasModel aliasModel) throws IOException
    {
        PlaylistCsvResult result = new PlaylistCsvResult();
        Map<ChannelKey,ChannelImportAccumulator> channelRows = new LinkedHashMap<>();

        try(CSVParser parser = CSVParser.parse(reader, CSV_IMPORT_FORMAT))
        {
            validateHeaders(parser);

            for(CSVRecord record: parser)
            {
                String recordType = get(record, RECORD_TYPE);

                try
                {
                    if(RECORD_TYPE_CHANNEL.equalsIgnoreCase(recordType))
                    {
                        ChannelImportRow row = parseChannel(record);
                        ChannelKey key = new ChannelKey(row.system(), row.site(), row.name());
                        channelRows.computeIfAbsent(key, channelKey -> new ChannelImportAccumulator(row)).add(row);
                        result.incrementChannelRows();
                    }
                    else if(RECORD_TYPE_ALIAS_ID.equalsIgnoreCase(recordType))
                    {
                        AliasImportRow row = parseAlias(record);
                        applyAlias(row, aliasModel, result);
                        result.incrementAliasIdentifierRows();
                    }
                    else
                    {
                        throw new IllegalArgumentException("Unsupported record_type [" + recordType + "]");
                    }
                }
                catch(IllegalArgumentException e)
                {
                    result.addError(record.getRecordNumber() + 1, e.getMessage());
                }
            }
        }

        for(ChannelImportAccumulator accumulator: channelRows.values())
        {
            applyChannel(accumulator, channelModel, result);
        }

        return result;
    }

    private void validateHeaders(CSVParser parser)
    {
        Map<String,Integer> headerMap = parser.getHeaderMap();

        for(String header: HEADERS)
        {
            if(!headerMap.containsKey(header))
            {
                throw new IllegalArgumentException("Missing CSV header [" + header + "]");
            }
        }
    }

    private List<List<String>> channelRecords(Channel channel)
    {
        List<List<String>> records = new ArrayList<>();
        SourceConfiguration sourceConfiguration = channel.getSourceConfiguration();
        String preferredTuner = "";
        String rotationDelay = "";

        if(sourceConfiguration instanceof SourceConfigTuner sourceConfigTuner)
        {
            preferredTuner = value(sourceConfigTuner.getPreferredTuner());
            records.add(channelRecord(channel, formatFrequency(sourceConfigTuner.getFrequency()), "", preferredTuner, ""));
        }
        else if(sourceConfiguration instanceof SourceConfigTunerMultipleFrequency sourceConfigMulti)
        {
            preferredTuner = value(sourceConfigMulti.getPreferredTuner());
            rotationDelay = String.valueOf(sourceConfigMulti.getFrequencyRotationDelay());

            for(int x = 0; x < sourceConfigMulti.getFrequencies().size(); x++)
            {
                String label = x < sourceConfigMulti.getFrequencyLabels().size() ?
                    sourceConfigMulti.getFrequencyLabels().get(x) : "";
                records.add(channelRecord(channel, formatFrequency(sourceConfigMulti.getFrequencies().get(x)), label,
                    preferredTuner, rotationDelay));
            }
        }

        if(records.isEmpty())
        {
            records.add(channelRecord(channel, "", "", "", ""));
        }

        return records;
    }

    private List<String> channelRecord(Channel channel, String frequency, String frequencyLabel, String preferredTuner,
                                      String rotationDelay)
    {
        String decoderType = channel.getDecodeConfiguration() != null &&
            channel.getDecodeConfiguration().getDecoderType() != null ?
            channel.getDecodeConfiguration().getDecoderType().name() : "";

        return values(RECORD_TYPE_CHANNEL, channel.getAliasListName(), "", "", "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", channel.getSystem(), channel.getSite(), channel.getName(), decoderType, frequency, frequencyLabel,
            preferredTuner, String.valueOf(channel.isAutoStart()),
            channel.hasAutoStartOrder() ? String.valueOf(channel.getAutoStartOrder()) : "", rotationDelay);
    }

    private List<String> aliasRecord(Alias alias, AliasID aliasID)
    {
        Map<String,String> idValues = exportAliasID(aliasID);

        return values(RECORD_TYPE_ALIAS_ID, alias.getAliasListName(), alias.getName(), alias.getGroup(),
            alias.getColorHex(), alias.getIconName(), alias.hasCallPriority() ? String.valueOf(alias.getPlaybackPriority()) : "",
            String.valueOf(alias.isRecordable()), idValues.get(IDENTIFIER_TYPE), idValues.get(PROTOCOL),
            idValues.get(TALKGROUP), idValues.get(TALKGROUP_MIN), idValues.get(TALKGROUP_MAX),
            idValues.get(RADIO_ID), idValues.get(RADIO_ID_MIN), idValues.get(RADIO_ID_MAX), idValues.get(WACN),
            idValues.get(SYSTEM_ID), "", "", "", "", "", "", "", "", "", "");
    }

    private Map<String,String> exportAliasID(AliasID aliasID)
    {
        Map<String,String> values = new HashMap<>();
        values.put(IDENTIFIER_TYPE, aliasID.getType().name());
        values.put(PROTOCOL, "");
        values.put(TALKGROUP, "");
        values.put(TALKGROUP_MIN, "");
        values.put(TALKGROUP_MAX, "");
        values.put(RADIO_ID, "");
        values.put(RADIO_ID_MIN, "");
        values.put(RADIO_ID_MAX, "");
        values.put(WACN, "");
        values.put(SYSTEM_ID, "");

        switch(aliasID.getType())
        {
            case TALKGROUP:
                Talkgroup talkgroup = (Talkgroup)aliasID;
                values.put(PROTOCOL, talkgroup.getProtocol().name());
                values.put(TALKGROUP, String.valueOf(talkgroup.getValue()));
                break;
            case TALKGROUP_RANGE:
                TalkgroupRange talkgroupRange = (TalkgroupRange)aliasID;
                values.put(PROTOCOL, talkgroupRange.getProtocol().name());
                values.put(TALKGROUP_MIN, String.valueOf(talkgroupRange.getMinTalkgroup()));
                values.put(TALKGROUP_MAX, String.valueOf(talkgroupRange.getMaxTalkgroup()));
                break;
            case RADIO_ID:
                Radio radio = (Radio)aliasID;
                values.put(PROTOCOL, radio.getProtocol().name());
                values.put(RADIO_ID, String.valueOf(radio.getValue()));
                break;
            case RADIO_ID_RANGE:
                RadioRange radioRange = (RadioRange)aliasID;
                values.put(PROTOCOL, radioRange.getProtocol().name());
                values.put(RADIO_ID_MIN, String.valueOf(radioRange.getMinRadio()));
                values.put(RADIO_ID_MAX, String.valueOf(radioRange.getMaxRadio()));
                break;
            case P25_FULLY_QUALIFIED_TALKGROUP:
                P25FullyQualifiedTalkgroup p25Talkgroup = (P25FullyQualifiedTalkgroup)aliasID;
                values.put(PROTOCOL, p25Talkgroup.getProtocol().name());
                values.put(TALKGROUP, String.valueOf(p25Talkgroup.getValue()));
                values.put(WACN, String.valueOf(p25Talkgroup.getWacn()));
                values.put(SYSTEM_ID, String.valueOf(p25Talkgroup.getSystem()));
                break;
            case P25_FULLY_QUALIFIED_RADIO_ID:
                P25FullyQualifiedRadio p25Radio = (P25FullyQualifiedRadio)aliasID;
                values.put(PROTOCOL, p25Radio.getProtocol().name());
                values.put(RADIO_ID, String.valueOf(p25Radio.getValue()));
                values.put(WACN, String.valueOf(p25Radio.getWacn()));
                values.put(SYSTEM_ID, String.valueOf(p25Radio.getSystem()));
                break;
            default:
                break;
        }

        return values;
    }

    private ChannelImportRow parseChannel(CSVRecord record)
    {
        String name = require(record, CHANNEL_NAME);
        DecoderType decoderType = parseDecoderType(require(record, DECODER_TYPE));
        long frequency = parseFrequency(require(record, FREQUENCY_MHZ));
        Integer rotationDelay = parseOptionalInteger(record, FREQUENCY_ROTATION_DELAY_MS);

        return new ChannelImportRow(get(record, ALIAS_LIST), get(record, CHANNEL_SYSTEM), get(record, CHANNEL_SITE),
            name, decoderType, frequency, get(record, FREQUENCY_LABEL), get(record, PREFERRED_TUNER),
            parseOptionalBoolean(record, AUTO_START), parseOptionalInteger(record, AUTO_START_ORDER), rotationDelay);
    }

    private AliasImportRow parseAlias(CSVRecord record)
    {
        String aliasName = require(record, ALIAS_NAME);
        AliasIDType identifierType = parseAliasIDType(require(record, IDENTIFIER_TYPE));

        if(!SUPPORTED_ALIAS_ID_TYPES.contains(identifierType))
        {
            throw new IllegalArgumentException("Unsupported identifier_type [" + identifierType.name() + "]");
        }

        AliasID aliasID = parseAliasID(record, identifierType);

        if(!aliasID.isValid())
        {
            throw new IllegalArgumentException("Invalid alias identifier");
        }

        String aliasColorValue = get(record, ALIAS_COLOR);

        return new AliasImportRow(get(record, ALIAS_LIST), aliasName, get(record, ALIAS_GROUP),
            aliasColorValue.isBlank() ? null : parseColor(aliasColorValue),
            get(record, ICON_NAME), parseOptionalInteger(record, CALL_PRIORITY), parseOptionalBoolean(record, RECORDABLE),
            aliasID);
    }

    private AliasID parseAliasID(CSVRecord record, AliasIDType identifierType)
    {
        return switch(identifierType)
        {
            case TALKGROUP -> new Talkgroup(parseProtocol(require(record, PROTOCOL)),
                parseInteger(require(record, TALKGROUP), TALKGROUP));
            case TALKGROUP_RANGE -> new TalkgroupRange(parseProtocol(require(record, PROTOCOL)),
                parseInteger(require(record, TALKGROUP_MIN), TALKGROUP_MIN),
                parseInteger(require(record, TALKGROUP_MAX), TALKGROUP_MAX));
            case RADIO_ID -> new Radio(parseProtocol(require(record, PROTOCOL)),
                parseInteger(require(record, RADIO_ID), RADIO_ID));
            case RADIO_ID_RANGE -> new RadioRange(parseProtocol(require(record, PROTOCOL)),
                parseInteger(require(record, RADIO_ID_MIN), RADIO_ID_MIN),
                parseInteger(require(record, RADIO_ID_MAX), RADIO_ID_MAX));
            case P25_FULLY_QUALIFIED_TALKGROUP -> new P25FullyQualifiedTalkgroup(parseInteger(require(record, WACN), WACN),
                parseInteger(require(record, SYSTEM_ID), SYSTEM_ID), parseInteger(require(record, TALKGROUP), TALKGROUP));
            case P25_FULLY_QUALIFIED_RADIO_ID -> new P25FullyQualifiedRadio(parseInteger(require(record, WACN), WACN),
                parseInteger(require(record, SYSTEM_ID), SYSTEM_ID), parseInteger(require(record, RADIO_ID), RADIO_ID));
            default -> throw new IllegalArgumentException("Unsupported identifier_type [" + identifierType.name() + "]");
        };
    }

    private void applyChannel(ChannelImportAccumulator row, ChannelModel channelModel, PlaylistCsvResult result)
    {
        Channel channel = findChannel(channelModel, row.system(), row.site(), row.name());
        boolean created = false;

        if(channel == null)
        {
            channel = new Channel();
            channelModel.addChannel(channel);
            created = true;
        }

        channel.setSystem(emptyToNull(row.system()));
        channel.setSite(emptyToNull(row.site()));
        channel.setName(row.name());
        channel.setAliasListName(emptyToNull(row.aliasList()));
        channel.setDecodeConfiguration(DecoderFactory.getDecodeConfiguration(row.decoderType()));
        channel.setSourceConfiguration(createSourceConfiguration(row));

        if(row.autoStart() != null)
        {
            channel.setAutoStart(row.autoStart());
        }

        if(row.autoStartOrder() != null)
        {
            channel.setAutoStartOrder(row.autoStartOrder());
        }

        if(created)
        {
            result.incrementChannelsCreated();
        }
        else
        {
            result.incrementChannelsUpdated();
        }
    }

    private SourceConfiguration createSourceConfiguration(ChannelImportAccumulator row)
    {
        if(row.frequencies().size() == 1)
        {
            SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
            sourceConfigTuner.setFrequency(row.frequencies().getFirst());
            sourceConfigTuner.setPreferredTuner(emptyToNull(row.preferredTuner()));
            return sourceConfigTuner;
        }

        SourceConfigTunerMultipleFrequency sourceConfigMulti = new SourceConfigTunerMultipleFrequency();
        sourceConfigMulti.setFrequencies(new ArrayList<>(row.frequencies()));
        sourceConfigMulti.setFrequencyLabels(new ArrayList<>(row.frequencyLabels()));
        sourceConfigMulti.setPreferredTuner(emptyToNull(row.preferredTuner()));

        if(row.frequencyRotationDelay() != null)
        {
            sourceConfigMulti.setFrequencyRotationDelay(row.frequencyRotationDelay());
        }

        return sourceConfigMulti;
    }

    private void applyAlias(AliasImportRow row, AliasModel aliasModel, PlaylistCsvResult result)
    {
        Alias alias = findAlias(aliasModel, row.aliasList(), row.aliasName());
        boolean created = false;

        if(alias == null)
        {
            alias = new Alias();
            alias.setAliasListName(emptyToNull(row.aliasList()));
            alias.setName(row.aliasName());
            aliasModel.addAlias(alias);
            created = true;
        }

        alias.setAliasListName(emptyToNull(row.aliasList()));
        alias.setName(row.aliasName());
        alias.setGroup(emptyToNull(row.aliasGroup()));
        alias.setIconName(emptyToNull(row.iconName()));

        if(row.aliasColor() != null)
        {
            alias.setColor(row.aliasColor());
        }

        if(hasAliasIdentifier(alias, row.aliasID()))
        {
            result.incrementAliasIdentifiersMatched();
        }
        else
        {
            alias.addAliasID(row.aliasID());
            result.incrementAliasIdentifiersAdded();
        }

        if(row.callPriority() != null)
        {
            alias.setCallPriority(row.callPriority());
        }

        if(row.recordable() != null)
        {
            alias.setRecordable(row.recordable());
        }

        if(created)
        {
            result.incrementAliasesCreated();
        }
        else
        {
            result.incrementAliasesUpdated();
        }
    }

    private Channel findChannel(ChannelModel channelModel, String system, String site, String name)
    {
        for(Channel channel: channelModel.getChannels())
        {
            if(equalsNullable(channel.getSystem(), system) && equalsNullable(channel.getSite(), site) &&
                equalsNullable(channel.getName(), name))
            {
                return channel;
            }
        }

        return null;
    }

    private Alias findAlias(AliasModel aliasModel, String aliasList, String aliasName)
    {
        for(Alias alias: aliasModel.getAliases())
        {
            if(equalsNullable(alias.getAliasListName(), aliasList) && equalsNullable(alias.getName(), aliasName))
            {
                return alias;
            }
        }

        return null;
    }

    private boolean hasAliasIdentifier(Alias alias, AliasID aliasID)
    {
        for(AliasID existing: alias.getAliasIdentifiers())
        {
            if(aliasIdentifiersMatch(existing, aliasID))
            {
                return true;
            }
        }

        return false;
    }

    private boolean aliasIdentifiersMatch(AliasID existing, AliasID imported)
    {
        if(existing.getType() != imported.getType())
        {
            return false;
        }

        return switch(existing.getType())
        {
            case P25_FULLY_QUALIFIED_RADIO_ID -> {
                P25FullyQualifiedRadio existingRadio = (P25FullyQualifiedRadio)existing;
                P25FullyQualifiedRadio importedRadio = (P25FullyQualifiedRadio)imported;
                yield existingRadio.getProtocol() == importedRadio.getProtocol() &&
                    existingRadio.getValue() == importedRadio.getValue() &&
                    existingRadio.getWacn() == importedRadio.getWacn() &&
                    existingRadio.getSystem() == importedRadio.getSystem();
            }
            case P25_FULLY_QUALIFIED_TALKGROUP -> {
                P25FullyQualifiedTalkgroup existingTalkgroup = (P25FullyQualifiedTalkgroup)existing;
                P25FullyQualifiedTalkgroup importedTalkgroup = (P25FullyQualifiedTalkgroup)imported;
                yield existingTalkgroup.getProtocol() == importedTalkgroup.getProtocol() &&
                    existingTalkgroup.getValue() == importedTalkgroup.getValue() &&
                    existingTalkgroup.getWacn() == importedTalkgroup.getWacn() &&
                    existingTalkgroup.getSystem() == importedTalkgroup.getSystem();
            }
            default -> existing.matches(imported);
        };
    }

    private String require(CSVRecord record, String header)
    {
        String value = get(record, header);

        if(value.isBlank())
        {
            throw new IllegalArgumentException("Missing required value [" + header + "]");
        }

        return value;
    }

    private String get(CSVRecord record, String header)
    {
        String value = record.get(header);
        return value != null ? value.trim() : "";
    }

    private DecoderType parseDecoderType(String value)
    {
        for(DecoderType decoderType: DecoderType.PRIMARY_DECODERS)
        {
            if(decoderType.name().equalsIgnoreCase(value) || decoderType.toString().equalsIgnoreCase(value) ||
                decoderType.getShortDisplayString().equalsIgnoreCase(value))
            {
                return decoderType;
            }
        }

        throw new IllegalArgumentException("Invalid decoder_type [" + value + "]");
    }

    private AliasIDType parseAliasIDType(String value)
    {
        try
        {
            return AliasIDType.valueOf(value.trim().toUpperCase(Locale.US));
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("Invalid identifier_type [" + value + "]");
        }
    }

    private Protocol parseProtocol(String value)
    {
        for(Protocol protocol: Protocol.values())
        {
            if(protocol.name().equalsIgnoreCase(value) || protocol.toString().equalsIgnoreCase(value) ||
                protocol.getFileNameLabel().equalsIgnoreCase(value))
            {
                return protocol;
            }
        }

        throw new IllegalArgumentException("Invalid protocol [" + value + "]");
    }

    private Integer parseOptionalInteger(CSVRecord record, String header)
    {
        String value = get(record, header);
        return value.isBlank() ? null : parseInteger(value, header);
    }

    private int parseInteger(String value, String header)
    {
        try
        {
            return Integer.parseInt(value.trim());
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid integer [" + header + "=" + value + "]");
        }
    }

    private Boolean parseOptionalBoolean(CSVRecord record, String header)
    {
        String value = get(record, header);

        if(value.isBlank())
        {
            return null;
        }

        if("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value))
        {
            return true;
        }
        else if("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "0".equals(value))
        {
            return false;
        }

        throw new IllegalArgumentException("Invalid boolean [" + header + "=" + value + "]");
    }

    private long parseFrequency(String value)
    {
        try
        {
            long frequency = new BigDecimal(value).multiply(BigDecimal.valueOf(1_000_000L))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();

            if(frequency <= 0)
            {
                throw new IllegalArgumentException("Frequency must be greater than zero [" + value + "]");
            }

            return frequency;
        }
        catch(NumberFormatException | ArithmeticException e)
        {
            throw new IllegalArgumentException("Invalid frequency_mhz [" + value + "]");
        }
    }

    private int parseColor(String value)
    {
        try
        {
            if(value.startsWith("#"))
            {
                return Integer.parseUnsignedInt(value.substring(1), 16);
            }

            return Integer.parseInt(value);
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid alias_color [" + value + "]");
        }
    }

    private String formatFrequency(long frequency)
    {
        return FREQUENCY_FORMAT.format(BigDecimal.valueOf(frequency).divide(BigDecimal.valueOf(1_000_000L)));
    }

    private List<String> values(String ... values)
    {
        List<String> cleaned = new ArrayList<>();

        for(String value: values)
        {
            cleaned.add(value(value));
        }

        return cleaned;
    }

    private String value(String value)
    {
        return value != null ? value : "";
    }

    private String emptyToNull(String value)
    {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean equalsNullable(String first, String second)
    {
        return value(first).equals(value(second));
    }

    private record ChannelImportRow(String aliasList, String system, String site, String name, DecoderType decoderType,
                                    long frequency, String frequencyLabel, String preferredTuner,
                                    Boolean autoStart, Integer autoStartOrder, Integer frequencyRotationDelay)
    {
    }

    private record ChannelKey(String system, String site, String name)
    {
    }

    private static class ChannelImportAccumulator
    {
        private ChannelImportRow mFirstRow;
        private List<Long> mFrequencies = new ArrayList<>();
        private List<String> mFrequencyLabels = new ArrayList<>();

        public ChannelImportAccumulator(ChannelImportRow firstRow)
        {
            mFirstRow = firstRow;
        }

        public void add(ChannelImportRow row)
        {
            mFrequencies.add(row.frequency());
            mFrequencyLabels.add(row.frequencyLabel());
        }

        public String aliasList()
        {
            return mFirstRow.aliasList();
        }

        public String system()
        {
            return mFirstRow.system();
        }

        public String site()
        {
            return mFirstRow.site();
        }

        public String name()
        {
            return mFirstRow.name();
        }

        public DecoderType decoderType()
        {
            return mFirstRow.decoderType();
        }

        public List<Long> frequencies()
        {
            return mFrequencies;
        }

        public List<String> frequencyLabels()
        {
            return mFrequencyLabels;
        }

        public String preferredTuner()
        {
            return mFirstRow.preferredTuner();
        }

        public Boolean autoStart()
        {
            return mFirstRow.autoStart();
        }

        public Integer autoStartOrder()
        {
            return mFirstRow.autoStartOrder();
        }

        public Integer frequencyRotationDelay()
        {
            return mFirstRow.frequencyRotationDelay();
        }
    }

    private record AliasImportRow(String aliasList, String aliasName, String aliasGroup, Integer aliasColor,
                                  String iconName, Integer callPriority, Boolean recordable, AliasID aliasID)
    {
    }
}
