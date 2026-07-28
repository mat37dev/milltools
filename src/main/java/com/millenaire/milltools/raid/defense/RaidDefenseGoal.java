package com.millenaire.milltools.raid.defense;

import net.minecraft.resources.ResourceLocation;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.village.VillageId;

import java.util.Set;
import java.util.UUID;

public class RaidDefenseGoal implements VillagerGoal {

    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("milltools", "raid_defense");

    private final VillageId villageId;
    private final Set<UUID> raidMobUUIDs;
    private volatile boolean active = true;

    public RaidDefenseGoal(VillageId villageId, Set<UUID> raidMobUUIDs) {
        this.villageId = villageId;
        this.raidMobUUIDs = raidMobUUIDs;
    }

    public void deactivate() { this.active = false; }

    @Override public ResourceLocation id()               { return ID; }
    @Override public int computePriority(GoalContext ctx) { return 2000; }
    @Override public boolean isLeisure()                 { return false; }
    @Override public boolean canBeDoneAtNight()          { return true; }
    @Override public boolean canBeDoneInDayTime()        { return true; }
    @Override public long reoccurDelayTicks()            { return 0L; }

    @Override
    public boolean canStart(GoalContext ctx) {
        return active
                && !raidMobUUIDs.isEmpty()
                && villageId.equals(ctx.villager().getVillageId());
    }

    @Override
    public VillagerTask start(GoalContext ctx) {
        return new VillageDefenseTask(raidMobUUIDs);
    }
}
