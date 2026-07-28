package com.millenaire.milltools.mixin;

import net.minecraft.server.level.ServerLevel;
import org.millenaire.village.Village;
import org.millenaire.village.VillageIntegrityChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// VillageIntegrityChecker.checkIntegrity respawn les villageois tués dès le check périodique
// suivant (lastRespawnTick vaut 0 par défaut, donc le cooldown de 6000 ticks est déjà expiré dès
// la 1ère mort). Pendant un raid Millénaire (Village.isUnderAttack), ça permet aux défenseurs de
// revenir en ~30s, empêchant liveDefenders de retomber à 0 et donc les attaquants de gagner
// (RaidManager.shouldEndRaid). On bloque donc tout respawn de ce village tant que le raid dure.
@Mixin(value = VillageIntegrityChecker.class, remap = false)
public abstract class VillageIntegrityCheckerMixin {

    @Inject(method = "checkIntegrity", at = @At("HEAD"), cancellable = true, remap = false)
    private static void milltools$blockRespawnDuringRaid(ServerLevel level, Village village, CallbackInfo ci) {
        if (village.isUnderAttack()) {
            ci.cancel();
        }
    }
}
