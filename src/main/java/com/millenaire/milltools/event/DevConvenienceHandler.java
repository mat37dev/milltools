package com.millenaire.milltools.event;

import com.millenaire.milltools.MillToolsMod;
import com.millenaire.milltools.config.RaidConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.millenaire.village.PlayerCultureReputation;

public class DevConvenienceHandler {

    private static final int MAX_CULTURE_REPUTATION = 4096;

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        String cultureName = RaidConfig.INSTANCE.devPrestigeCulture;
        ResourceLocation cultureId = ResourceLocation.parse("millenaire:" + cultureName);
        PlayerCultureReputation pcr = PlayerCultureReputation.get(overworld);

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
}
