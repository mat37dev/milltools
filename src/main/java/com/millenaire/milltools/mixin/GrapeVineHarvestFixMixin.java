package com.millenaire.milltools.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.block.BlockGrapeVine;
import org.millenaire.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// FIX : BlockGrapeVine.onRemove efface silencieusement (level.setBlock(..., AIR, 3)) la moitié
// jumelle du plant (haut/bas) sans jamais passer par la loot table — combiné au fait que
// crop_vine.json n'accorde le raisin que pour age=7 ET half=bottom, ça donne un rendement
// incohérent selon la moitié cassée en premier : bas d'abord = 1 raisin, haut d'abord = 0 raisin,
// le pied entier étant perdu dans ce dernier cas. On complète ici : quand la moitié effacée
// automatiquement était mûre (age=7), on fait tomber un raisin pour elle aussi. Combiné à la
// correction de crop_vine.json (condition sans "half"), la moitié effectivement minée est déjà
// couverte par la loot table native : le pied mûr donne donc toujours 2 raisins au total,
// peu importe l'ordre de cueillette.
@Mixin(value = BlockGrapeVine.class, remap = false)
public abstract class GrapeVineHarvestFixMixin {

    @Redirect(
            method = "onRemove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            ),
            remap = false
    )
    private boolean milltools$dropGrapeFromClearedPartner(Level level, BlockPos pos, BlockState newState, int flags) {
        BlockState partner = level.getBlockState(pos);
        if (partner.getBlock() instanceof BlockGrapeVine && (Integer) partner.getValue(BlockGrapeVine.AGE) == 7) {
            Block.popResource(level, pos, new ItemStack(ModItems.GRAPES.get()));
        }
        return level.setBlock(pos, newState, flags);
    }
}
