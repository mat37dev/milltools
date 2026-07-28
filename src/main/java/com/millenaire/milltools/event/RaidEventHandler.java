package com.millenaire.milltools.event;

import com.millenaire.milltools.RaidPhase;
import com.millenaire.milltools.command.RaidState;
import com.millenaire.milltools.config.RaidConfig;
import com.millenaire.milltools.raid.RaidManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;

import java.util.HashSet;
import java.util.Set;

public class RaidEventHandler {

    // Clés = gameDay * 10 + numéro de phase — garantit un seul déclenchement par phase par nuit
    private final Set<Long> triggeredPhases = new HashSet<>();

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        if (level.isClientSide()) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        ServerLevel serverLevel = (ServerLevel) level;
        RaidConfig config = RaidConfig.INSTANCE;

        if (!RaidState.get(serverLevel).isEnabled()) return;
        if (!config.enabled) return;

        long dayTime = serverLevel.getDayTime() % 24000L;
        long gameDay = serverLevel.getGameTime() / 24000L;

        // Tick permanent pour mettre à jour tous les raids actifs
        RaidManager.tick(serverLevel);

        long phase1Key = gameDay * 10L + 1L;
        long phase2Key = gameDay * 10L + 2L;

        if (dayTime >= config.phase1Tick && dayTime < config.phase1Tick + 20L
                && !triggeredPhases.contains(phase1Key)) {
            triggeredPhases.add(phase1Key);
            triggerRaidsForPhase(serverLevel, RaidPhase.PHASE_1);
        }

        if (dayTime >= config.phase2Tick && dayTime < config.phase2Tick + 20L
                && !triggeredPhases.contains(phase2Key)) {
            triggeredPhases.add(phase2Key);
            triggerRaidsForPhase(serverLevel, RaidPhase.PHASE_2);
        }

        long dawnKey = gameDay * 10L + 3L;
        if (dayTime >= 23000L && dayTime < 23020L
                && !triggeredPhases.contains(dawnKey)) {
            triggeredPhases.add(dawnKey);
            RaidManager.expireAll(serverLevel);
        }

        // Purge des anciennes clés pour éviter la fuite mémoire
        long threshold = (gameDay - 2L) * 10L;
        triggeredPhases.removeIf(key -> key < threshold);
    }

    private void triggerRaidsForPhase(ServerLevel level, RaidPhase phase) {
        VillageSavedData villageData = VillageSavedData.get(level);
        VillageManager manager = villageData.getVillageManager();

        Set<VillageId> targeted = new HashSet<>();

        for (ServerPlayer player : level.players()) {
            BlockPos playerPos = player.blockPosition();
            Village village = manager.findNearestVillage(playerPos, RaidConfig.INSTANCE.villageRadius);

            if (village == null) continue;
            if (!village.isActive()) continue;
            if (village.isLoneBuilding()) continue;

            VillageId villageId = village.getId();
            if (targeted.contains(villageId)) continue;
            if (RaidManager.hasActiveRaid(villageId)) continue;

            targeted.add(villageId);
            RaidManager.startRaid(village, phase, level);
        }
    }
}
