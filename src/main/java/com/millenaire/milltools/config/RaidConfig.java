package com.millenaire.milltools.config;

import com.millenaire.milltools.RaidPhase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RaidConfig {

    // Instance statique accessible depuis les Mixins
    public static RaidConfig INSTANCE = defaults();

    // [raid] — système de raids nocturnes, non finalisé, déconseillé de l'activer en production
    public boolean enabled = false;
    public double villageRadius = 80.0;
    public long phase1Tick = 13000L;
    public long phase2Tick = 18000L;

    // [village_enemies] — mobs supplémentaires toujours traités comme ennemis par tous les villages
    // (en plus des Monster vanilla que Millenaire reconnaît déjà nativement, hors Creeper)
    public Set<String> extraEnemyTypes = new LinkedHashSet<>(Set.of("SLIME"));
    // Distance minimale (blocs, depuis les côtés du bâtiment, pas son centre) à laquelle ces mobs
    // peuvent apparaître naturellement près d'un bâtiment Millenaire.
    public int enemyBuildingBuffer = 10;

    // [dev]
    public boolean devAutoPrestige = false;
    public String devPrestigeCulture = "norman";

    // [difficulty]
    public int phase1Points = 20;
    public int phase2Points = 40;
    public double pointsPerDay = 0.5;

    // [mob_costs]
    public Map<String, Integer> mobCosts = new LinkedHashMap<>();

    // [mob_weights]
    public Map<String, Integer> mobWeights = new LinkedHashMap<>();

    // Raids prédéfinis
    public List<PredefinedRaidDef> predefinedRaids = new ArrayList<>();

    public record PredefinedRaidDef(String name, RaidPhase phase, Map<String, Integer> mobs) {}

    public static RaidConfig defaults() {
        RaidConfig c = new RaidConfig();

        c.mobCosts.put("ZOMBIE", 3);
        c.mobCosts.put("SKELETON", 4);
        c.mobCosts.put("SPIDER", 3);
        c.mobCosts.put("PILLAGER", 6);
        c.mobCosts.put("STRAY", 5);
        c.mobCosts.put("HUSK", 3);

        c.mobWeights.put("ZOMBIE", 60);
        c.mobWeights.put("SKELETON", 25);
        c.mobWeights.put("SPIDER", 20);
        c.mobWeights.put("PILLAGER", 10);
        c.mobWeights.put("STRAY", 5);
        c.mobWeights.put("HUSK", 10);

        c.predefinedRaids.add(new PredefinedRaidDef("raid_starter", RaidPhase.PHASE_1,
                Map.of("ZOMBIE", 3)));
        c.predefinedRaids.add(new PredefinedRaidDef("raid_medium", RaidPhase.PHASE_1,
                linkedMapOf("ZOMBIE", 3, "SKELETON", 2)));
        c.predefinedRaids.add(new PredefinedRaidDef("raid_heavy", RaidPhase.PHASE_2,
                linkedMapOf("ZOMBIE", 4, "SKELETON", 3, "SPIDER", 2)));
        c.predefinedRaids.add(new PredefinedRaidDef("raid_elite", RaidPhase.PHASE_2,
                linkedMapOf("ZOMBIE", 5, "SKELETON", 3, "PILLAGER", 2)));

        return c;
    }

    private static Map<String, Integer> linkedMapOf(Object... pairs) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }
}
