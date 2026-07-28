package com.millenaire.milltools.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.millenaire.entity.MillVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// MillVillager.getItemBySlot() recalcule les 4 slots d'armure depuis l'inventaire virtuel du
// villageois (VillagerInventory), qui n'est jamais synchronisé au client — confirmé en comparant
// les logs serveur (armure correcte) et client (toujours vide) pour la même entité au même tick.
// Le mécanisme vanilla de synchronisation d'équipement (celui qui fait fonctionner l'affichage de
// l'arme en main) fonctionne bien de son côté et met à jour la "vraie" valeur d'équipement côté
// client — mais getItemBySlot() l'ignore et recalcule depuis l'inventaire vide à chaque lecture.
// Fix : côté client uniquement, retomber sur la vraie valeur vanilla (super.getItemBySlot) au lieu
// de recalculer, pour que le rendu (HumanoidArmorLayer) affiche ce que le serveur a synchronisé.
@Mixin(value = MillVillager.class, remap = false)
public abstract class MillVillagerArmorSyncMixin extends PathfinderMob {

    protected MillVillagerArmorSyncMixin(EntityType<? extends MillVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void milltools$clientArmorFallback(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (!this.level().isClientSide()) return;
        if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
            cir.setReturnValue(super.getItemBySlot(slot));
        }
    }
}
