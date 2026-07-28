package com.millenaire.milltools.raid.wave;

import com.millenaire.milltools.RaidPhase;
import com.millenaire.milltools.config.RaidConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomPointWave implements WaveDefinition {

    private final RaidPhase phase;
    private final Random random;

    public RandomPointWave(RaidPhase phase, Random random) {
        this.phase = phase;
        this.random = random;
    }

    @Override
    public List<RaidMobEntry> resolveMobs(RaidConfig config, long worldDay) {
        double budget = (phase == RaidPhase.PHASE_1 ? config.phase1Points : config.phase2Points)
                + worldDay * config.pointsPerDay;

        Map<String, Integer> accumulator = new LinkedHashMap<>();

        while (budget > 0) {
            // Construire la liste des mobs éligibles (coût <= budget et présents dans mobWeights)
            List<String> eligibleMobs = new ArrayList<>();
            List<Integer> eligibleWeights = new ArrayList<>();
            int totalWeight = 0;

            for (Map.Entry<String, Integer> entry : config.mobWeights.entrySet()) {
                String mob = entry.getKey();
                int weight = entry.getValue();
                int cost = config.mobCosts.getOrDefault(mob, Integer.MAX_VALUE);
                if (cost <= budget) {
                    eligibleMobs.add(mob);
                    eligibleWeights.add(weight);
                    totalWeight += weight;
                }
            }

            if (eligibleMobs.isEmpty()) break;

            // Weighted random selection
            int roll = random.nextInt(totalWeight);
            int cumul = 0;
            String selected = eligibleMobs.get(0);
            for (int i = 0; i < eligibleMobs.size(); i++) {
                cumul += eligibleWeights.get(i);
                if (roll < cumul) {
                    selected = eligibleMobs.get(i);
                    break;
                }
            }

            accumulator.merge(selected, 1, Integer::sum);
            budget -= config.mobCosts.get(selected);
        }

        return accumulator.entrySet().stream()
                .map(e -> new RaidMobEntry(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    public RaidPhase getPhase() {
        return phase;
    }
}
