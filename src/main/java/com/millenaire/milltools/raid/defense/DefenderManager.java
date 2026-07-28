package com.millenaire.milltools.raid.defense;

import com.millenaire.milltools.MillToolsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DefenderManager {

    private static final double SEARCH_RADIUS = 120.0;

    private static Field goalsField;
    public static void setGoalsField(Field f) { goalsField = f; }

    private final Map<UUID, RaidDefenseGoal> injectedGoals = new HashMap<>();
    private Set<UUID> currentRaidMobUUIDs;
    private VillageId currentVillageId;
    private int tickCount = 0;

    public void activateDefenders(Village village, ServerLevel level, Set<UUID> raidMobUUIDs) {
        this.currentRaidMobUUIDs = raidMobUUIDs;
        this.currentVillageId = village.getId();

        AABB area = AABB.ofSize(Vec3.atCenterOf(village.getCenter()),
                SEARCH_RADIUS * 2, SEARCH_RADIUS * 2, SEARCH_RADIUS * 2);

        List<MillVillager> allVillagers = level.getEntitiesOfClass(MillVillager.class, area,
                v -> village.getId().equals(v.getVillageId()));

        int totalFound = allVillagers.size();
        int warriorsFound = 0;

        for (MillVillager villager : allVillagers) {
            if (!isWarrior(villager)) continue;
            warriorsFound++;
            try {
                RaidDefenseGoal goal = new RaidDefenseGoal(village.getId(), raidMobUUIDs);
                injectGoal(villager, goal);
                injectedGoals.put(villager.getUUID(), goal);

                GuardEquipmentManager.equipBestAvailable(villager);
                GoalContext ctx = villager.buildGoalContext();
                if (ctx != null) {
                    villager.getGoalScheduler().forceTask(new VillageDefenseTask(raidMobUUIDs), ctx);
                    MillToolsMod.LOGGER.info("[MillTools] Défenseur activé : type={}, uuid={}",
                            villager.getVillagerTypeId(), villager.getUUID());
                } else {
                    MillToolsMod.LOGGER.warn("[MillTools] buildGoalContext null pour {} — skip forceTask",
                            villager.getUUID());
                }
            } catch (Exception e) {
                MillToolsMod.LOGGER.warn("[MillTools] Erreur activation défenseur: {}", e.getMessage());
            }
        }

        MillToolsMod.LOGGER.info("[MillTools] Défense '{}' : {}/{} villageois 'helpInAttacks', {} actifs",
                village.getVillageName(), warriorsFound, totalFound, injectedGoals.size());
    }

    public void tick(ServerLevel level) {
        if (currentRaidMobUUIDs == null) return;
        if (++tickCount % 20 != 0) return;

        for (Map.Entry<UUID, RaidDefenseGoal> entry : injectedGoals.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof MillVillager villager) || !villager.isAlive()) continue;

            // Force réveil au cas où — RestTask aurait pu redémarrer
            if (villager.isSleeping())         villager.stopSleeping();
            if (villager.isVillagerSleeping()) villager.setVillagerSleeping(false);

            GoalScheduler scheduler = villager.getGoalScheduler();
            if (scheduler == null) continue;

            // Rafraîchir l'équipement toutes les 200 ticks (~10 s)
            if (tickCount % 200 == 0) {
                GuardEquipmentManager.equipBestAvailable(villager);
            }

            // Si la tâche en cours n'est plus VillageDefenseTask, ré-installer
            if (!(scheduler.getCurrentTask() instanceof VillageDefenseTask)) {
                GoalContext ctx = villager.buildGoalContext();
                if (ctx != null) {
                    scheduler.forceTask(new VillageDefenseTask(currentRaidMobUUIDs), ctx);
                    MillToolsMod.LOGGER.debug("[MillTools] Re-force VillageDefenseTask pour {}",
                            villager.getUUID());
                }
            }
        }
    }

    public void deactivateDefenders(ServerLevel level) {
        for (Map.Entry<UUID, RaidDefenseGoal> entry : injectedGoals.entrySet()) {
            entry.getValue().deactivate();
            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof MillVillager villager && villager.isAlive()) {
                removeGoal(villager, entry.getValue());
                try {
                    GoalContext ctx = villager.buildGoalContext();
                    if (ctx != null) villager.getGoalScheduler().forceStop(ctx);
                } catch (Exception e) {
                    MillToolsMod.LOGGER.warn("[MillTools] Erreur désactivation: {}", e.getMessage());
                }
            }
        }
        injectedGoals.clear();
        currentRaidMobUUIDs = null;
        currentVillageId = null;
    }

    @SuppressWarnings("unchecked")
    private void injectGoal(MillVillager villager, RaidDefenseGoal goal) {
        if (goalsField == null) return;
        try {
            GoalScheduler scheduler = villager.getGoalScheduler();
            if (scheduler == null) return;
            List<VillagerGoal> goals = (List<VillagerGoal>) goalsField.get(scheduler);
            int before = goals.size();
            goals.add(goal);
            MillToolsMod.LOGGER.debug("[MillTools] Goal injecté ({}→{}) pour {}",
                    before, goals.size(), villager.getUUID());
        } catch (Exception e) {
            MillToolsMod.LOGGER.warn("[MillTools] Injection goal échouée: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void removeGoal(MillVillager villager, RaidDefenseGoal goal) {
        if (goalsField == null) return;
        try {
            GoalScheduler scheduler = villager.getGoalScheduler();
            if (scheduler == null) return;
            List<VillagerGoal> goals = (List<VillagerGoal>) goalsField.get(scheduler);
            goals.remove(goal);
        } catch (Exception e) {
            MillToolsMod.LOGGER.warn("[MillTools] Retrait goal échoué: {}", e.getMessage());
        }
    }

    private boolean isWarrior(MillVillager villager) {
        if (villager.isChild()) return false;
        ResourceLocation typeId = villager.getVillagerTypeId();
        if (typeId == null) return false;
        VillagerType type = ModCultures.getVillagerType(typeId);
        return type != null && type.hasTag("helpInAttacks");
    }
}
