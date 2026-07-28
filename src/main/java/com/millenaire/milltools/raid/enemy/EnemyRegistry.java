package com.millenaire.milltools.raid.enemy;

import com.millenaire.milltools.MillToolsMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Mobs configurés dans [village_enemies] (milltools.cfg) et traités comme ennemis par tous les
 * villages, en complément des Monster vanilla que Millenaire reconnaît déjà nativement
 * (voir HuntMonsterGoalMixin / EngageTargetGoalMixin).
 */
public final class EnemyRegistry {

    private static Set<EntityType<?>> extraEnemyTypes = Set.of();

    private EnemyRegistry() {}

    public static void init(Set<String> configuredIds) {
        Set<EntityType<?>> resolved = new HashSet<>();
        for (String rawId : configuredIds) {
            EntityType<?> type = resolve(rawId);
            if (type == null) {
                MillToolsMod.LOGGER.warn("[MillTools] [village_enemies] Mob inconnu ignoré : {}", rawId);
            } else if (type == EntityType.CREEPER) {
                MillToolsMod.LOGGER.warn(
                        "[MillTools] [village_enemies] Creeper ignoré volontairement (explosions près des bâtiments).");
            } else {
                resolved.add(type);
            }
        }
        extraEnemyTypes = resolved;
        MillToolsMod.LOGGER.info("[MillTools] Ennemis de village supplémentaires actifs : {}", resolved);
    }

    public static boolean isConfiguredEnemy(Entity entity) {
        return entity != null && extraEnemyTypes.contains(entity.getType());
    }

    private static EntityType<?> resolve(String rawId) {
        String id = rawId.strip().toLowerCase(Locale.ROOT);
        if (id.isEmpty()) return null;
        String path = id.contains(":") ? id : "minecraft:" + id;
        return EntityType.byString(path).orElse(null);
    }
}
