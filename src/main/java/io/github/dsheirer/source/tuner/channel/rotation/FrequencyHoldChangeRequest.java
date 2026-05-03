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

package io.github.dsheirer.source.tuner.channel.rotation;

import io.github.dsheirer.module.ModuleEventBusMessage;

/**
 * Request to enable or disable frequency hold for a multi-frequency tuner source.
 */
public class FrequencyHoldChangeRequest extends ModuleEventBusMessage
{
    private long mFrequency;
    private boolean mHold;

    /**
     * Constructs an instance.
     * @param frequency to hold
     * @param hold true to hold or false to resume rotation
     */
    public FrequencyHoldChangeRequest(long frequency, boolean hold)
    {
        mFrequency = frequency;
        mHold = hold;
    }

    /**
     * Frequency to hold.
     */
    public long getFrequency()
    {
        return mFrequency;
    }

    /**
     * Indicates if this request enables frequency hold.
     */
    public boolean isHoldRequest()
    {
        return mHold;
    }

    /**
     * Indicates if this request disables frequency hold.
     */
    public boolean isReleaseRequest()
    {
        return !mHold;
    }

    /**
     * Utility method to create a hold request.
     */
    public static FrequencyHoldChangeRequest hold(long frequency)
    {
        return new FrequencyHoldChangeRequest(frequency, true);
    }

    /**
     * Utility method to create a release request.
     */
    public static FrequencyHoldChangeRequest release()
    {
        return new FrequencyHoldChangeRequest(0, false);
    }
}
