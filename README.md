# MillTools

Add-on **NeoForge** pour le mod [**Millenaire**](https://millenaire.org/) — Minecraft 1.21.1, testé
contre Millenaire `9.0.0-beta.2`.

MillTools n'est pas un mod à thème unique : c'est une **boîte à outils** de correctifs, d'extensions
et d'options de confort pour Millenaire. Chaque fonctionnalité est indépendante et
**activable/désactivable** via le fichier de configuration généré au premier lancement
(`config/milltools/milltools.cfg`).

Historique des versions : voir [CHANGELOG.md](CHANGELOG.md).

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
/milltools reload    — recharge milltools.cfg depuis le disque et applique les changements
                       à chaud (y compris les flags [dev], sans redémarrage)
```

### 🍎 Recettes de craft restaurées
Section `[general]`, `restore_millenaire_recipes` (activé par défaut) : restaure 8 recettes de
craft joueur présentes dans l'ancienne version du mod mais disparues lors de la réécriture
9.0.0-beta — cidre, vin, huile d'olive, curry de poulet, curry de légumes, masa, wah, coton →
laine. Recettes cachées par défaut dans le recipe book, débloquées à l'obtention de l'ingrédient
correspondant (comme du contenu Millenaire natif). Nécessite un `/reload` (ou redémarrage) après
modification de ce paramètre, comme toute recette Minecraft.

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
- Fix de la vigne (`crop_vine`) : un pied mûr ne donnait de raisin que si on cassait précisément le
  bloc du bas ; casser le haut en premier détruisait le pied sans rien donner. Un pied mûr donne
  maintenant 2 raisins au total, peu importe l'ordre de cueillette des deux moitiés.

### 🛠️ Outils de développement
Section `[dev]`, désactivée par défaut, utile pour tester rapidement sans passer par la
progression normale :
- `auto_prestige` : prestige max + contrôle de culture au login pour une culture donnée.
- `unlock_crop_knowledge` : débloque au login la connaissance de plantation de toutes les cultures
  récoltables (pommiers, vigne, riz, coton, etc.), sans avoir à l'acheter à un chef de village.

Les deux flags prennent effet immédiatement via `/milltools reload`, sans redémarrage.

## Configuration

Générée automatiquement dans `config/milltools/milltools.cfg` au premier lancement, puis
régénérée au format canonique à chaque démarrage (valeurs personnalisées conservées, sections/clés
manquantes ajoutées automatiquement). Sections : `[general]`, `[dev]`, `[village_enemies]` (actives
en permanence), puis `[raid]`, `[difficulty]`, `[mob_costs]`, `[mob_weights]` et les raids
prédéfinis (regroupés, spécifiques au système de raids). Le fichier est commenté ; modifiez-le puis
utilisez `/milltools reload` pour appliquer les changements à chaud (sauf `[general]`, lié aux
recettes : nécessite un `/reload` vanilla ou un redémarrage).

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
