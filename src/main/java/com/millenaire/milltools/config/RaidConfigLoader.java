package com.millenaire.milltools.config;

import com.millenaire.milltools.MillToolsMod;
import com.millenaire.milltools.RaidPhase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RaidConfigLoader {

    public static RaidConfig load(Path configDir) {
        Path file = configDir.resolve("milltools/milltools.cfg");
        RaidConfig config;

        if (!Files.exists(file)) {
            config = RaidConfig.defaults();
        } else {
            try {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                config = parse(lines);
            } catch (IOException e) {
                MillToolsMod.LOGGER.error("[MillTools] Impossible de lire milltools.cfg : {}", e.getMessage());
                return RaidConfig.defaults();
            }
        }

        // Réécrit systématiquement le fichier au format canonique : les valeurs déjà présentes
        // sont conservées (fusionnées dans `config` par parse()), et toute section/clé manquante
        // (ex: ajoutée par une mise à jour du mod) est ajoutée avec sa valeur par défaut.
        // Les commentaires personnalisés éventuellement ajoutés à la main sont en revanche perdus.
        try {
            Files.createDirectories(file.getParent());
            writeDefaults(file, config);
        } catch (IOException e) {
            MillToolsMod.LOGGER.error("[MillTools] Impossible d'écrire milltools.cfg : {}", e.getMessage());
        }

        return config;
    }

    private static RaidConfig parse(List<String> lines) {
        RaidConfig config = RaidConfig.defaults();
        String section = "";

        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).strip();
                continue;
            }

            // Cas spécial : predefined_raid (pas dans une section)
            if (line.startsWith("predefined_raid")) {
                parsePredefinedRaid(config, line);
                continue;
            }

            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).strip();
            String value = line.substring(eq + 1).strip();

            try {
                switch (section) {
                    case "general" -> parseGeneral(config, key, value);
                    case "raid" -> parseRaid(config, key, value);
                    case "difficulty" -> parseDifficulty(config, key, value);
                    case "mob_costs" -> config.mobCosts.put(key.toUpperCase(), Integer.parseInt(value));
                    case "mob_weights" -> config.mobWeights.put(key.toUpperCase(), Integer.parseInt(value));
                    case "dev" -> parseDev(config, key, value);
                    case "village_enemies" -> parseVillageEnemies(config, key, value);
                }
            } catch (NumberFormatException e) {
                MillToolsMod.LOGGER.warn("[MillTools] Valeur invalide ignorée : {}={}", key, value);
            }
        }
        return config;
    }

    private static void parseGeneral(RaidConfig c, String key, String value) {
        switch (key) {
            case "restore_millenaire_recipes" -> c.generalRestoreMillenaireRecipes = Boolean.parseBoolean(value);
        }
    }

    private static void parseRaid(RaidConfig c, String key, String value) {
        switch (key) {
            case "enabled" -> c.enabled = Boolean.parseBoolean(value);
            case "village_radius" -> c.villageRadius = Double.parseDouble(value);
            case "phase1_tick" -> c.phase1Tick = Long.parseLong(value);
            case "phase2_tick" -> c.phase2Tick = Long.parseLong(value);
        }
    }

    private static void parseVillageEnemies(RaidConfig c, String key, String value) {
        switch (key) {
            // Format : enemies = SLIME, monmod:custom_mob  (remplace la liste par défaut)
            case "enemies" -> {
                c.extraEnemyTypes.clear();
                for (String token : value.split(",")) {
                    String id = token.strip();
                    if (!id.isEmpty()) c.extraEnemyTypes.add(id);
                }
            }
            case "building_buffer" -> c.enemyBuildingBuffer = Integer.parseInt(value);
        }
    }

    private static void parseDev(RaidConfig c, String key, String value) {
        switch (key) {
            case "auto_prestige" -> c.devAutoPrestige = Boolean.parseBoolean(value);
            case "prestige_culture" -> c.devPrestigeCulture = value;
            case "unlock_crop_knowledge" -> c.devUnlockCropKnowledge = Boolean.parseBoolean(value);
        }
    }

    private static void parseDifficulty(RaidConfig c, String key, String value) {
        switch (key) {
            case "phase1_points" -> c.phase1Points = Integer.parseInt(value);
            case "phase2_points" -> c.phase2Points = Integer.parseInt(value);
            case "points_per_day" -> c.pointsPerDay = Double.parseDouble(value);
        }
    }

    private static void parsePredefinedRaid(RaidConfig config, String line) {
        // Format : predefined_raid = nom | PHASE_X | MOB:n MOB:n
        int eq = line.indexOf('=');
        if (eq < 0) return;
        String[] parts = line.substring(eq + 1).split("\\|");
        if (parts.length < 3) {
            MillToolsMod.LOGGER.warn("[MillTools] Ligne predefined_raid invalide : {}", line);
            return;
        }
        String name = parts[0].strip();
        RaidPhase phase;
        try {
            phase = RaidPhase.valueOf(parts[1].strip());
        } catch (IllegalArgumentException e) {
            MillToolsMod.LOGGER.warn("[MillTools] Phase invalide dans predefined_raid : {}", parts[1]);
            return;
        }
        Map<String, Integer> mobs = new LinkedHashMap<>();
        for (String token : parts[2].strip().split("\\s+")) {
            String[] kv = token.split(":");
            if (kv.length == 2) {
                try {
                    mobs.put(kv[0].toUpperCase(), Integer.parseInt(kv[1]));
                } catch (NumberFormatException e) {
                    MillToolsMod.LOGGER.warn("[MillTools] Quantité invalide dans predefined_raid : {}", token);
                }
            }
        }
        // Remplacer si un raid du même nom existe déjà (permet override depuis le fichier)
        config.predefinedRaids.removeIf(r -> r.name().equals(name));
        config.predefinedRaids.add(new RaidConfig.PredefinedRaidDef(name, phase, mobs));
    }

    private static void writeDefaults(Path file, RaidConfig c) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# ============================================\n");
        sb.append("# MillTools - Fichier de configuration\n");
        sb.append("# Modifiez ce fichier puis redémarrez le serveur.\n");
        sb.append("# ============================================\n\n");

        sb.append("[general]\n");
        sb.append("# Recettes de craft Millenaire disparues en 9.0.0-beta (cidre, vin, huile d'olive, curry,\n");
        sb.append("# masa, wah, coton -> laine)\n");
        sb.append("restore_millenaire_recipes = ").append(c.generalRestoreMillenaireRecipes).append("\n\n");

        sb.append("[dev]\n");
        sb.append("# Prestige max + contrôle de culture au login\n");
        sb.append("auto_prestige = ").append(c.devAutoPrestige).append("\n");
        sb.append("# Culture visée par auto_prestige (ex: norman, japanese, byzantine)\n");
        sb.append("prestige_culture = ").append(c.devPrestigeCulture).append("\n");
        sb.append("# Débloque la connaissance de plantation de toutes les cultures au login, sans achat\n");
        sb.append("unlock_crop_knowledge = ").append(c.devUnlockCropKnowledge).append("\n\n");

        sb.append("[village_enemies]\n");
        sb.append("# Mobs supplémentaires traités comme ennemis par tous les villages (ex: le Slime, pas un\n");
        sb.append("# Monster vanilla). Liste séparée par des virgules : SLIME ou namespace:path. Creeper ignoré.\n");
        sb.append("enemies = ").append(String.join(", ", c.extraEnemyTypes)).append("\n");
        sb.append("# Distance min. (blocs) au spawn naturel de ces mobs près d'un bâtiment\n");
        sb.append("building_buffer = ").append(c.enemyBuildingBuffer).append("\n\n");

        sb.append("# ================================================================\n");
        sb.append("# SYSTÈME DE RAIDS NOCTURNES — sections [raid], [difficulty],\n");
        sb.append("# [mob_costs], [mob_weights] et les raids prédéfinis ci-dessous.\n");
        sb.append("# ================================================================\n\n");

        sb.append("[raid]\n");
        sb.append("# /!\\ EN COURS DE DÉVELOPPEMENT, NON FINALISÉ. Déconseillé en production.\n");
        sb.append("enabled = ").append(c.enabled).append("\n");
        sb.append("# Rayon (blocs) autour du centre du village pour considérer un joueur \"dans le village\"\n");
        sb.append("village_radius = ").append((int) c.villageRadius).append("\n");
        sb.append("# Tick de déclenchement de la phase 1 (début de nuit)\n");
        sb.append("phase1_tick = ").append(c.phase1Tick).append("\n");
        sb.append("# Tick de déclenchement de la phase 2 (milieu de nuit)\n");
        sb.append("phase2_tick = ").append(c.phase2Tick).append("\n\n");

        sb.append("[difficulty]\n");
        sb.append("# Budget de points d'un raid aléatoire en phase 1\n");
        sb.append("phase1_points = ").append(c.phase1Points).append("\n");
        sb.append("# Budget de points d'un raid aléatoire en phase 2\n");
        sb.append("phase2_points = ").append(c.phase2Points).append("\n");
        sb.append("# Bonus de points par jour mondial\n");
        sb.append("points_per_day = ").append(c.pointsPerDay).append("\n\n");

        sb.append("[mob_costs]\n");
        sb.append("# Coût en points de chaque mob (raids aléatoires)\n");
        c.mobCosts.forEach((mob, cost) -> sb.append(mob).append(" = ").append(cost).append("\n"));
        sb.append("\n");

        sb.append("[mob_weights]\n");
        sb.append("# Poids de sélection aléatoire (plus élevé = plus fréquent)\n");
        c.mobWeights.forEach((mob, weight) -> sb.append(mob).append(" = ").append(weight).append("\n"));
        sb.append("\n");

        sb.append("# ------------------------------------------------\n");
        sb.append("# Système de raids : raids prédéfinis\n");
        sb.append("# Format : predefined_raid = nom | PHASE_1 ou PHASE_2 | MOB:quantité MOB:quantité ...\n");
        sb.append("# ------------------------------------------------\n");
        c.predefinedRaids.forEach(r -> {
            sb.append("predefined_raid = ").append(r.name()).append(" | ").append(r.phase().name()).append(" | ");
            r.mobs().forEach((mob, qty) -> sb.append(mob).append(":").append(qty).append(" "));
            sb.append("\n");
        });

        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }
}
