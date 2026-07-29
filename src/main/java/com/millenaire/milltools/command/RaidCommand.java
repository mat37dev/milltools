package com.millenaire.milltools.command;

import com.millenaire.milltools.config.RaidConfig;
import com.millenaire.milltools.config.RaidConfigLoader;
import com.millenaire.milltools.event.DevConvenienceHandler;
import com.millenaire.milltools.raid.RaidManager;
import com.millenaire.milltools.raid.enemy.EnemyRegistry;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class RaidCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext context) {
        dispatcher.register(
                Commands.literal("milltools")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("enable")
                                .executes(RaidCommand::enable))
                        .then(Commands.literal("disable")
                                .executes(RaidCommand::disable))
                        .then(Commands.literal("status")
                                .executes(RaidCommand::status))
                        .then(Commands.literal("reload")
                                .executes(RaidCommand::reload))
        );
    }

    private static int enable(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        RaidState.get(level).setEnabled(true);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("milltools.command.enabled"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int disable(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        RaidState.get(level).setEnabled(false);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("milltools.command.disabled"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        boolean isEnabled = RaidState.get(level).isEnabled();
        int activeCount = RaidManager.getActiveRaidCount();
        Component statusKey = Component.translatable(
                isEnabled ? "milltools.status.active" : "milltools.status.inactive");
        ctx.getSource().sendSuccess(
                () -> Component.translatable("milltools.command.status", statusKey, activeCount), false);
        return Command.SINGLE_SUCCESS;
    }

    // Recharge milltools.cfg depuis le disque sans redémarrer le jeu, et réapplique immédiatement
    // les effets qui en dépendent (ex: [dev] auto_prestige / unlock_crop_knowledge pour les joueurs
    // déjà connectés, sans attendre leur prochaine connexion).
    private static int reload(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Path configDir = FMLPaths.GAMEDIR.get().resolve("config");
        RaidConfig config = RaidConfigLoader.load(configDir);
        RaidConfig.INSTANCE = config;
        RaidManager.init(config);
        EnemyRegistry.init(config.extraEnemyTypes);
        DevConvenienceHandler.applyToOnlinePlayers(ctx.getSource().getServer());

        ctx.getSource().sendSuccess(
                () -> Component.translatable("milltools.command.reloaded"), true);
        return Command.SINGLE_SUCCESS;
    }
}
