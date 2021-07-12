// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.smithing.system;

import org.joml.Vector3f;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.prefab.PrefabManager;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.delay.DelayManager;
import org.terasology.engine.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.particles.components.ParticleEmitterComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.block.regions.BlockRegionComponent;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.systems.InventoryManager;
import org.terasology.module.inventory.systems.InventoryUtils;
import org.terasology.smithing.component.CharcoalPitComponent;
import org.terasology.smithing.event.OpenCharcoalPitRequest;
import org.terasology.smithing.event.ProduceCharcoalRequest;

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
                Vector3f position =
                        region.region.center(new Vector3f())
                                .sub(0.5f, 0, 0.5f)
                                .setComponent(1, region.region.maxY() + 1);

                LocationComponent location = entity.getComponent(LocationComponent.class);
                location.setWorldPosition(position);
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
