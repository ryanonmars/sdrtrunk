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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result for a playlist CSV import or export operation.
 */
public class PlaylistCsvResult
{
    private int mChannelRows;
    private int mAliasIdentifierRows;
    private int mChannelsCreated;
    private int mChannelsUpdated;
    private int mAliasesCreated;
    private int mAliasesUpdated;
    private int mAliasIdentifiersAdded;
    private int mAliasIdentifiersMatched;
    private List<String> mErrors = new ArrayList<>();

    public int getChannelRows()
    {
        return mChannelRows;
    }

    public void incrementChannelRows()
    {
        mChannelRows++;
    }

    public int getAliasIdentifierRows()
    {
        return mAliasIdentifierRows;
    }

    public void incrementAliasIdentifierRows()
    {
        mAliasIdentifierRows++;
    }

    public int getChannelsCreated()
    {
        return mChannelsCreated;
    }

    public void incrementChannelsCreated()
    {
        mChannelsCreated++;
    }

    public int getChannelsUpdated()
    {
        return mChannelsUpdated;
    }

    public void incrementChannelsUpdated()
    {
        mChannelsUpdated++;
    }

    public int getAliasesCreated()
    {
        return mAliasesCreated;
    }

    public void incrementAliasesCreated()
    {
        mAliasesCreated++;
    }

    public int getAliasesUpdated()
    {
        return mAliasesUpdated;
    }

    public void incrementAliasesUpdated()
    {
        mAliasesUpdated++;
    }

    public int getAliasIdentifiersAdded()
    {
        return mAliasIdentifiersAdded;
    }

    public void incrementAliasIdentifiersAdded()
    {
        mAliasIdentifiersAdded++;
    }

    public int getAliasIdentifiersMatched()
    {
        return mAliasIdentifiersMatched;
    }

    public void incrementAliasIdentifiersMatched()
    {
        mAliasIdentifiersMatched++;
    }

    public List<String> getErrors()
    {
        return Collections.unmodifiableList(mErrors);
    }

    public void addError(long rowNumber, String message)
    {
        mErrors.add("Row " + rowNumber + ": " + message);
    }

    public boolean hasErrors()
    {
        return !mErrors.isEmpty();
    }

    public boolean hasChanges()
    {
        return mChannelsCreated > 0 || mChannelsUpdated > 0 || mAliasesCreated > 0 || mAliasesUpdated > 0 ||
            mAliasIdentifiersAdded > 0;
    }
}
