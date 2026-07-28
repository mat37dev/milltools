package com.millenaire.milltools.raid.wave;

import com.millenaire.milltools.RaidPhase;
import com.millenaire.milltools.config.RaidConfig;

import java.util.List;

public interface WaveDefinition {
    List<RaidMobEntry> resolveMobs(RaidConfig config, long worldDay);
    RaidPhase getPhase();
}
