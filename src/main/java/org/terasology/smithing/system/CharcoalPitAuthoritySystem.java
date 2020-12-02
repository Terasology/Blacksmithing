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

import org.terasology.engine.Time;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.particles.components.ParticleEmitterComponent;
import org.terasology.registry.In;
import org.terasology.smithing.component.CharcoalPitComponent;
import org.terasology.smithing.event.OpenCharcoalPitRequest;
import org.terasology.smithing.event.ProduceCharcoalRequest;
import org.terasology.world.block.regions.BlockRegionComponent;

/*
* Controls the processes of the charcoal pit and alters the players inventory
*/
@RegisterSystem(value = RegisterMode.AUTHORITY)
public class CharcoalPitAuthoritySystem extends BaseComponentSystem {
    public static final String PRODUCE_CHARCOAL_ACTION_PREFIX = "Smithing:ProduceCharcoal|";
    @In
    private Time time;
    @In
    private PrefabManager prefabManager;
    @In
    private EntityManager entityManager;
    @In
    private DelayManager delayManager;
    @In
    private InventoryManager inventoryManager;

    /*
    * Called upon when the charcoal pit is activated by a user
    *
    * @param  event the event associated with activating the charcoal pit
    * @param  entity the entity that activated the charcoal pit
    * @param  charcoalPit the charcoal pit component being activated
    */
    @ReceiveEvent
    public void userActivatesCharcoalPit(ActivateEvent event, EntityRef entity, CharcoalPitComponent charcoalPit) {
        entity.send(new OpenCharcoalPitRequest());
    }

    /*
    * Removes logs from the players inventory to begin the production of charcoal
    *
    * @param  event the event associated with a request to produce charcoal
    * @param  entity the entity that is trying to produce charcoal
    * @param  charcoalPit the component of the charcoal pit that is producing charcoal
    * @param  inventoryComponent the inventory component of the entity
    */
    @ReceiveEvent
    public void startBurningCharcoal(ProduceCharcoalRequest event, EntityRef entity,
                                     CharcoalPitComponent charcoalPit, InventoryComponent inventoryComponent) {
        int logCount = CharcoalPitUtils.getLogCount(entity);

        if (CharcoalPitUtils.canBurnCharcoal(logCount, entity)) {
            // Remove logs from inventory
            for (int i = 0; i < charcoalPit.inputSlotCount; i++) {
                EntityRef itemInSlot = InventoryUtils.getItemAt(entity, i);
                if (itemInSlot.exists()) {
                    inventoryManager.removeItem(entity, entity, itemInSlot, true);
                }
            }

            int charcoalCount = CharcoalPitUtils.getResultCharcoalCount(logCount, entity);
            int burnLength = 5 * 60 * 1000;

            // Set burn length
            charcoalPit.burnStartWorldTime = time.getGameTimeInMs();
            charcoalPit.burnFinishWorldTime = charcoalPit.burnStartWorldTime + burnLength;
            entity.saveComponent(charcoalPit);

            Prefab prefab = prefabManager.getPrefab("Smithing:CharcoalPitSmoke");
            for (Component c : prefab.iterateComponents()) {
                entity.addComponent(c);
            }

            BlockRegionComponent region = entity.getComponent(BlockRegionComponent.class);
            if (region != null) {
                org.joml.Vector3f center = region.region.center(new org.joml.Vector3f());
                org.joml.Vector3i max = region.region.getMax(new org.joml.Vector3i());

                LocationComponent location = entity.getComponent(LocationComponent.class);
                location.setWorldPosition(new Vector3f(center.x - 0.5f, max.y + 1, center.z - 0.5f));
                entity.saveComponent(location);
            }

            delayManager.addDelayedAction(entity, PRODUCE_CHARCOAL_ACTION_PREFIX + charcoalCount, burnLength);
        }
    }

    /*
    * Adds the produced charcoal to the charcoal pit's inventory
    *
    * @param  event the event corresponding to triggering a delayed action
    * @param  entity the entity triggering the delayed action
    * @param  charcoalPit the component of the charcoal pit
    * @param  inventoryComponent the inventory component of the entity
    */
    @ReceiveEvent
    public void charcoalBurningFinished(DelayedActionTriggeredEvent event, EntityRef entity,
                                        CharcoalPitComponent charcoalPit, InventoryComponent inventoryComponent) {
        String actionId = event.getActionId();
        if (actionId.startsWith(PRODUCE_CHARCOAL_ACTION_PREFIX)) {

            entity.removeComponent(ParticleEmitterComponent.class);

            int count = Integer.parseInt(actionId.substring(PRODUCE_CHARCOAL_ACTION_PREFIX.length()));
            for (int i = charcoalPit.inputSlotCount; i < charcoalPit.inputSlotCount + charcoalPit.outputSlotCount; i++) {
                EntityRef itemInSlot = InventoryUtils.getItemAt(entity, i);
                if (!itemInSlot.exists()) {
                    int toAdd = Math.min(count, 99);
                    EntityRef charcoalItem = entityManager.create("Smithing:Charcoal");
                    ItemComponent item = charcoalItem.getComponent(ItemComponent.class);
                    item.stackCount = (byte) toAdd;
                    charcoalItem.saveComponent(item);
                    if (!inventoryManager.giveItem(entity, entity, charcoalItem, i)) {
                        charcoalItem.destroy();
                    } else {
                        count -= toAdd;
                    }
                }
                if (count == 0) {
                    break;
                }
            }
        }
    }
}
