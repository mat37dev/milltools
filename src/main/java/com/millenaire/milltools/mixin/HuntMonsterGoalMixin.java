package com.millenaire.milltools.mixin;

import com.millenaire.milltools.raid.enemy.EnemyRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.millenaire.goal.impl.HuntMonsterGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Étend HuntMonsterGoal.isHuntableEntity : les villageois 'helpInAttacks' scannent aussi les
// mobs configurés dans [village_enemies] (ex: Slime, qui n'est pas un Monster vanilla).
@Mixin(value = HuntMonsterGoal.class, remap = false)
public abstract class HuntMonsterGoalMixin {

    @Inject(method = "isHuntableEntity", at = @At("RETURN"), cancellable = true, remap = false)
    private static void milltools$extraEnemies(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && entity != null && entity.isAlive() && EnemyRegistry.isConfiguredEnemy(entity)) {
            cir.setReturnValue(true);
        }
    }
}
