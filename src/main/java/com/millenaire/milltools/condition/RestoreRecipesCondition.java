package com.millenaire.milltools.condition;

import com.millenaire.milltools.config.RaidConfig;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

// Condition data-driven ("neoforge:conditions") référencée par les recettes de
// data/milltools/recipe/ : évaluée au chargement des datapacks (démarrage, /reload), elle lit
// RaidConfig.INSTANCE.generalRestoreMillenaireRecipes pour n'inclure ces recettes que si
// [general] restore_millenaire_recipes vaut true.
public record RestoreRecipesCondition() implements ICondition {

    public static final MapCodec<RestoreRecipesCondition> CODEC = MapCodec.unit(RestoreRecipesCondition::new);

    @Override
    public boolean test(IContext context) {
        return RaidConfig.INSTANCE.generalRestoreMillenaireRecipes;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "milltools:recipes_enabled";
    }
}
