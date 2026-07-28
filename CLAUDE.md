# MillTools (add-on Millenaire) - CLAUDE.md

## Contexte du projet

Add-on NeoForge **`milltools`** (package `com.millenaire.milltools`) pour le mod **Millenaire 9.0.0-beta.2** (Minecraft 1.21.1).
Boîte à outils générique : correctifs/QoL côté village contrôlé, plus le système de **raids nocturnes de monstres** (implémenté en premier, mais une fonctionnalité parmi d'autres désormais — toutes activables/désactivables indépendamment).
Renommé depuis « Millenaire Raids » une fois la portée du projet élargie au-delà des raids.

## Fichiers présents

- `millenaire-9.0.0-beta.2.jar` — Le mod Millenaire (dépendance compileOnly, voir build.gradle)
- `decompiled_millenaire_beta2/` — Source décompilée (Vineflower) du JAR ci-dessus, pour exploration rapide de l'API
- `saves_party/data/` — Données de save de test (NBT gzippé)
  - `millenaire_villages.dat` — Structure des villages (village_type, center, culture, buildings, etc.)
  - `millenaire_culture_reputation.dat` — Réputation par culture
  - `millenaire_quest_data.dat` — Données de quêtes

## Stack technique

- **Loader** : NeoForge 21.1+
- **Minecraft** : 1.21.1
- **Langage** : Java 21
- **Build** : Gradle (MDK NeoForge)

## Architecture Millenaire (API confirmée par javap, voir PLAN.md)

```
VillageSavedData.get(ServerLevel)
  └── .getVillageManager() → VillageManager
        ├── .getAllVillages() → Collection<Village>
        ├── .findNearestVillage(BlockPos, double) → Village
        └── .isWithinMinDistance(BlockPos, double) → boolean
              Village
              ├── .getCenter() → BlockPos
              ├── .getVillageName() → String
              ├── .getId() → VillageId
              ├── .getCultureId() → ResourceLocation
              ├── .isActive() / .isLoneBuilding() → boolean
              └── .getWaypointGraph() → VillageWaypointGraph
```

Classes importantes à connaitre :
- `org.millenaire.village.VillageSavedData`, `VillageManager`, `Village`, `VillageId`
- `org.millenaire.entity.MillVillager` (`getGoalScheduler()`, `getVillagerTypeId()`, `buildGoalContext()`)
- `org.millenaire.goal.GoalScheduler` / `GoalContext` / `VillagerGoal` / `VillagerTask` (système de tâches IA des villageois)
- `org.millenaire.culture.ModCultures` / `VillagerType` (tags de rôle, ex. `"helpInAttacks"`)
- `org.millenaire.item.SummoningWandItem`, `org.millenaire.network.VillageCreationRequestPayload` (création de village)

## Points d'attention

- `GoalScheduler` a des champs internes privés (ex. liste des goals) → accès par réflexion nécessaire, voir `DefenderManager`
- Millenaire a déjà `mp_raidonplayer` (villageois attaquent le joueur si mauvaise réputation) — notre add-on ajoute l'inverse (monstres attaquent le village)
- Pour explorer l'API rapidement : lire `decompiled_millenaire_beta2/` directement, ou `/jar-inspect <Classe>`
- Voir `PLAN.md` pour l'état d'avancement détaillé du mod
