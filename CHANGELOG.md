# Changelog

Toutes les modifications notables de MillTools sont documentées dans ce fichier.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), et le versionning
suit [Semantic Versioning](https://semver.org/lang/fr/) (`MAJOR.MINOR.PATCH`) :
- **PATCH** : correctifs de bugs uniquement, aucun changement de comportement/config.
- **MINOR** : nouvelle fonctionnalité, rétrocompatible.
- **MAJOR** : changement cassant (format de config, save) ou première version stable.

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
