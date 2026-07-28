package com.millenaire.milltools.raid;

import com.millenaire.milltools.MillToolsMod;
import com.millenaire.milltools.RaidPhase;
import com.millenaire.milltools.config.RaidConfig;
import com.millenaire.milltools.raid.defense.DefenderManager;
import com.millenaire.milltools.raid.mob.MobRegistry;
import com.millenaire.milltools.raid.wave.RaidMobEntry;
import com.millenaire.milltools.raid.wave.WaveDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.entity.MillVillager;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageSavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class RaidInstance {

    public enum State { STARTING, ACTIVE, WON, CANCELLED }

    private final VillageId villageId;
    private final BlockPos center;
    private final String villageName;
    private final RaidPhase phase;
    private final WaveDefinition wave;
    private final RaidConfig config;
    private final Set<UUID> activeMobs = new HashSet<>();
    private State state = State.STARTING;
    private final ServerBossEvent bossBar;
    private int totalMobs = 0;
    private final Map<UUID, UUID> mobToDisplay = new HashMap<>();
    private final Set<UUID> pendingDisplayCleanup = new HashSet<>();
    private final DefenderManager defenderManager = new DefenderManager();

    public RaidInstance(VillageId villageId, BlockPos center, String villageName,
                        RaidPhase phase, WaveDefinition wave, RaidConfig config) {
        this.villageId = villageId;
        this.center = center;
        this.villageName = villageName;
        this.phase = phase;
        this.wave = wave;
        this.config = config;
        BossEvent.BossBarColor color = (phase == RaidPhase.PHASE_1)
                ? BossEvent.BossBarColor.RED
                : BossEvent.BossBarColor.PURPLE;
        this.bossBar = new ServerBossEvent(
                Component.translatable("milltools.bossbar.title", villageName),
                color,
                BossEvent.BossBarOverlay.PROGRESS
        );
    }

    public void tick(ServerLevel level) {
        switch (state) {
            case STARTING -> {
                spawnMobs(level);
                state = State.ACTIVE;
                VillageSavedData.get(level).getVillageManager().getAllVillages().stream()
                        .filter(v -> v.getId().equals(villageId))
                        .findFirst()
                        .ifPresent(village -> defenderManager.activateDefenders(village, level, activeMobs));
            }
            case ACTIVE -> {
                activeMobs.removeIf(uuid -> {
                    Entity e = level.getEntity(uuid);
                    if (e == null || !e.isAlive()) {
                        UUID dispUuid = mobToDisplay.remove(uuid);
                        if (dispUuid != null) {
                            Entity disp = level.getEntity(dispUuid);
                            if (disp != null) disp.discard();
                            else pendingDisplayCleanup.add(dispUuid);
                        }
                        return true;
                    }
                    return false;
                });
                pendingDisplayCleanup.removeIf(uuid -> {
                    Entity disp = level.getEntity(uuid);
                    if (disp != null) { disp.discard(); return true; }
                    return false;
                });
                defenderManager.tick(level);
                for (Map.Entry<UUID, UUID> entry : mobToDisplay.entrySet()) {
                    Entity mob = level.getEntity(entry.getKey());
                    Entity disp = level.getEntity(entry.getValue());
                    if (mob != null && disp != null) {
                        disp.moveTo(mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ());
                    }
                }
                bossBar.setProgress(totalMobs > 0 ? (float) activeMobs.size() / totalMobs : 1f);
                bossBar.removeAllPlayers();
                for (ServerPlayer p : level.players()) {
                    if (p.blockPosition().closerThan(center, config.villageRadius)) {
                        bossBar.addPlayer(p);
                    }
                }
                if (activeMobs.isEmpty()) {
                    defenderManager.deactivateDefenders(level);
                    cleanupOrphanedMarkers(level);
                    bossBar.removeAllPlayers();
                    state = State.WON;
                    MillToolsMod.LOGGER.info("[MillTools] Raid WON sur '{}'", villageName);
                    notifyNearbyPlayers(level, Component.translatable("milltools.raid.victory", villageName));
                }
            }
            default -> {} // WON / CANCELLED : états terminaux
        }
    }

    private void spawnMobs(ServerLevel level) {
        long worldDay = level.getGameTime() / 24000L;
        List<RaidMobEntry> entries = wave.resolveMobs(config, worldDay);
        Random rng = new Random();
        int spawnedCount = 0;

        for (RaidMobEntry entry : entries) {
            for (int i = 0; i < entry.count(); i++) {
                Mob mob = MobRegistry.create(entry.mobType(), level);
                if (mob == null) {
                    MillToolsMod.LOGGER.warn("[MillTools] Type de mob inconnu : {}", entry.mobType());
                    continue;
                }

                double angle = rng.nextDouble() * 2 * Math.PI;
                int radius = (int) config.villageRadius + rng.nextInt(21); // [villageRadius, villageRadius+20]
                int dx = (int) (Math.cos(angle) * radius);
                int dz = (int) (Math.sin(angle) * radius);

                BlockPos spawnPos = level.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        center.offset(dx, 0, dz)
                );

                mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0f, 0f);
                mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, MillVillager.class, true));

                if (level.addFreshEntity(mob)) {
                    activeMobs.add(mob.getUUID());
                    spawnedCount++;

                    // Empêche le despawn vanilla et force le mob à marcher vers le village
                    mob.setPersistenceRequired();
                    mob.restrictTo(center, 20);

                    if (mob instanceof PathfinderMob pm) {
                        pm.goalSelector.addGoal(1, new BreakDoorGoal(pm,
                                diff -> diff != Difficulty.PEACEFUL));
                        pm.goalSelector.addGoal(7, new MoveTowardsRestrictionGoal(pm, 1.0));
                    }

                    ArmorStand label = new ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, level);
                    label.setCustomName(Component.literal("⚔ Raid").withStyle(s -> s.withColor(0xFF0000)));
                    label.setCustomNameVisible(true);
                    label.setInvisible(true);
                    label.setNoGravity(true);
                    label.addTag("milltools_marker");
                    label.moveTo(mob.getX(), mob.getY() + mob.getBbHeight() - 0.5, mob.getZ(), 0f, 0f);
                    if (level.addFreshEntity(label)) {
                        mobToDisplay.put(mob.getUUID(), label.getUUID());
                    }
                }
            }
        }

        this.totalMobs = activeMobs.size();
        bossBar.setProgress(1.0f);

        MillToolsMod.LOGGER.info("[MillTools] {} mobs spawnés pour le raid {} sur '{}'",
                spawnedCount, phase, villageName);

        if (spawnedCount > 0) {
            notifyNearbyPlayers(level, Component.translatable("milltools.raid.attack", villageName));
        }
    }

    private void cleanupOrphanedMarkers(ServerLevel level) {
        double r = config.villageRadius + 30;
        AABB area = AABB.ofSize(Vec3.atCenterOf(center), r * 2, 256, r * 2);
        level.getEntitiesOfClass(ArmorStand.class, area,
                stand -> stand.getTags().contains("milltools_marker"))
            .forEach(Entity::discard);
        pendingDisplayCleanup.clear();
    }

    private void notifyNearbyPlayers(ServerLevel level, Component msg) {
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(center, 128)) {
                player.sendSystemMessage(msg);
            }
        }
    }

    public void cancel(ServerLevel level) {
        defenderManager.deactivateDefenders(level);
        for (UUID dispUuid : mobToDisplay.values()) {
            Entity disp = level.getEntity(dispUuid);
            if (disp != null) disp.discard();
        }
        mobToDisplay.clear();
        cleanupOrphanedMarkers(level);
        bossBar.removeAllPlayers();
        for (UUID uuid : activeMobs) {
            Entity e = level.getEntity(uuid);
            if (e != null) e.discard();
        }
        activeMobs.clear();
        state = State.CANCELLED;
    }

    public void expire(ServerLevel level) {
        defenderManager.deactivateDefenders(level);
        for (UUID uuid : activeMobs) {
            Entity e = level.getEntity(uuid);
            if (e != null) e.discard();
        }
        activeMobs.clear();
        for (UUID dispUuid : mobToDisplay.values()) {
            Entity disp = level.getEntity(dispUuid);
            if (disp != null) disp.discard();
        }
        mobToDisplay.clear();
        cleanupOrphanedMarkers(level);
        bossBar.removeAllPlayers();
        state = State.CANCELLED;
        notifyNearbyPlayers(level,
                Component.translatable("milltools.raid.expired", villageName));
    }

    public boolean isFinished()        { return state == State.WON || state == State.CANCELLED; }
    public State getState()            { return state; }
    public BlockPos getVillageCenter() { return center; }
    public String getVillageName()     { return villageName; }
    public VillageId getVillageId()    { return villageId; }
}
