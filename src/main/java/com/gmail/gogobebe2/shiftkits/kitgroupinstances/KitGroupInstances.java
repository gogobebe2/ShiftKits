package com.gmail.gogobebe2.shiftkits.kitgroupinstances;

import com.gmail.gogobebe2.shiftkits.kit.KitGroup;

import java.util.ArrayList;
import java.util.List;

public final class KitGroupInstances {
    private static List<KitGroup> kitGroups = new ArrayList<>();

    public static List<KitGroup> getInstances() {
        if (kitGroups.isEmpty()) {
            kitGroups.add(new ArcherKitGroup().getKitGroup());
            kitGroups.add(new MinerKitGroup().getKitGroup());
            kitGroups.add(new SwordsmanKitGroup().getKitGroup());
            kitGroups.add(new RogueKitGroup().getKitGroup());
        }
        return kitGroups;
    }
}
