# Changelog

Toutes les modifications notables de MillTools sont documentées dans ce fichier.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), et le versionning
suit [Semantic Versioning](https://semver.org/lang/fr/) (`MAJOR.MINOR.PATCH`) :
- **PATCH** : correctifs de bugs uniquement, aucun changement de comportement/config.
- **MINOR** : nouvelle fonctionnalité, rétrocompatible.
- **MAJOR** : changement cassant (format de config, save) ou première version stable.

## [0.2.0] - 2026-07-29

### Added
- `[dev] unlock_crop_knowledge` : débloque au login la connaissance de plantation de toutes les
  cultures récoltables Millenaire (pommiers, vigne, riz, coton, etc.), sans avoir à l'acheter à un
  chef de village. Désactivé par défaut, indépendant de `auto_prestige`.
- `[general] restore_millenaire_recipes` (activé par défaut) : restaure 8 recettes de craft
  joueur disparues lors de la réécriture 9.0.0-beta de Millenaire (cidre, vin, huile d'olive,
  curry de poulet, curry de légumes, masa, wah, coton → laine) — les objets existent toujours
  côté Millenaire mais n'étaient plus fabricables que par les villageois. Recettes cachées par
  défaut dans le recipe book, débloquées à l'obtention de l'ingrédient correspondant, comme du
  contenu Millenaire natif.

### Changed
- `/milltools reload` applique désormais immédiatement les flags `[dev]` (`auto_prestige`,
  `unlock_crop_knowledge`) aux joueurs déjà connectés, sans redémarrage nécessaire (auparavant
  seul un redémarrage complet du jeu les prenait en compte).
- `milltools.cfg` est maintenant régénéré au format canonique à chaque démarrage : les valeurs déjà
  personnalisées sont conservées, et toute section/clé manquante (ex. après une mise à jour du mod)
  est ajoutée automatiquement avec sa valeur par défaut, au lieu de rester silencieusement absente.

### Fixed
- Vigne (`crop_vine`) : un pied mûr (age=7) ne donnait de raisin que si on cassait précisément le
  bloc du **bas** ; casser le bloc du **haut** en premier détruisait le pied entier sans jamais
  donner de raisin (la loot table native exigeait `half=bottom`, et la moitié jumelle effacée
  automatiquement ne passait jamais par la loot table). Un pied mûr donne maintenant 2 raisins au
  total, peu importe l'ordre de cueillette des deux moitiés.

## [0.1.0] - 2026-07-28

Première version publique.

### Added
- Raids nocturnes de monstres sur les villages Millenaire (expérimental, désactivé par défaut) :
  déclenchement en deux phases, vagues prédéfinies ou aléatoires (budget de points), défenseurs
  villageois actifs, bossbar, marqueurs visuels, expiration à l'aube.
- Ennemis de village personnalisables (`[village_enemies]`) : mobs supplémentaires (ex. Slime)
  traités comme ennemis par les villageois `helpInAttacks`, avec buffer anti-spawn près des
  bâtiments.
- Commande `/milltools` (OP niveau 2) : `enable`, `disable`, `status`, `reload`.
- Outils de développement (`[dev]`, désactivé par défaut) : auto-prestige max + contrôle de
  culture au login.

### Fixed
- Respawn trop rapide des défenseurs villageois lors des raids **inter-villages natifs** de
  Millenaire (le cooldown de 5 minutes n'était pas respecté après la toute première mort).
- Armure des villageois jamais affichée côté client (inventaire virtuel de Millenaire jamais
  synchronisé au client).
- Arbres natifs (pommiers, oliviers, pistachiers) ne poussant jamais lorsque plantés sous Y=1
  (village en sous-sol/carrière) : le générateur d'arbre supposait un monde sans hauteur négative
  (héritage d'avant la 1.18) et rejetait la génération avant même de vérifier lumière/sol/espace.
