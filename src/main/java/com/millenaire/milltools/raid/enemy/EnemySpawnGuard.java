package com.millenaire.milltools.raid.enemy;

import com.millenaire.milltools.config.RaidConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.millenaire.building.BuildingInstance;
import org.millenaire.village.Village;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;

/**
 * Empêche les mobs de [village_enemies] (ex: Slime) de spawn naturellement à moins de
 * {@code building_buffer} blocs d'un bâtiment Millenaire, mesuré depuis les côtés de son
 * empreinte (BuildingInstance.getCachedMinX/MaxX/MinZ/MaxZ), pas depuis son origine/centre.
 */
public class EnemySpawnGuard {

    // Rayon de recherche du village le plus proche, généreux pour couvrir les plus grands villages.
    private static final double VILLAGE_SEARCH_RADIUS = 150.0;

    @SubscribeEvent
    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL) return;
        if (!EnemyRegistry.isConfiguredEnemy(event.getEntity())) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (isNearBuilding(level, pos, RaidConfig.INSTANCE.enemyBuildingBuffer)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    private boolean isNearBuilding(ServerLevel level, BlockPos pos, int buffer) {
        VillageManager manager = VillageSavedData.get(level).getVillageManager();
        Village village = manager.findNearestVillage(pos, VILLAGE_SEARCH_RADIUS);
        if (village == null) return false;

        int px = pos.getX();
        int pz = pos.getZ();

        for (BuildingInstance building : village.getBuildings()) {
            BlockPos origin = building.getOrigin();
            int minX = origin.getX() + building.getCachedMinX() - buffer;
            int maxX = origin.getX() + building.getCachedMaxX() + buffer;
            int minZ = origin.getZ() + building.getCachedMinZ() - buffer;
            int maxZ = origin.getZ() + building.getCachedMaxZ() + buffer;

            if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
                return true;
            }
        }

        return false;
    }
}
