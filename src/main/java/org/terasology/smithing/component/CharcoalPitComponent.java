// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.smithing.component;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.network.Replicate;

public class CharcoalPitComponent implements Component {

    /**
     * The time in milliseconds in-game when the charcoal pit begins to burn, otherwise holds the last time the charcoal
     * pit began to burn
     */
    @Replicate
    public long burnStartWorldTime;

    /**
     * The time in milliseconds in-game when the charcoal pit stops burning, otherwise holds the last time the charcoal
     * pit finished burning
     */
    @Replicate
    public long burnFinishWorldTime;

    /**
     * Minimum number of logs
     */
    @Replicate
    public int minimumLogCount;

    /**
     * Maximum number of logs
     */
    @Replicate
    public int maximumLogCount;

    /**
     * Number of input slots for fuel
     */
    @Replicate
    public int inputSlotCount;

    /**
     * Number of output slots for charcoal
     */
    @Replicate
    public int outputSlotCount;
}
