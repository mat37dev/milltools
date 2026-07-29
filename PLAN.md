# Plan — MillTools (add-on Millenaire)

> Renommé depuis « Millenaire Raids » : le mod est désormais une boîte à outils générique pour
> Millenaire (raids nocturnes + correctifs/QoL village contrôlé), le système de raids ci-dessous
> n'étant qu'une des fonctionnalités qu'elle regroupe, toutes activables/désactivables.

## État d'avancement

### ✅ Étape 1 — Environnement de développement (TERMINÉ)
- `build.gradle` configuré (NeoForge 21.1.227 + Millenaire `compileOnly`)
- `gradle.properties` (mod_id=`milltools`, version=0.2.0)
- `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/wrapper/` en place
- `neoforge.mods.toml` (dépendance `millenaire` required, chargé AFTER)
- `MillToolsMod.java` — point d'entrée minimal
- `localRuntime "net.neoforged:testframework:${neo_version}"` — fix crash GameTestPlayer (bug NeoForge 21.1.227)

### ✅ Étape 2 — API Millenaire (TERMINÉ)
Analyse du JAR par `javap` — API publiques confirmées :

| Classe | Méthode | Retour |
|---|---|---|
| `VillageSavedData` | `get(ServerLevel)` | `VillageSavedData` |
| `VillageSavedData` | `getVillageManager()` | `VillageManager` |
| `VillageManager` | `getAllVillages()` | `Collection<Village>` |
| `VillageManager` | `findNearestVillage(BlockPos, double)` | `Village` |
| `Village` | `getCenter()` | `BlockPos` |
| `Village` | `getVillageName()` | `String` |
| `Village` | `getId()` | `VillageId` |
| `Village` | `getCultureId()` | `ResourceLocation` |
| `Village` | `isActive()` | `boolean` |
| `Village` | `isLoneBuilding()` | `boolean` |
| `VillageId` | `uuid()` | `UUID` (Record) |

---

## Architecture actuelle du mod (✅ = implémenté)

```
src/main/java/com/millenaire/raids/
├── MillToolsMod.java          ✅ Point d'entrée, wiring config + events + mixins + reflection GoalScheduler
├── RaidPhase.java                   ✅ Enum : PHASE_1 | PHASE_2
├── command/
│   ├── RaidCommand.java             ✅ /milltools enable|disable|status|reload (OP 2, i18n)
│   └── RaidState.java               ✅ SavedData persistant (état enabled)
├── config/
│   ├── RaidConfig.java              ✅ POJO de configuration (defaults inclus)
│   └── RaidConfigLoader.java        ✅ Génère et parse config/milltools/milltools.cfg
├── condition/
│   └── RestoreRecipesCondition.java ✅ Condition data-driven "milltools:recipes_enabled" ([general] restore_millenaire_recipes), enregistrée dans NeoForgeRegistries.Keys.CONDITION_CODECS
├── raid/
│   ├── RaidManager.java             ✅ Map<VillageId, RaidInstance>, sélection de vague, tick, annulation distance
│   ├── RaidInstance.java            ✅ STARTING→ACTIVE→WON/CANCELLED, spawn périmètre, goals IA, bossbar, défenseurs, messages i18n
│   ├── mob/
│   │   └── MobRegistry.java         ✅ Mapping String → mob factory
│   ├── wave/
│   │   ├── WaveDefinition.java      ✅ Interface modulaire
│   │   ├── RaidMobEntry.java        ✅ Record (mobType, count)
│   │   ├── PredefinedWave.java      ✅ Vague fixe depuis milltools.cfg
│   │   └── RandomPointWave.java     ✅ Vague aléatoire par budget de points
│   ├── enemy/
│   │   ├── EnemyRegistry.java       ✅ Mobs custom (ex: Slime) traités comme ennemis de village (milltools.cfg [village_enemies])
│   │   └── EnemySpawnGuard.java     ✅ Empêche ces mobs custom de spawn naturellement trop près d'un bâtiment
│   └── defense/
│       ├── VillageDefenseTask.java     ✅ VillagerTask : cible et attaque le mob de raid le plus proche
│       ├── RaidDefenseGoal.java        ✅ VillagerGoal (priorité 2000) injecté par réflexion dans GoalScheduler
│       ├── DefenderManager.java        ✅ Sélection (tag culture "helpInAttacks", non-enfant) + activation/désactivation/refresh
│       └── GuardEquipmentManager.java  ✅ Équipe l'arme du garde (mainhand) ; l'armure s'affiche via MillVillagerArmorSyncMixin (section 12)
└── event/
    ├── RaidEventHandler.java        ✅ Tick nocturne + détection joueur en village
    └── DevConvenienceHandler.java   ✅ [dev] auto_prestige + unlock_crop_knowledge : prestige max/contrôle culture/déblocage plantation au login (outil de test, pas pour la prod). Toujours enregistré sur l'event bus (relit RaidConfig.INSTANCE à chaque appel) ; applyToOnlinePlayers() permet à /milltools reload de réappliquer l'effet aux joueurs déjà connectés

src/main/java/com/millenaire/raids/mixin/           ⚠️ Modifient le comportement NATIF de Millénaire (voir section 12)
├── HuntMonsterGoalMixin.java         ✅ Étend HuntMonsterGoal aux mobs de [village_enemies] (ex: Slime)
├── EngageTargetGoalMixin.java        ✅ Autorise EngageTargetGoal à engager ces mêmes cibles étendues
├── VillageIntegrityCheckerMixin.java ✅ Bloque tout respawn de villageois d'un village tant que son raid Millénaire natif (village.isUnderAttack()) n'est pas terminé
├── MillVillagerArmorSyncMixin.java   ✅ Corrige l'armure des villageois jamais affichée côté client (bug natif Millénaire)
└── GrapeVineHarvestFixMixin.java     ✅ Corrige le rendement incohérent de la vigne (crop_vine) selon la moitié cassée en premier — voir section 12

src/main/resources/
├── milltools.mixins.json      ✅ Déclare les mixins ci-dessus (remap=false, JAVA_21)
├── data/millenaire/loot_table/blocks/crop_vine.json  ✅ Override de la loot table native (condition sans "half") — 1ère utilisation d'un override de datapack dans ce projet, voir section 12
├── data/milltools/recipe/*.json               ✅ 8 recettes de craft joueur restaurées (cidre, vin, huile d'olive, curry x2, masa, wah, coton→laine), chacune conditionnée par "milltools:recipes_enabled"
├── data/milltools/advancement/recipes/*.json  ✅ 6 advancements cachées (pas de "display") qui débloquent ces recettes dans le recipe book à l'obtention de l'ingrédient correspondant, même condition
└── assets/milltools/lang/
    ├── en_us.json                    ✅ Traductions anglaises
    └── fr_fr.json                    ✅ Traductions françaises
```

---

## Spécifications détaillées

### 1. Commande de contrôle
```
/milltools enable   → active les raids (sauvegardé)
/milltools disable  → désactive les raids (sauvegardé)
/milltools status   → affiche l'état + raids en cours
/milltools reload   → recharge milltools.cfg depuis le disque et réapplique à chaud tous les
                             effets qui en dépendent, y compris [dev] (auto_prestige,
                             unlock_crop_knowledge) pour les joueurs déjà connectés — aucun
                             redémarrage nécessaire
```
- Nécessite OP (niveau 2)
- État sauvegardé dans un `SavedData` pour persister entre les sessions
- Messages traduits via `Component.translatable()` (EN/FR)

### 2. Déclenchement — Ciblage du village

Un raid ne se déclenche que pour **le village où se trouve le joueur** :
- Utiliser `VillageManager.findNearestVillage(player.blockPosition(), rayon)` pour chaque joueur en ligne
- Si le joueur est dans le rayon du village (configurable, défaut : 80 blocs du centre)
- Un seul raid actif par village à la fois
- Si plusieurs joueurs sont dans le même village → un seul raid, tous reçoivent la bossbar

### 3. Phases nocturnes

La nuit Minecraft = ticks 13000 → 23000 (sur 24000).

| Phase | Déclenchement (tick) | Description |
|---|---|---|
| Phase 1 | 13000 (configurable) | Début de nuit — vagues légères |
| Phase 2 | 18000 (configurable) | Milieu/fin de nuit — vagues plus lourdes |

- Les deux valeurs de déclenchement sont dans `milltools.cfg`
- Chaque phase peut avoir ses propres templates de vague et multiplicateurs de difficulté

### 4. Fichier de configuration (`milltools.cfg`)

Créé automatiquement par le mod au premier lancement dans `config/milltools/milltools.cfg`.
Format : fichier texte structuré (style `.cfg` ou `.toml` simple, lisible sans IDE).

```toml
# === MillTools - Configuration ===

[dev]
# Active le mode développeur : octroie automatiquement prestige max + contrôle culture au login
auto_prestige = false
# Culture visée par auto_prestige (ex: norman, japanese, byzantine)
prestige_culture = norman
# Débloque au login la connaissance de plantation de toutes les cultures récoltables, sans achat
unlock_crop_knowledge = false

[general]
enabled = true
village_radius = 80          # Rayon (blocs) autour du centre pour considérer un joueur "dans le village"
phase1_tick = 13000          # Tick de déclenchement de la phase 1
phase2_tick = 18000          # Tick de déclenchement de la phase 2

[village_enemies]
# Mobs supplémentaires toujours traités comme ennemis par TOUS les villages (chassés par les
# villageois "helpInAttacks", comme les monstres vanilla). Utile pour les mobs que Millenaire ne
# reconnaît pas nativement comme hostiles (ex: le Slime, qui n'est pas un Monster vanilla).
# ID court vanilla en majuscules (SLIME) ou namespace:path pour un mob d'un autre mod.
# Le Creeper est volontairement ignoré même si présent dans la liste (explosions près des bâtiments).
enemies = SLIME
building_buffer = 5           # Distance min. (blocs, depuis les côtés du bâtiment) pour le spawn naturel de ces mobs

[difficulty]
# Points de départ d'un raid aléatoire selon la phase
phase1_points = 20
phase2_points = 40
# Bonus de points par jour mondial (scaling progressif)
points_per_day = 0.5

[mob_costs]
# Format : NOM_MOB = points
ZOMBIE = 3
SKELETON = 4
SPIDER = 3
PILLAGER = 6
STRAY = 5
HUSK = 3

[mob_weights]
# Poids de sélection aléatoire (plus le chiffre est élevé, plus le mob apparaît souvent)
ZOMBIE = 60
SKELETON = 25
SPIDER = 20
PILLAGER = 10
STRAY = 5
HUSK = 10

# Raids prédéfinis — format : nom | phase | mob:quantité mob:quantité ...
# predefined_raid = raid_starter  | PHASE_1 | ZOMBIE:3
# predefined_raid = raid_medium   | PHASE_1 | ZOMBIE:3 SKELETON:2
# predefined_raid = raid_heavy    | PHASE_2 | ZOMBIE:4 SKELETON:3 SPIDER:2
# predefined_raid = raid_elite    | PHASE_2 | ZOMBIE:5 SKELETON:3 PILLAGER:2
```

### 5. Types de raids

#### Raids prédéfinis
- Liste fixe de mobs, définie dans `milltools.cfg`
- Associés à une phase (PHASE_1 ou PHASE_2)
- Sélectionnés aléatoirement parmi ceux compatibles avec la phase courante

#### Raids aléatoires (système de points)
- Un budget de points est calculé : `base_points + (worldDay * points_per_day)`
- On tire des mobs aléatoirement (selon leur poids) jusqu'à épuiser le budget
- Chaque mob a un coût en points (`mob_costs` dans `milltools.cfg`)
- Permet une variété infinie sans intervention manuelle

#### Sélection au déclenchement
- Tirage aléatoire entre : raid prédéfini ou raid aléatoire (ratio configurable, défaut 50/50)

### 6. Comportement des mobs de raid

- **Ennemis du village** : les mobs ciblent automatiquement les villageois Millenaire (`MillVillager`)
  - Via `NearestAttackableTargetGoal<MillVillager>` ajouté à leur AI au spawn
- **Spawn à l'extérieur du village** : rayon `[villageRadius, villageRadius+20]` blocs depuis le centre (défaut : 80-100 blocs), en surface (`MOTION_BLOCKING_NO_LEAVES`)
- **Portes** : ⬜ à implémenter — `BreakDoorGoal` sur les mobs de raid pour qu'ils enfoncent les portes des maisons (les villageois se réfugient à l'intérieur la nuit)
- **Pas de creepers** par défaut dans `milltools.cfg`

### 7. Indicateurs visuels

#### Marqueur au-dessus des mobs de raid
- ⬜ Utiliser un `ArmorStand` invisible ou un `TextDisplay` (1.21.1 supporte les Display Entities)
- Affiche une icône ou texte `⚔ Raid` au-dessus de chaque mob de raid
- L'entité d'affichage est liée au mob (même UUID de tracking)

#### Bossbar
- ⬜ Une `BossEvent` (bossbar) par raid actif
- Titre : `⚔ Raid sur [Nom du village]`
- Progression : `mobs_restants / mobs_totaux`
- Couleur : ROUGE (Phase 1) → VIOLET (Phase 2)
- Visible uniquement pour les joueurs proches du village (rayon `village_radius`)

### 8. Gestion de la distance joueur ✅

Si aucun joueur n'est dans un rayon de `4 × villageRadius` (320 blocs) du centre :
- Le raid est annulé proprement : despawn des mobs de raid (`entity.discard()`)
- Vérification à chaque tick dans `RaidManager.tick()`

### 9. Conditions de fin de raid

| Condition | Résultat |
|---|---|
| Tous les mobs tués | **Victoire** — message traduit aux joueurs proches (≤128 blocs) |
| Aucun joueur dans 4×rayon | **Annulé** — nettoyage silencieux |
| Aube (tick 23000+) | **Expiré** ⬜ — les mobs restants despawnent |

### 10. Système i18n ✅

Messages joueur traduits côté client via `Component.translatable()`. Résolution automatique selon la langue Minecraft.

| Clé | Français | Anglais |
|-----|----------|---------|
| `milltools.raid.attack` | `§c[Raid] Le village %s est attaqué !` | `§c[Raid] Village %s is under attack!` |
| `milltools.raid.victory` | `§a[Raid] Le village %s a repoussé l'attaque !` | `§a[Raid] Village %s repelled the attack!` |
| `milltools.command.enabled` | `[MillTools] Raids activés.` | `[MillTools] Raids enabled.` |
| `milltools.command.disabled` | `[MillTools] Raids désactivés.` | `[MillTools] Raids disabled.` |
| `milltools.command.status` | `[MillTools] Raids: %s \| Raids actifs: %s` | `[MillTools] Raids: %s \| Active raids: %s` |
| `milltools.status.active` | `ACTIF` | `ACTIVE` |
| `milltools.status.inactive` | `INACTIF` | `INACTIVE` |

### 11. Défense active des villageois ✅ (TERMINÉ — Tranche 4)

#### Comportement au démarrage d'un raid (`RaidInstance.tick()` état STARTING → `DefenderManager.activateDefenders()`)

1. Trouver tous les `MillVillager` du village dans un rayon de 120 blocs (`level.getEntitiesOfClass(MillVillager.class, aabb, v -> village.getId().equals(v.getVillageId()))`)
2. Filtrer les **combattants** (`DefenderManager.isWarrior`) :
   - Non-enfant (`villager.isChild()`)
   - Tag culture **`helpInAttacks`** sur son `VillagerType` (`ModCultures.getVillagerType(typeId).hasTag("helpInAttacks")`) — remplace l'approche par nom de rôle initialement envisagée, plus fiable et déjà utilisée nativement par Millénaire pour ce même usage
3. Pour chaque combattant : injection par réflexion d'un `RaidDefenseGoal` (priorité 2000) dans la liste privée `GoalScheduler.goals`, équipement (`GuardEquipmentManager`), puis `goalScheduler.forceTask(new VillageDefenseTask(...), context)` — réveille les dormeurs et interrompt le sommeil

#### Comportement pendant le raid (`VillageDefenseTask.tick`, `DefenderManager.tick` toutes les 20 ticks)

- Le défenseur cible en continu le mob de raid vivant **le plus proche** (`raidMobUUIDs`), s'en approche via `getNavigation().moveTo()`, et attaque au contact (`doHurtTarget`, cooldown 20 ticks)
- Toutes les ~10s (200 ticks) : re-réveil forcé si rendormi, ré-injection de `VillageDefenseTask` si une autre tâche a repris la main, ré-appel `GuardEquipmentManager.equipBestAvailable()`
- `VillageDefenseTask.isFinished()` retourne `true` dès que `raidMobUUIDs` est vide (tous les mobs du raid morts)

#### Fin du raid (`DefenderManager.deactivateDefenders()`, appelé aussi sur `cancel()`/`expire()`)

- `goalScheduler.forceStop(context)` sur chaque défenseur encore vivant
- Retrait du `RaidDefenseGoal` injecté (réflexion, même champ `goals`)
- Le `GoalScheduler` reprend naturellement son cycle (sommeil, travail, etc.)

#### API Millenaire utilisée

| Classe | Utilité |
|--------|---------|
| `VillagerType.hasTag("helpInAttacks")` | Filtrage des combattants (remplace l'idée initiale par rôle) |
| `GoalScheduler.forceTask(VillagerTask, GoalContext)` / `forceStop(GoalContext)` | **public** — impose/arrête une tâche, interrompt le sommeil |
| `MillVillager.buildGoalContext()` | **public** — construit le `GoalContext` courant du villageois |
| `MillVillager.ensureCombatWeaponEquipped()` | **public** — sélection native de la meilleure arme possédée (jusqu'au diamant) |

**Point fragile** : `GoalScheduler.goals` est un champ **privé** dans `GoalScheduler` → accès par reflection une seule fois à l'init du mod (`MillToolsMod.initDefenderReflection()`, `Field.setAccessible(true)`, stocké dans `DefenderManager.goalsField`). Si Millenaire change la structure interne, il faudra ré-analyser le JAR.

---

### 12. Mixins — modifications du comportement natif de Millénaire ⚠️

Ces mixins (`milltools.mixins.json`, `remap=false` car le JAR Millénaire n'est pas obfusqué) ne concernent **pas** notre système de raids de monstres, mais corrigent/étendent des mécaniques **propres à Millénaire** :

| Mixin | Cible | Effet |
|---|---|---|
| `HuntMonsterGoalMixin` | `HuntMonsterGoal.isHuntableEntity` (static) | Les villageois "helpInAttacks" traquent aussi les mobs listés dans `[village_enemies]` (ex: Slime), pas seulement les `Monster` vanilla que Millénaire reconnaît nativement |
| `EngageTargetGoalMixin` | `EngageTargetGoal.resolveTarget` (static) | Sans ce mixin, un villageois ayant choisi un Slime comme cible via le mixin ci-dessus abandonnerait aussitôt le combat (`resolveTarget` rejette tout ce qui n'est ni `Monster` ni `MillVillager`) |
| `VillageIntegrityCheckerMixin` | `VillageIntegrityChecker.checkIntegrity` (static, `@Inject HEAD` cancellable) | **Corrige un déséquilibre des raids inter-villages natifs de Millénaire** (village vs village, pas nos raids de monstres) : `VillagerRecord.lastRespawnTick` vaut `0` par défaut et n'est mis à jour qu'au moment d'un respawn effectif ⇒ le tout premier mort d'un villageois est immédiatement éligible au respawn dès le check périodique suivant (jusqu'à 30s, cadence de `Village.integrityTickCounter`), au lieu du cooldown annoncé de 6000 ticks (5 min). En pratique les défenseurs revenaient presque aussitôt, empêchant `liveDefenders` de retomber à 0 et donc les attaquants de gagner (`RaidManager.shouldEndRaid`). Le mixin annule tout `checkIntegrity` pour un village tant que `village.isUnderAttack()` est vrai — effet de bord assumé : la récupération des villageois "manquants" (perdus/bloqués) est aussi mise en pause pendant la durée du raid |
| `MillVillagerArmorSyncMixin` | `MillVillager.getItemBySlot` (`@Inject HEAD` cancellable) | Corrige l'armure invisible sur tous les gardes (natif Millénaire, pas spécifique à nos raids) — voir détail juste en dessous |
| `GrapeVineHarvestFixMixin` | `BlockGrapeVine.onRemove` (`@Redirect` sur `Level.setBlock`) | **Corrige le rendement incohérent de la vigne (`crop_vine`)** : `onRemove` efface silencieusement la moitié jumelle (haut/bas) du plant via `setBlock(..., AIR, 3)` sans jamais passer par la loot table. Combiné à l'ancienne condition `age=7 ET half=bottom` de `crop_vine.json`, un pied mûr ne donnait 1 raisin que si on cassait le bas en premier ; casser le haut en premier détruisait tout le pied pour 0 raisin. Le mixin fait tomber un raisin pour la moitié effacée automatiquement quand elle était mûre. Combiné à l'override de `crop_vine.json` ci-dessous (condition réduite à `age=7`, sans `half`), un pied mûr donne maintenant 2 raisins au total, peu importe l'ordre de cueillette |

**Overrides de datapack** — en complément des mixins, `data/millenaire/loot_table/blocks/crop_vine.json` (dans `src/main/resources/`) remplace intégralement le fichier du même nom du mod Millénaire. Ça fonctionne car `neoforge.mods.toml` déclare `ordering="AFTER"` sur la dépendance `millenaire` : les ressources de milltools sont chargées après celles de Millénaire et gagnent en cas de chemin de ressource identique (remplacement complet du fichier, pas de fusion). Cette technique n'est à utiliser que pour du contenu purement data-driven (loot tables, recipes, tags...) — tout ce qui touche à du code reste un Mixin.

**Bug natif — armure jamais affichée (résolu)** : `MillVillager.getItemBySlot()` est overridé par Millénaire pour les 4 slots d'armure — il recalcule *dynamiquement* la meilleure pièce possédée depuis `tool_categories.json` + l'inventaire **virtuel** du villageois (`VillagerInventory`, un simple `Map<Item,Integer>`) à **chaque lecture** (rendu, calcul de protection), en ignorant totalement ce qui a pu être posé via `setItemSlot`. Diagnostiqué via deux commandes de debug temporaires (`debug_armor` côté serveur + un mixin de log côté client, retiré une fois le diagnostic terminé) : au même tick, pour la même entité, le serveur calculait correctement `getItemBySlot(CHEST) = iron_chestplate` alors que le client renvoyait systématiquement `air`. Cause : l'inventaire virtuel n'est **jamais synchronisé au client** ; la synchronisation vanilla de l'équipement (celle qui fait fonctionner l'affichage de l'arme en main) fonctionne bien et met à jour la vraie valeur d'équipement côté client, mais `getItemBySlot()` l'ignore et recalcule depuis l'inventaire vide à chaque lecture, y compris côté client. `MillVillagerArmorSyncMixin` fait retomber `getItemBySlot()` sur `super.getItemBySlot()` (la vraie valeur vanilla synchronisée) uniquement côté client pour les 4 slots d'armure — le serveur n'est pas touché. Netherite reste hors de portée (absent de `tool_categories.json`), inchangé.

---

## Architecture modulaire (pour extensions futures)

`WaveDefinition` est une interface :
```java
interface WaveDefinition {
    List<RaidMobEntry> resolveMobs(ServerLevel level, int worldDay);
    RaidPhase getPhase();
}
```
→ Permet d'ajouter ultérieurement : raids infinis, raids scriptés, raids d'invasion de culture, etc.

---

## Questions ouvertes (à décider plus tard)

1. **Villageois se défendent-ils ?** ✅ **Répondu** — Faisable via `GoalScheduler.forceTask()` (public) + reflection pour accéder au champ privé. Planifié en Tranche 4.
2. **Cultures spécifiques ?** Tables de mobs différentes selon la culture du village
3. **Persistance des raids ?** Sauvegarder en cas de redémarrage serveur mid-raid
4. **Advancements ?** Badge "Défenseur" pour les joueurs qui participent à un raid

---

## Ordre de développement

### ✅ Tranche 1 — Squelette fonctionnel (TERMINÉ)
- `RaidPhase.java` — enum PHASE_1 / PHASE_2
- `config/RaidConfig.java` — POJO + `defaults()`
- `config/RaidConfigLoader.java` — génère et parse `config/milltools/milltools.cfg`
- `command/RaidState.java` — SavedData persistant (état enabled/disabled)
- `command/RaidCommand.java` — `/milltools enable|disable|status` (OP 2)
- `raid/RaidManager.java` — squelette (log seulement, pas encore de RaidInstance)
- `event/RaidEventHandler.java` — tick nocturne, fenêtre 20 ticks, détection joueur en village via `findNearestVillage`
- `MillToolsMod.java` — wiring complet (config + commande + event handler)

**Résultat** : compile, génère `milltools.cfg`, commande fonctionnelle, log au déclenchement des phases.

### ✅ Tranche 2 — Cycle de vie du raid (TERMINÉ)
- `raid/mob/MobRegistry.java` — mapping String → mob factory (ZOMBIE, SKELETON, SPIDER, PILLAGER, STRAY, HUSK)
- `raid/wave/RaidMobEntry.java` — record (mobType, count)
- `raid/wave/WaveDefinition.java` — interface modulaire
- `raid/wave/PredefinedWave.java` — vague fixe depuis `milltools.cfg`
- `raid/wave/RandomPointWave.java` — vague aléatoire par budget de points (weighted random)
- `raid/RaidInstance.java` — cycle de vie STARTING→ACTIVE→WON/CANCELLED, spawn mobs, goals IA MillVillager
- `raid/RaidManager.java` — mise à jour : sélection de vague 50/50, `tick()` implémenté
- `event/RaidEventHandler.java` — `RaidManager.tick()` déplacé hors des blocs de phase (exécution permanente)
- `MillToolsMod.java` — ajout de `RaidManager.init(config)`

**Résultat** : compile, mobs spawned autour du village la nuit, ciblent les MillVillager, raid se termine quand tous sont tués.

### ✅ Tranche 2.5 — Améliorations qualité (TERMINÉ)
- **Spawn périmètre** : rayon `[villageRadius, villageRadius+20]` au lieu de `[15, 25]` — mobs apparaissent à l'extérieur du village (rayon Millenaire = 80 blocs, confirmé par bytecode JAR)
- **Annulation distance** : si aucun joueur dans `4 × villageRadius` (320 blocs) → `raid.cancel(level)` dans `RaidManager.tick()`
- **i18n EN/FR** : tous les messages joueur via `Component.translatable()`, fichiers `lang/en_us.json` et `lang/fr_fr.json` créés

### ✅ Tranche 3 — Feedback visuel et nettoyage (TERMINÉ)
- ✅ Bossbar (`ServerBossEvent`) : `⚔ Raid sur [Village]`, progression, couleur par phase (rouge/violet)
- ✅ Marqueurs ArmorStand invisible + nom `⚔ Raid` rouge au-dessus de chaque mob (TextDisplay privé en 1.21.1)
- ✅ `BreakDoorGoal` sur les mobs de raid (`PathfinderMob`, `diff != PEACEFUL`)
- ✅ Expiration à l'aube (tick 23000+, clé `gameDay*10+3`)
- ✅ `setPersistenceRequired()` — empêche le despawn vanilla des mobs de raid
- ✅ `restrictTo(center, 20)` + `MoveTowardsRestrictionGoal` — les mobs marchent vers le village
- ✅ Annulation à 2×villageRadius (au lieu de 4×) si aucun joueur à portée

### ✅ Tranche 4 — Défense active des villageois (TERMINÉ)
- `raid/defense/VillageDefenseTask.java` — `VillagerTask` : cible et attaque le mob de raid vivant le plus proche
- `raid/defense/RaidDefenseGoal.java` — `VillagerGoal` (priorité 2000), injecté par réflexion
- `raid/defense/DefenderManager.java` — sélection des combattants (tag culture `helpInAttacks`, non-enfant), activation/désactivation/refresh (toutes les 20 ticks, équipement toutes les ~10s)
- `raid/defense/GuardEquipmentManager.java` — équipement de l'arme (mainhand) au démarrage et en continu
- `raid/RaidInstance.java` — appel `DefenderManager.activateDefenders()` au passage STARTING→ACTIVE, `deactivateDefenders()` sur victoire/`cancel()`/`expire()`
- Reflection sur le champ **privé `GoalScheduler.goals`** (pas `MillVillager.goalScheduler`, qui est accessible via le getter public) — initialisée une fois dans `MillToolsMod.initDefenderReflection()`

**Résultat** : compile, les villageois taggés `helpInAttacks` se réveillent, s'équipent et combattent activement les mobs de raid ; retour à leur routine normale après le raid.

**Écart avec le plan initial** : le filtrage des combattants se fait par tag de culture `hasTag("helpInAttacks")`, pas par sexe (`ModelType.MALE`) ni par nom de rôle (`getRoleName()` contenant "warrior"/"archer"/etc.) — plus fiable et c'est le même tag que Millénaire utilise nativement pour ce rôle.

### ✅ Tranche 5 — Ennemis custom, correctifs natifs Millénaire, outils dev (TERMINÉ)
- `raid/enemy/EnemyRegistry.java` + `EnemySpawnGuard.java` — mobs configurables dans `[village_enemies]` (ex: Slime) traités comme ennemis de village par tous les villages, avec anti-spawn près des bâtiments (`building_buffer`)
- `mixin/HuntMonsterGoalMixin.java` + `mixin/EngageTargetGoalMixin.java` — étendent la traque/l'engagement des villageois "helpInAttacks" à ces mobs custom (Millénaire ne reconnaît nativement que les `Monster` vanilla)
- `mixin/VillageIntegrityCheckerMixin.java` — corrige un déséquilibre des **raids inter-villages natifs de Millénaire** (pas nos raids de monstres) : les défenseurs tués respawnaient en ~30s au lieu des 5 min annoncées (bug de `lastRespawnTick` initialisé à 0), empêchant les attaquants de jamais gagner. Voir section 12 pour le détail.
- `GuardEquipmentManager` — équipe l'arme (mainhand, avec ajout Netherite) ; ne gère pas l'armure (Millénaire s'en charge nativement via l'inventaire du villageois). L'armure ne s'affichait jamais en jeu (bug natif, pas spécifique aux gardes) — corrigé par `MillVillagerArmorSyncMixin`. Voir section 12.
- `event/DevConvenienceHandler.java` + `[dev]` dans `milltools.cfg` — auto-prestige/contrôle de culture au login, outil de confort pour les tests, désactivé par défaut
- `command/RaidCommand.java` — sous-commande `reload` (recharge `milltools.cfg` à chaud, sauf `[dev]`)

**Point d'attention pour le déploiement** : un changement de code (mixin, classes Java) nécessite un **redémarrage complet du jeu** — `/reload` ne recharge que les datapacks/resourcepacks, jamais le bytecode ni les mixins (tissés une seule fois au boot de la JVM). Et côté build : `./gradlew compileJava` seul ne régénère pas le jar dans `build/libs/` — il faut `./gradlew build` (ou `jar`) avant de redéployer dans `mods/`.

### ✅ Tranche 6 — Confort dev supplémentaire, reload à chaud généralisé, fix vigne (TERMINÉ)
- `config/RaidConfig.java` + `RaidConfigLoader.java` — nouveau flag `[dev] unlock_crop_knowledge` (défaut `false`) ; `RaidConfigLoader.load()` régénère désormais `milltools.cfg` au format canonique à chaque démarrage (valeurs conservées, clés/sections manquantes complétées automatiquement) au lieu de ne l'écrire qu'à la création initiale du fichier
- `event/DevConvenienceHandler.java` — refactor : toujours enregistré sur l'event bus (`MillToolsMod`), relit `RaidConfig.INSTANCE` à l'appel plutôt qu'au démarrage ; nouvelle méthode `applyToOnlinePlayers()` pour réappliquer l'effet aux joueurs déjà connectés
- `command/RaidCommand.java` — `reload` appelle désormais `DevConvenienceHandler.applyToOnlinePlayers()` : les flags `[dev]` prennent effet immédiatement, sans redémarrage
- `mixin/GrapeVineHarvestFixMixin.java` + override `data/millenaire/loot_table/blocks/crop_vine.json` — corrige le rendement incohérent de la vigne selon l'ordre de cueillette des deux moitiés (voir section 12)

**Retenu pour la suite** : toute nouvelle option de config doit suivre ce même pattern (event handler toujours enregistré + lecture live de `RaidConfig.INSTANCE` + réapplication active dans `reload()` si l'effet est ponctuel plutôt que vérifié en continu) — ne pas réintroduire de flag dont l'effet nécessite un redémarrage.

### ✅ Tranche 7 — Restauration de recettes de craft joueur disparues (TERMINÉ)
- `config/RaidConfig.java` + `RaidConfigLoader.java` — nouvelle section `[general] restore_millenaire_recipes` (défaut `true`) ; commentaires du fichier de config généré fortement raccourcis (1 ligne par option, sans justification)
- `condition/RestoreRecipesCondition.java` — condition `ICondition` custom (`record`, `MapCodec.unit`) lisant `RaidConfig.INSTANCE.generalRestoreMillenaireRecipes` au chargement des datapacks ; enregistrée via `DeferredRegister<MapCodec<? extends ICondition>>` sur `NeoForgeRegistries.Keys.CONDITION_CODECS` dans `MillToolsMod` (nom : `milltools:recipes_enabled`)
- `data/milltools/recipe/*.json` — 8 recettes `crafting_shaped` (format 1.21.1, `"result": {"id", "count"}`) retrouvées dans l'ancienne version 1.12.2 du mod (`OldSource/resources/assets/millenaire/recipes/` sur le dépôt GitHub `mat37dev/Millenaire-New-Age`) et disparues sans remplacement lors de la réécriture 9.0.0-beta : `cider`, `winebasic`, `oliveoil`, `chickencurry`, `vegcurry`, `masa`, `wah`, `cotton_to_wool`. Tous les ingrédients et résultats existent toujours dans `ModItems.java` de la beta2 — seule la recette manquait
- `data/milltools/advancement/recipes/*.json` — 6 advancements cachées (pas de bloc `display`, `parent: minecraft:recipes/root`) qui débloquent ces recettes dans le recipe book via `minecraft:inventory_changed` sur l'ingrédient de base (`cider_apple`, `grapes`, `olives`, `rice`+`turmeric`, `maize`, `cotton`) — comportement vanilla standard (recette invisible tant que l'ingrédient n'a jamais été obtenu). Chaque recette et chaque advancement portent la condition `milltools:recipes_enabled` pour rester cohérents avec le flag

**Point d'attention** : contrairement aux flags `[dev]`, ce toggle est lié au cycle de chargement des datapacks (recettes/advancements) et prend effet via un `/reload` **vanilla** ou un redémarrage — pas via `/milltools reload`, qui ne touche que `milltools.cfg` et les systèmes internes du mod.
