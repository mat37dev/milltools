package com.millenaire.milltools.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// FIX : org.millenaire.world.AbstractTreeGenerator (utilisé par AppleTreeSaplingBlock,
// OliveTreeGenerator, PistachioTreeGenerator...) code en dur l'hypothèse "le monde commence à
// Y=0" — héritage d'avant la 1.18 (hauteur de monde négative). Confirmé par désassemblage
// bytecode (javap) : `doGenerate` a un vrai ICONST_1 pour `position.getY() >= 1`, mais
// `checkSpace` compile `j < 0` en un opcode IFLT (comparaison implicite à zéro, aucune constante
// à patcher). Sur un monde moderne (minBuildHeight = -64), tout sapling planté sous Y=1 (ex:
// village en sous-sol/carrière) échoue à générer son arbre à 100%, indéfiniment, quels que
// soient lumière/sol/espace/bonemeal. On réimplémente les deux méthodes à l'identique, en
// remplaçant uniquement le plancher 0/1 codé en dur par level.getMinBuildHeight().
@Mixin(targets = "org.millenaire.world.AbstractTreeGenerator", remap = false)
public abstract class AbstractTreeGeneratorFixMixin {

    @Shadow
    protected abstract Block getLogBlock();

    @Shadow
    protected abstract BlockState getLeavesState();

    @Inject(method = "doGenerate", at = @At("HEAD"), cancellable = true, remap = false)
    private void milltools$fixNegativeYFloor(ServerLevel level, BlockPos position, RandomSource rand,
                                              CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(milltools$doGenerateFixed(level, position, rand));
        cir.cancel();
    }

    private boolean milltools$doGenerateFixed(ServerLevel level, BlockPos position, RandomSource rand) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int treeHeight = rand.nextInt(2) + 5;

        if (position.getY() < minY + 1 || position.getY() + treeHeight + 1 > maxY) {
            return false;
        }
        if (!milltools$checkSpaceFixed(level, position, treeHeight)) {
            return false;
        }

        BlockState groundState = level.getBlockState(position.below());
        if (!groundState.is(BlockTags.DIRT)) {
            return false;
        }
        if (position.getY() >= maxY - treeHeight - 1) {
            return false;
        }

        level.setBlock(position, Blocks.AIR.defaultBlockState(), 4);
        BlockState leavesState = this.getLeavesState();
        Block logBlock = this.getLogBlock();

        for (int yPos = 0; yPos < 5; yPos++) {
            BlockPos trunkPos = position.above(yPos);
            BlockState stateAt = level.getBlockState(trunkPos);
            if (stateAt.isAir() || stateAt.is(BlockTags.LEAVES) || stateAt.is(BlockTags.REPLACEABLE)) {
                level.setBlock(trunkPos, logBlock.defaultBlockState(), 3);
            }
        }

        for (Direction facing : Plane.HORIZONTAL) {
            int branchStartY = 3 + rand.nextInt(1);
            int horizontalSize = 3 - rand.nextInt(2);
            int xPos = position.getX();
            int zPos = position.getZ();
            int yPosx = position.getY() + branchStartY;
            int curve = rand.nextBoolean() ? 1 : -1;

            for (int hPos = 0; hPos < horizontalSize; hPos++) {
                if (yPosx < position.getY() + treeHeight && rand.nextFloat() < 0.7F) {
                    yPosx++;
                }

                if (facing.getStepX() != 0) {
                    xPos += facing.getStepX();
                    if (rand.nextFloat() < 0.15F) {
                        zPos += curve;
                    }
                } else {
                    zPos += facing.getStepZ();
                    if (rand.nextFloat() < 0.15F) {
                        xPos += curve;
                    }
                }

                BlockPos branchPos = new BlockPos(xPos, yPosx, zPos);
                BlockState branchState = level.getBlockState(branchPos);
                if (branchState.isAir() || branchState.is(BlockTags.LEAVES)) {
                    level.setBlock(branchPos, logBlock.defaultBlockState(), 3);

                    for (int dx = -1; dx < 2; dx++) {
                        for (int dz = -1; dz < 2; dz++) {
                            for (int dy = -1; dy < 2; dy++) {
                                BlockPos leafPos = branchPos.offset(dx, dy, dz);
                                BlockState leafState = level.getBlockState(leafPos);
                                if (leafState.isAir() && rand.nextInt(100) < 50) {
                                    level.setBlock(leafPos, leavesState, 3);
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    @Inject(method = "checkSpace", at = @At("HEAD"), cancellable = true, remap = false)
    private static void milltools$fixCheckSpaceFloor(ServerLevel level, BlockPos position, int treeHeight,
                                                       CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(milltools$checkSpaceFixed(level, position, treeHeight));
        cir.cancel();
    }

    private static boolean milltools$checkSpaceFixed(ServerLevel level, BlockPos position, int treeHeight) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        for (int j = position.getY(); j <= position.getY() + 1 + treeHeight; j++) {
            int k = 1;
            if (j == position.getY()) k = 0;
            if (j >= position.getY() + 1 + treeHeight - 2) k = 2;

            for (int l = position.getX() - k; l <= position.getX() + k; l++) {
                for (int i1 = position.getZ() - k; i1 <= position.getZ() + k; i1++) {
                    if (j < minY || j >= maxY) {
                        return false;
                    }

                    BlockPos checkPos = new BlockPos(l, j, i1);
                    if (!checkPos.equals(position)) {
                        BlockState stateAt = level.getBlockState(checkPos);
                        if (!stateAt.isAir() && !stateAt.is(BlockTags.LEAVES)
                                && !stateAt.is(BlockTags.LOGS) && !stateAt.is(BlockTags.REPLACEABLE)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
