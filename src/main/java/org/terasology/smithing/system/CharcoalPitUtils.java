/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.smithing.system;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.module.inventory.systems.InventoryUtils;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.smithing.component.CharcoalPitComponent;
import org.terasology.workstationCrafting.component.CraftingStationIngredientComponent;

/*
* Provides utilities for the logic and variables of the charcoal pit
*/
public final class CharcoalPitUtils {
    private CharcoalPitUtils() {
    }

    /*
    * Calculates the number of produced charcoal
    *
    * @param  logCount the number of inputted logs
    * @param  charcoalPitEntity the charcoal pit entity that has produced the charcoal
    *
    * @return the number of charcoal
    */
    public static int getResultCharcoalCount(int logCount, EntityRef charcoalPitEntity) {
        CharcoalPitComponent charcoalPit = charcoalPitEntity.getComponent(CharcoalPitComponent.class);
        int min = charcoalPit.minimumLogCount;
        int max = charcoalPit.maximumLogCount;

        return Math.round(1f * logCount * logCount / max);
    }

    /*
    * Retrieves the number of logs inputted into the charcoal pit
    *
    * @param  charcoalPitEntity the charcoal pit entity that contains the logs
    *
    * @return the number of logs
    */
    public static int getLogCount(EntityRef charcoalPitEntity) {
        CharcoalPitComponent charcoalPit = charcoalPitEntity.getComponent(CharcoalPitComponent.class);
        int logCount = 0;
        for (int i = 0; i < charcoalPit.inputSlotCount; i++) {
            EntityRef itemInSlot = InventoryUtils.getItemAt(charcoalPitEntity, i);
            if (itemInSlot.hasComponent(CraftingStationIngredientComponent.class)
                    && itemInSlot.getComponent(CraftingStationIngredientComponent.class).type.equals("WorkstationCrafting:wood")) {
                logCount += itemInSlot.getComponent(ItemComponent.class).stackCount;
            } else if (itemInSlot.exists()) {
                return -1;
            }
        }
        return logCount;
    }

    /*
    * Computes if the charcoal pit is able to burn the logs and produce charcoal
    *
    * @param  logCount the number of inputted logs
    * @param  charcoalPitEntity the charcoal pit entity that is being tested
    *
    * @return true if the charcoal pit can successfully produce the the charcoal and false if otherwise
    */
    public static boolean canBurnCharcoal(int logCount, EntityRef charcoalPitEntity) {
        CharcoalPitComponent charcoalPit = charcoalPitEntity.getComponent(CharcoalPitComponent.class);

        int resultCharcoalCount = getResultCharcoalCount(logCount, charcoalPitEntity);
        int availableCharcoalPlace = 0;
        for (int i = charcoalPit.inputSlotCount; i < charcoalPit.inputSlotCount + charcoalPit.outputSlotCount; i++) {
            EntityRef itemInSlot = InventoryUtils.getItemAt(charcoalPitEntity, i);
            if (!itemInSlot.exists()) {
                availableCharcoalPlace += 99;
            }
        }

        return logCount >= charcoalPit.minimumLogCount && logCount <= charcoalPit.maximumLogCount && resultCharcoalCount <= availableCharcoalPlace;
    }
}
