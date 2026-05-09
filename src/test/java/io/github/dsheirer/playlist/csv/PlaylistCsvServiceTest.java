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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PlaylistCsvServiceTest
{
    private final PlaylistCsvService mService = new PlaylistCsvService();

    @Test
    void exportEmptyPlaylistTemplateHeaders() throws Exception
    {
        StringWriter writer = new StringWriter();
        PlaylistCsvResult result = mService.export(writer, new ChannelModel(new AliasModel()), new AliasModel());

        assertEquals(0, result.getChannelRows());
        assertEquals(0, result.getAliasIdentifierRows());
        assertEquals(String.join(",", PlaylistCsvService.HEADERS) + "\r\n", writer.toString());
    }

    @Test
    void exportWritesOneChannelRowPerFrequency() throws Exception
    {
        AliasModel aliasModel = new AliasModel();
        ChannelModel channelModel = new ChannelModel(aliasModel);
        Channel channel = new Channel();
        channel.setSystem("Metro");
        channel.setSite("North");
        channel.setName("Control");
        channel.setAliasListName("Metro");
        SourceConfigTunerMultipleFrequency source = new SourceConfigTunerMultipleFrequency();
        source.setFrequencies(List.of(851012500L, 852025000L));
        source.setFrequencyLabels(List.of("Primary", "Alternate"));
        channel.setSourceConfiguration(source);
        channelModel.addChannel(channel);

        StringWriter writer = new StringWriter();
        PlaylistCsvResult result = mService.export(writer, channelModel, aliasModel);

        assertEquals(2, result.getChannelRows());
        assertTrue(writer.toString().contains("frequency_mhz,frequency_label"));
        assertTrue(writer.toString().contains("851.0125,Primary"));
        assertTrue(writer.toString().contains("852.025,Alternate"));
    }

    @Test
    void importCreatesSingleAndMultipleFrequencyChannels() throws Exception
    {
        AliasModel aliasModel = new AliasModel();
        ChannelModel channelModel = new ChannelModel(aliasModel);

        PlaylistCsvResult result = mService.importCsv(new StringReader(csv(
            row(PlaylistCsvService.RECORD_TYPE_CHANNEL, PlaylistCsvService.ALIAS_LIST, "Metro",
                PlaylistCsvService.CHANNEL_SYSTEM, "Metro",
                PlaylistCsvService.CHANNEL_SITE, "North", PlaylistCsvService.CHANNEL_NAME, "Control",
                PlaylistCsvService.DECODER_TYPE, "P25_PHASE1", PlaylistCsvService.FREQUENCY_MHZ,
                "851.0125", PlaylistCsvService.FREQUENCY_LABEL, "Primary",
                PlaylistCsvService.AUTO_START, "true", PlaylistCsvService.AUTO_START_ORDER, "1",
                PlaylistCsvService.FREQUENCY_ROTATION_DELAY_MS, "1500"),
            row(PlaylistCsvService.RECORD_TYPE_CHANNEL, PlaylistCsvService.ALIAS_LIST, "Metro",
                PlaylistCsvService.CHANNEL_SYSTEM, "Metro",
                PlaylistCsvService.CHANNEL_SITE, "North", PlaylistCsvService.CHANNEL_NAME, "Control",
                PlaylistCsvService.DECODER_TYPE, "P25_PHASE1", PlaylistCsvService.FREQUENCY_MHZ,
                "852.025", PlaylistCsvService.FREQUENCY_LABEL, "Alternate",
                PlaylistCsvService.AUTO_START, "true", PlaylistCsvService.AUTO_START_ORDER, "1",
                PlaylistCsvService.FREQUENCY_ROTATION_DELAY_MS, "1500"),
            row(PlaylistCsvService.RECORD_TYPE_CHANNEL, PlaylistCsvService.CHANNEL_SYSTEM, "Metro",
                PlaylistCsvService.CHANNEL_SITE, "South", PlaylistCsvService.CHANNEL_NAME, "Dispatch",
                PlaylistCsvService.DECODER_TYPE, "DMR", PlaylistCsvService.FREQUENCY_MHZ, "453.125",
                PlaylistCsvService.AUTO_START, "false")
        )), channelModel, aliasModel);

        assertEquals(2, result.getChannelsCreated());
        assertEquals(2, channelModel.getChannels().size());

        Channel control = channelModel.getChannels().getFirst();
        assertEquals("Control", control.getName());
        assertEquals("Metro", control.getAliasListName());
        assertEquals(DecoderType.P25_PHASE1, control.getDecodeConfiguration().getDecoderType());
        SourceConfigTunerMultipleFrequency multi =
            assertInstanceOf(SourceConfigTunerMultipleFrequency.class, control.getSourceConfiguration());
        assertEquals(2, multi.getFrequencies().size());
        assertEquals(851012500L, multi.getFrequencies().getFirst());
        assertEquals("Primary", multi.getFrequencyLabels().getFirst());
        assertEquals(1500, multi.getFrequencyRotationDelay());
        assertTrue(control.isAutoStart());

        Channel dispatch = channelModel.getChannels().get(1);
        SourceConfigTuner single = assertInstanceOf(SourceConfigTuner.class, dispatch.getSourceConfiguration());
        assertEquals(453125000L, single.getFrequency());
    }

    @Test
    void importCreatesAliasesWithTalkgroupAndRadioIdentifiers() throws Exception
    {
        AliasModel aliasModel = new AliasModel();
        ChannelModel channelModel = new ChannelModel(aliasModel);

        PlaylistCsvResult result = mService.importCsv(new StringReader(csv(
            row(PlaylistCsvService.RECORD_TYPE_ALIAS_ID, PlaylistCsvService.ALIAS_LIST, "Metro",
                PlaylistCsvService.ALIAS_NAME, "Police Dispatch", PlaylistCsvService.ALIAS_GROUP, "Law",
                PlaylistCsvService.ALIAS_COLOR, "#00FF00", PlaylistCsvService.CALL_PRIORITY, "5",
                PlaylistCsvService.RECORDABLE, "true", PlaylistCsvService.IDENTIFIER_TYPE, "TALKGROUP",
                PlaylistCsvService.PROTOCOL, "APCO25", PlaylistCsvService.TALKGROUP, "101"),
            row(PlaylistCsvService.RECORD_TYPE_ALIAS_ID, PlaylistCsvService.ALIAS_LIST, "Metro",
                PlaylistCsvService.ALIAS_NAME, "Unit 123", PlaylistCsvService.ALIAS_GROUP, "Units",
                PlaylistCsvService.RECORDABLE, "false", PlaylistCsvService.IDENTIFIER_TYPE, "RADIO_ID",
                PlaylistCsvService.PROTOCOL, "APCO25", PlaylistCsvService.RADIO_ID, "123"),
            row(PlaylistCsvService.RECORD_TYPE_ALIAS_ID, PlaylistCsvService.ALIAS_LIST, "Metro",
                PlaylistCsvService.ALIAS_NAME, "Unit 456", PlaylistCsvService.ALIAS_GROUP, "Units",
                PlaylistCsvService.RECORDABLE, "false", PlaylistCsvService.IDENTIFIER_TYPE,
                "P25_FULLY_QUALIFIED_RADIO_ID", PlaylistCsvService.PROTOCOL, "APCO25",
                PlaylistCsvService.RADIO_ID, "456", PlaylistCsvService.WACN, "924", PlaylistCsvService.SYSTEM_ID, "45")
        )), channelModel, aliasModel);

        assertEquals(3, result.getAliasesCreated());
        assertEquals(3, aliasModel.getAliases().size());

        Alias talkgroupAlias = aliasModel.getAliases().getFirst();
        assertEquals("Police Dispatch", talkgroupAlias.getName());
        assertEquals("Law", talkgroupAlias.getGroup());
        assertEquals(5, talkgroupAlias.getPlaybackPriority());
        assertTrue(talkgroupAlias.isRecordable());
        assertEquals(AliasIDType.TALKGROUP, talkgroupAlias.getAliasIdentifiers().getFirst().getType());
        Talkgroup talkgroup = assertInstanceOf(Talkgroup.class, talkgroupAlias.getAliasIdentifiers().getFirst());
        assertEquals(Protocol.APCO25, talkgroup.getProtocol());
        assertEquals(101, talkgroup.getValue());

        Alias radioAlias = aliasModel.getAliases().get(1);
        Radio radio = assertInstanceOf(Radio.class, radioAlias.getAliasIdentifiers().getFirst());
        assertEquals(123, radio.getValue());

        Alias fullyQualifiedRadioAlias = aliasModel.getAliases().get(2);
        P25FullyQualifiedRadio p25Radio =
            assertInstanceOf(P25FullyQualifiedRadio.class, fullyQualifiedRadioAlias.getAliasIdentifiers().getFirst());
        assertEquals(924, p25Radio.getWacn());
        assertEquals(45, p25Radio.getSystem());
        assertEquals(456, p25Radio.getValue());
    }

    @Test
    void importUpdatesExistingChannelAndAliasWithoutDuplicating() throws Exception
    {
        AliasModel aliasModel = new AliasModel();
        ChannelModel channelModel = new ChannelModel(aliasModel);

        mService.importCsv(new StringReader(csv(
            row(PlaylistCsvService.RECORD_TYPE_CHANNEL, PlaylistCsvService.CHANNEL_SYSTEM, "Metro",
                PlaylistCsvService.CHANNEL_SITE, "North", PlaylistCsvService.CHANNEL_NAME, "Control",
                PlaylistCsvService.DECODER_TYPE, "P25_PHASE1", PlaylistCsvService.FREQUENCY_MHZ, "851.0125",
                PlaylistCsvService.AUTO_START, "false"),
            row(PlaylistCsvService.RECORD_TYPE_ALIAS_ID, PlaylistCsvService.ALIAS_LIST, "Metro",
                PlaylistCsvService.ALIAS_NAME, "Police Dispatch", PlaylistCsvService.RECORDABLE, "false",
                PlaylistCsvService.IDENTIFIER_TYPE, "TALKGROUP", PlaylistCsvService.PROTOCOL, "APCO25",
                PlaylistCsvService.TALKGROUP, "101")
        )), channelModel, aliasModel);

        PlaylistCsvResult update = mService.importCsv(new StringReader(csv(
            row(PlaylistCsvService.RECORD_TYPE_CHANNEL, PlaylistCsvService.CHANNEL_SYSTEM, "Metro",
                PlaylistCsvService.CHANNEL_SITE, "North", PlaylistCsvService.CHANNEL_NAME, "Control",
                PlaylistCsvService.DECODER_TYPE, "DMR", PlaylistCsvService.FREQUENCY_MHZ, "452.500",
                PlaylistCsvService.AUTO_START, "true", PlaylistCsvService.AUTO_START_ORDER, "2"),
            row(PlaylistCsvService.RECORD_TYPE_ALIAS_ID, PlaylistCsvService.ALIAS_LIST, "Metro",
                PlaylistCsvService.ALIAS_NAME, "Police Dispatch", PlaylistCsvService.ALIAS_GROUP, "Law",
                PlaylistCsvService.ALIAS_COLOR, "#0000FF", PlaylistCsvService.CALL_PRIORITY, "3",
                PlaylistCsvService.RECORDABLE, "false", PlaylistCsvService.IDENTIFIER_TYPE, "TALKGROUP",
                PlaylistCsvService.PROTOCOL, "APCO25", PlaylistCsvService.TALKGROUP, "101")
        )), channelModel, aliasModel);

        assertEquals(1, update.getChannelsUpdated());
        assertEquals(1, update.getAliasesUpdated());
        assertEquals(1, update.getAliasIdentifiersMatched());
        assertEquals(1, channelModel.getChannels().size());
        assertEquals(1, aliasModel.getAliases().size());

        Channel channel = channelModel.getChannels().getFirst();
        assertEquals(DecoderType.DMR, channel.getDecodeConfiguration().getDecoderType());
        SourceConfigTuner source = assertInstanceOf(SourceConfigTuner.class, channel.getSourceConfiguration());
        assertEquals(452500000L, source.getFrequency());
        assertTrue(channel.isAutoStart());
        assertEquals(2, channel.getAutoStartOrder());

        Alias alias = aliasModel.getAliases().getFirst();
        assertEquals("Law", alias.getGroup());
        assertEquals(3, alias.getPlaybackPriority());
        assertEquals(2, alias.getAliasIdentifiers().size()); //Original talkgroup plus updated priority.
    }

    @Test
    void importReportsValidationFailuresAndAppliesValidRows() throws Exception
    {
        AliasModel aliasModel = new AliasModel();
        ChannelModel channelModel = new ChannelModel(aliasModel);

        PlaylistCsvResult result = mService.importCsv(new StringReader(csv(
            row("BAD"),
            row(PlaylistCsvService.RECORD_TYPE_CHANNEL, PlaylistCsvService.CHANNEL_SYSTEM, "Metro",
                PlaylistCsvService.CHANNEL_SITE, "North", PlaylistCsvService.CHANNEL_NAME, "Valid",
                PlaylistCsvService.DECODER_TYPE, "P25_PHASE1", PlaylistCsvService.FREQUENCY_MHZ, "851.0125",
                PlaylistCsvService.AUTO_START, "false"),
            row(PlaylistCsvService.RECORD_TYPE_CHANNEL, PlaylistCsvService.CHANNEL_SYSTEM, "Metro",
                PlaylistCsvService.CHANNEL_SITE, "North", PlaylistCsvService.CHANNEL_NAME, "Bad Decoder",
                PlaylistCsvService.DECODER_TYPE, "NOPE", PlaylistCsvService.FREQUENCY_MHZ, "851.0125",
                PlaylistCsvService.AUTO_START, "false"),
            row(PlaylistCsvService.RECORD_TYPE_CHANNEL, PlaylistCsvService.CHANNEL_SYSTEM, "Metro",
                PlaylistCsvService.CHANNEL_SITE, "North", PlaylistCsvService.CHANNEL_NAME, "Bad Frequency",
                PlaylistCsvService.DECODER_TYPE, "P25_PHASE1", PlaylistCsvService.FREQUENCY_MHZ, "not-a-number",
                PlaylistCsvService.AUTO_START, "false"),
            row(PlaylistCsvService.RECORD_TYPE_ALIAS_ID, PlaylistCsvService.ALIAS_LIST, "Metro",
                PlaylistCsvService.ALIAS_NAME, "Bad Protocol", PlaylistCsvService.RECORDABLE, "false",
                PlaylistCsvService.IDENTIFIER_TYPE, "TALKGROUP", PlaylistCsvService.PROTOCOL, "NOPE",
                PlaylistCsvService.TALKGROUP, "101"),
            row(PlaylistCsvService.RECORD_TYPE_ALIAS_ID, PlaylistCsvService.ALIAS_LIST, "Metro",
                PlaylistCsvService.ALIAS_NAME, "Bad Range", PlaylistCsvService.RECORDABLE, "false",
                PlaylistCsvService.IDENTIFIER_TYPE, "TALKGROUP_RANGE", PlaylistCsvService.PROTOCOL, "APCO25",
                PlaylistCsvService.TALKGROUP_MIN, "200", PlaylistCsvService.TALKGROUP_MAX, "100")
        )), channelModel, aliasModel);

        assertEquals(1, result.getChannelsCreated());
        assertEquals(1, channelModel.getChannels().size());
        assertEquals(5, result.getErrors().size());
    }

    private String csv(String ... rows)
    {
        return String.join(",", PlaylistCsvService.HEADERS) + "\n" + String.join("\n", rows) + "\n";
    }

    private String row(String recordType, String ... headerValuePairs)
    {
        String[] row = new String[PlaylistCsvService.HEADERS.length];
        Arrays.fill(row, "");
        row[0] = recordType;

        for(int x = 0; x < headerValuePairs.length; x += 2)
        {
            String header = headerValuePairs[x];
            String value = headerValuePairs[x + 1];

            for(int y = 0; y < PlaylistCsvService.HEADERS.length; y++)
            {
                if(PlaylistCsvService.HEADERS[y].equals(header))
                {
                    row[y] = value;
                    break;
                }
            }
        }

        return String.join(",", row);
    }
}
