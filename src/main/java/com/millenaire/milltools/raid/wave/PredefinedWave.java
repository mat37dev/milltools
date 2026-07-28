package com.millenaire.milltools.raid.wave;

import com.millenaire.milltools.RaidPhase;
import com.millenaire.milltools.config.RaidConfig;

import java.util.List;

public class PredefinedWave implements WaveDefinition {

    private final RaidConfig.PredefinedRaidDef def;

    public PredefinedWave(RaidConfig.PredefinedRaidDef def) {
        this.def = def;
    }

    @Override
    public List<RaidMobEntry> resolveMobs(RaidConfig config, long worldDay) {
        return def.mobs().entrySet().stream()
                .map(e -> new RaidMobEntry(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    public RaidPhase getPhase() {
        return def.phase();
    }
}
