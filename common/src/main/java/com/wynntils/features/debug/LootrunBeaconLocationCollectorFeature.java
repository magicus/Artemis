/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.debug;

import com.google.common.collect.ComparisonChain;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.storage.RegisterStorage;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.models.beacons.type.Beacon;
import com.wynntils.models.lootrun.event.LootrunBeaconSelectedEvent;
import com.wynntils.models.lootrun.type.LootrunLocation;
import com.wynntils.models.lootrun.type.LootrunTaskType;
import com.wynntils.utils.mc.type.Location;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@StartDisabled
@ConfigCategory(Category.DEBUG)
public class LootrunBeaconLocationCollectorFeature extends Feature {
    // Dumping to a storage is a bit weird,
    // but it's the easiest way to get the data out of the game for people to share.
    @RegisterStorage
    private final Storage<Map<LootrunLocation, Set<TaskLocation>>> tasks = new Storage<>(new TreeMap<>());

    @SubscribeEvent
    public void onLootrunBeaconSelected(LootrunBeaconSelectedEvent event) {
        Beacon beacon = event.getBeacon();

        if (!beacon.color().isUsedInLootruns()) return;

        Optional<LootrunTaskType> currentTaskTypeOpt = Models.Lootrun.getTaskType();
        if (currentTaskTypeOpt.isEmpty()) return;

        Optional<LootrunLocation> currentLocationOpt = Models.Lootrun.getLocation();
        if (currentLocationOpt.isEmpty()) return;

        tasks.get().putIfAbsent(currentLocationOpt.get(), new TreeSet<>());
        tasks.get().get(currentLocationOpt.get()).add(new TaskLocation(beacon.location(), currentTaskTypeOpt.get()));
        tasks.touched();
    }

    private record TaskLocation(Location location, LootrunTaskType taskType) implements Comparable<TaskLocation> {
        @Override
        public int compareTo(LootrunBeaconLocationCollectorFeature.TaskLocation taskLocation) {
            return ComparisonChain.start()
                    .compare(location.x(), taskLocation.location.x())
                    .compare(location.y(), taskLocation.location.y())
                    .compare(location.z(), taskLocation.location.z())
                    .compare(taskType, taskLocation.taskType)
                    .result();
        }
    }
}
