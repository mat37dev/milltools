package com.millenaire.milltools.raid.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Zombie;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class MobRegistry {

    private static final Map<String, Function<ServerLevel, Mob>> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put("ZOMBIE",   level -> new Zombie(EntityType.ZOMBIE, level));
        REGISTRY.put("SKELETON", level -> new Skeleton(EntityType.SKELETON, level));
        REGISTRY.put("SPIDER",   level -> new Spider(EntityType.SPIDER, level));
        REGISTRY.put("PILLAGER", level -> new Pillager(EntityType.PILLAGER, level));
        REGISTRY.put("STRAY",    level -> new Stray(EntityType.STRAY, level));
        REGISTRY.put("HUSK",     level -> new Husk(EntityType.HUSK, level));
    }

    private MobRegistry() {}

    @Nullable
    public static Mob create(String mobType, ServerLevel level) {
        Function<ServerLevel, Mob> factory = REGISTRY.get(mobType.toUpperCase());
        return factory != null ? factory.apply(level) : null;
    }

    public static boolean isKnown(String mobType) {
        return REGISTRY.containsKey(mobType.toUpperCase());
    }
}
