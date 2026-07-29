package com.millenaire.milltools.event;

import com.millenaire.milltools.MillToolsMod;
import com.millenaire.milltools.config.RaidConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.village.PlayerCultureReputation;

// Toujours enregistré sur l'event bus (voir MillToolsMod), que les flags [dev] soient actifs ou
// non : chaque méthode relit RaidConfig.INSTANCE au moment de l'appel, donc un /milltools reload
// suffit à changer de comportement sans redémarrer. applyToOnlinePlayers permet en plus au reload
// d'appliquer immédiatement l'effet aux joueurs déjà connectés, sans attendre leur prochaine connexion.
public class DevConvenienceHandler {

    private static final int MAX_CULTURE_REPUTATION = 4096;

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        apply(player);
    }

    public static void applyToOnlinePlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            apply(player);
        }
    }

    private static void apply(ServerPlayer player) {
        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        PlayerCultureReputation pcr = PlayerCultureReputation.get(overworld);

        if (RaidConfig.INSTANCE.devAutoPrestige) {
            String cultureName = RaidConfig.INSTANCE.devPrestigeCulture;
            ResourceLocation cultureId = ResourceLocation.parse("millenaire:" + cultureName);

            int current = pcr.get(player.getUUID(), cultureId);
            int delta = MAX_CULTURE_REPUTATION - current;
            if (delta > 0) {
                pcr.add(player.getUUID(), cultureId, delta);
                MillToolsMod.LOGGER.info("[MillTools] [dev] Prestige {} → {} pour {}",
                        current, MAX_CULTURE_REPUTATION, player.getName().getString());
            }

            if (!pcr.hasCultureControl(player.getUUID(), cultureId)) {
                pcr.grantCultureControl(player.getUUID(), cultureId);
                MillToolsMod.LOGGER.info("[MillTools] [dev] Contrôle culture '{}' accordé à {}",
                        cultureName, player.getName().getString());
            }
        }

        if (RaidConfig.INSTANCE.devUnlockCropKnowledge) {
            int learned = 0;
            for (Culture culture : ModCultures.getAllCultures().values()) {
                for (String cropKey : culture.knownCrops()) {
                    if (!pcr.hasLearnedCrop(player.getUUID(), cropKey)) {
                        pcr.learnCrop(player.getUUID(), cropKey);
                        learned++;
                    }
                }
            }
            if (learned > 0) {
                MillToolsMod.LOGGER.info("[MillTools] [dev] {} connaissance(s) de plantation débloquée(s) pour {}",
                        learned, player.getName().getString());
            }
        }
    }
}
