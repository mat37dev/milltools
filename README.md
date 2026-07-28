# MillTools

Add-on **NeoForge** pour le mod [**Millenaire**](https://millenaire.org/) — Minecraft 1.21.1, testé
contre Millenaire `9.0.0-beta.2`.

MillTools n'est pas un mod à thème unique : c'est une **boîte à outils** de correctifs, d'extensions
et d'options de confort pour Millenaire. Chaque fonctionnalité est indépendante et
**activable/désactivable** via le fichier de configuration généré au premier lancement
(`config/milltools/milltools.cfg`).

## Fonctionnalités

### 🗡️ Raids nocturnes — *en développement, désactivé par défaut*
Système de raids de monstres sur les villages Millenaire : déclenchement nocturne en deux phases,
vagues prédéfinies ou générées aléatoirement (budget de points), spawn en périphérie du village,
défenseurs villageois actifs (les villageois `helpInAttacks` s'équipent et combattent), bossbar,
marqueurs visuels, expiration à l'aube.

> ⚠️ Fonctionnalité non finalisée : `enabled = false` par défaut dans la section `[raid]` de
> `milltools.cfg`. Il est déconseillé de l'activer en dehors d'un contexte de test/contribution.

Commande de contrôle (OP niveau 2) :
```
/milltools enable    — active le système de raids (persistant)
/milltools disable   — désactive le système de raids (persistant)
/milltools status    — affiche l'état et le nombre de raids en cours
/milltools reload     — recharge milltools.cfg depuis le disque sans redémarrer
```

### 👹 Ennemis de village personnalisables
Section `[village_enemies]` : déclare des mobs supplémentaires (ex. le Slime, qui n'est pas un
`Monster` vanilla) comme ennemis des villages Millenaire, chassés par les villageois
`helpInAttacks` au même titre que les monstres vanilla. Ces mobs sont aussi empêchés de spawn
naturellement trop près des bâtiments (`building_buffer`). Actif en permanence, indépendamment du
système de raids.

### 🔧 Correctifs du comportement natif de Millenaire
- Fix d'un bug de respawn trop rapide des défenseurs lors des raids **inter-villages natifs** de
  Millenaire (le cooldown de 5 minutes n'était pas respecté après la toute première mort).
- Fix de l'armure des villageois jamais affichée côté client (l'inventaire virtuel de Millenaire
  n'est jamais synchronisé au client).
- Fix des arbres (pommiers, oliviers, pistachiers) qui ne poussaient jamais lorsque plantés sous
  Y=1 (village en sous-sol/carrière) : le générateur d'arbre natif suppose un monde sans hauteur
  négative (héritage d'avant la 1.18) et rejetait la génération avant même de vérifier
  lumière/sol/espace.

### 🛠️ Outils de développement
Auto-prestige max + contrôle de culture au login (section `[dev]`, désactivé par défaut) — utile
pour tester rapidement sans passer par la progression normale.

## Configuration

Générée automatiquement dans `config/milltools/milltools.cfg` au premier lancement. Sections :
`[dev]`, `[village_enemies]` (actives en permanence), puis `[raid]`, `[difficulty]`,
`[mob_costs]`, `[mob_weights]` et les raids prédéfinis (regroupés, spécifiques au système de
raids). Le fichier est commenté ; modifiez-le puis redémarrez le serveur (ou utilisez
`/milltools reload` pour la partie raids).

## Build depuis les sources

Prérequis :
- JDK 21
- Une copie du jar **`millenaire-9.0.0-beta.2.jar`** à placer à la racine du projet (dépendance
  `compileOnly`, non fournie dans ce dépôt — voir `build.gradle`)

```
./gradlew build
```

Le jar buildé se trouve ensuite dans `build/libs/milltools-<version>.jar`.

## Crédits

MillTools est un add-on **non-officiel**, tiers, pour le mod **[Millenaire](https://millenaire.org/)**.
Tous les mérites du mod original (villages, cultures, IA des villageois, système de quêtes, etc.)
reviennent à son équipe de développement. Rendez-vous sur **https://millenaire.org/** pour le mod
de base, sa documentation et ses propres crédits.

## Licence

Voir `mod_license` dans `gradle.properties`.
