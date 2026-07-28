package com.millenaire.milltools.mixin;

import com.millenaire.milltools.raid.enemy.EnemyRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.impl.EngageTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// EngageTargetGoal.resolveTarget rejette (retourne null) tout attackTarget qui n'est ni un
// Monster ni un MillVillager. Sans ce mixin, un villageois qui vient de choisir un Slime comme
// cible via HuntMonsterGoal (voir HuntMonsterGoalMixin) abandonnerait le combat immédiatement.
@Mixin(value = EngageTargetGoal.class, remap = false)
public abstract class EngageTargetGoalMixin {

    @Inject(method = "resolveTarget", at = @At("RETURN"), cancellable = true, remap = false)
    private static void milltools$allowExtraEnemies(MillVillager v, CallbackInfoReturnable<LivingEntity> cir) {
        if (cir.getReturnValue() != null) return;
        LivingEntity t = v.getAttackTarget();
        if (t != null && t.isAlive() && EnemyRegistry.isConfiguredEnemy(t)) {
            cir.setReturnValue(t);
        }
    }
}
