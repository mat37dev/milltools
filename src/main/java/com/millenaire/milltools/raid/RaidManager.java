package com.millenaire.milltools.raid;

import com.millenaire.milltools.MillToolsMod;
import com.millenaire.milltools.RaidPhase;
import com.millenaire.milltools.config.RaidConfig;
import com.millenaire.milltools.raid.wave.PredefinedWave;
import com.millenaire.milltools.raid.wave.RandomPointWave;
import com.millenaire.milltools.raid.wave.WaveDefinition;
import net.minecraft.server.level.ServerLevel;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class RaidManager {

    private static final Map<VillageId, RaidInstance> activeRaids = new HashMap<>();
    private static final Random RNG = new Random();
    private static RaidConfig config;

    private RaidManager() {}

    public static void init(RaidConfig cfg) {
        config = cfg;
    }

    public static void startRaid(Village village, RaidPhase phase, ServerLevel level) {
        VillageId id = village.getId();
        if (activeRaids.containsKey(id)) return;
        if (config == null) {
            MillToolsMod.LOGGER.error("[MillTools] RaidManager.init() non appelé avant startRaid !");
            return;
        }
        MillToolsMod.LOGGER.info("[MillTools] Démarrage raid {} sur village '{}' ({})",
                phase, village.getVillageName(), id);
        WaveDefinition wave = selectWave(config, phase);
        RaidInstance instance = new RaidInstance(id, village.getCenter(), village.getVillageName(), phase, wave, config);
        activeRaids.put(id, instance);
    }

    public static boolean hasActiveRaid(VillageId villageId) {
        return activeRaids.containsKey(villageId);
    }

    public static int getActiveRaidCount() {
        return activeRaids.size();
    }

    public static void tick(ServerLevel level) {
        activeRaids.entrySet().removeIf(entry -> {
            RaidInstance raid = entry.getValue();
            if (config != null && !isAnyPlayerNearby(level, raid.getVillageCenter(), config.villageRadius * 2)) {
                raid.cancel(level);
            }
            raid.tick(level);
            return raid.isFinished();
        });
    }

    private static boolean isAnyPlayerNearby(ServerLevel level, BlockPos center, double distance) {
        return level.players().stream().anyMatch(p -> p.blockPosition().closerThan(center, distance));
    }

    public static void clearAll() {
        activeRaids.clear();
    }

    public static void expireAll(ServerLevel level) {
        for (RaidInstance raid : activeRaids.values()) {
            raid.expire(level);
        }
        activeRaids.clear();
        MillToolsMod.LOGGER.info("[MillTools] Tous les raids expirés à l'aube.");
    }

    private static WaveDefinition selectWave(RaidConfig cfg, RaidPhase phase) {
        List<RaidConfig.PredefinedRaidDef> matching = cfg.predefinedRaids.stream()
                .filter(r -> r.phase() == phase)
                .collect(Collectors.toList());

        if (!matching.isEmpty() && RNG.nextBoolean()) {
            return new PredefinedWave(matching.get(RNG.nextInt(matching.size())));
        }
        return new RandomPointWave(phase, new Random());
    }
}
