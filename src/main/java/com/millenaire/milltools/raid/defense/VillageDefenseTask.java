package com.millenaire.milltools.raid.defense;

import com.millenaire.milltools.MillToolsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.VillagerTask;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public class VillageDefenseTask implements VillagerTask {

    private static final ResourceLocation GOAL_ID =
            ResourceLocation.fromNamespaceAndPath("milltools", "defend_village");

    private final Set<UUID> raidMobUUIDs;
    private int attackCooldown = 0;
    private int tickLogCounter = 0;

    public VillageDefenseTask(Set<UUID> raidMobUUIDs) {
        this.raidMobUUIDs = raidMobUUIDs;
    }

    @Override
    public void tick(GoalContext ctx) {
        MillVillager villager = ctx.villager();

        if (++tickLogCounter % 100 == 0) {
            MillToolsMod.LOGGER.debug("[MillTools] VillageDefenseTask tick {} pour {} (mobs restants={})",
                    tickLogCounter, villager.getUUID(), raidMobUUIDs.size());
        }

        if (attackCooldown > 0) { attackCooldown--; return; }

        Entity target = raidMobUUIDs.stream()
                .map(ctx.level()::getEntity)
                .filter(e -> e != null && e.isAlive())
                .min(Comparator.comparingDouble(villager::distanceTo))
                .orElse(null);

        if (target == null) return;

        if (villager.distanceTo(target) > 2.0) {
            villager.getNavigation().moveTo(target, 1.2);
        } else {
            villager.getNavigation().stop();
            villager.doHurtTarget(target);
            attackCooldown = 20;
        }
    }

    @Override public ResourceLocation goalId()  { return GOAL_ID; }
    @Override public boolean isFinished()        { return raidMobUUIDs.isEmpty(); }
    @Override public void stop(GoalContext ctx, StopReason reason) {
        ctx.villager().getNavigation().stop();
    }

    @Override
    public Component getGoalLabel() {
        return Component.literal("Défense du village");
    }
}
