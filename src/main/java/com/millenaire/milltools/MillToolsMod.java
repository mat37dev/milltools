package com.millenaire.milltools;

import com.millenaire.milltools.command.RaidCommand;
import com.millenaire.milltools.raid.RaidManager;
import com.millenaire.milltools.config.RaidConfig;
import com.millenaire.milltools.config.RaidConfigLoader;
import com.millenaire.milltools.event.DevConvenienceHandler;
import com.millenaire.milltools.event.RaidEventHandler;
import com.millenaire.milltools.raid.defense.DefenderManager;
import com.millenaire.milltools.raid.enemy.EnemyRegistry;
import com.millenaire.milltools.raid.enemy.EnemySpawnGuard;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.millenaire.goal.GoalScheduler;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.nio.file.Path;

@Mod(MillToolsMod.MOD_ID)
public class MillToolsMod {

    public static final String MOD_ID = "milltools";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MillToolsMod(IEventBus modEventBus, ModContainer modContainer) {
        Path configDir = FMLPaths.GAMEDIR.get().resolve("config");
        RaidConfig config = RaidConfigLoader.load(configDir);
        RaidConfig.INSTANCE = config;
        LOGGER.info("[MillTools] Mod initialisé.");

        EnemyRegistry.init(config.extraEnemyTypes);
        NeoForge.EVENT_BUS.register(new EnemySpawnGuard());

        RaidManager.init(config);
        NeoForge.EVENT_BUS.register(new RaidEventHandler());
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class,
                event -> RaidCommand.register(event.getDispatcher(), event.getBuildContext()));

        initDefenderReflection();

        if (config.devAutoPrestige) {
            NeoForge.EVENT_BUS.register(new DevConvenienceHandler());
            LOGGER.info("[MillTools] [dev] Mode auto-prestige activé pour la culture '{}'.", config.devPrestigeCulture);
        }
    }

    // GoalScheduler.goals est privé : DefenderManager y injecte/retire son goal de défense par
    // réflexion. À initialiser une seule fois ici (voir DefenderManager.injectGoal/removeGoal).
    private void initDefenderReflection() {
        try {
            Field goalsField = GoalScheduler.class.getDeclaredField("goals");
            goalsField.setAccessible(true);
            DefenderManager.setGoalsField(goalsField);
        } catch (NoSuchFieldException e) {
            LOGGER.error("[MillTools] Champ GoalScheduler.goals introuvable (API Millenaire modifiée ?) : {}", e.getMessage());
        }
    }
}
