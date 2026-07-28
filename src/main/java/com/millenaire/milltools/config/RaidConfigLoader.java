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
        if (!Files.exists(file)) {
            try {
                Files.createDirectories(file.getParent());
                writeDefaults(file, RaidConfig.defaults());
                MillToolsMod.LOGGER.info("[MillTools] Fichier de config généré : {}", file);
            } catch (IOException e) {
                MillToolsMod.LOGGER.error("[MillTools] Impossible de créer milltools.cfg : {}", e.getMessage());
                return RaidConfig.defaults();
            }
        }

        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return parse(lines);
        } catch (IOException e) {
            MillToolsMod.LOGGER.error("[MillTools] Impossible de lire milltools.cfg : {}", e.getMessage());
            return RaidConfig.defaults();
        }
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

        sb.append("[dev]\n");
        sb.append("# Active le mode développeur : octroie automatiquement prestige max + contrôle culture au login (true/false)\n");
        sb.append("auto_prestige = ").append(c.devAutoPrestige).append("\n");
        sb.append("# Culture visée par auto_prestige (ex: norman, japanese, byzantine)\n");
        sb.append("prestige_culture = ").append(c.devPrestigeCulture).append("\n\n");

        sb.append("[village_enemies]\n");
        sb.append("# Mobs supplémentaires toujours traités comme ennemis par TOUS les villages (chassés par les\n");
        sb.append("# villageois 'helpInAttacks', comme les monstres vanilla). Utile pour les mobs que Millenaire ne\n");
        sb.append("# reconnaît pas nativement comme hostiles (ex: le Slime, qui n'est pas un 'Monster' vanilla).\n");
        sb.append("# Liste séparée par des virgules : ID court vanilla en majuscules (ex: SLIME) ou ID complet\n");
        sb.append("# namespace:path pour un mob d'un autre mod (ex: monmod:custom_mob). Le Creeper est\n");
        sb.append("# volontairement ignoré même si présent dans la liste (explosions près des bâtiments).\n");
        sb.append("enemies = ").append(String.join(", ", c.extraEnemyTypes)).append("\n");
        sb.append("# Distance minimale (blocs, depuis les côtés des bâtiments, pas leur centre) à laquelle ces\n");
        sb.append("# mobs peuvent apparaître naturellement près d'un bâtiment Millenaire.\n");
        sb.append("building_buffer = ").append(c.enemyBuildingBuffer).append("\n\n");

        sb.append("# ================================================================\n");
        sb.append("# SYSTÈME DE RAIDS NOCTURNES — sections [raid], [difficulty],\n");
        sb.append("# [mob_costs], [mob_weights] et les raids prédéfinis ci-dessous.\n");
        sb.append("# ================================================================\n\n");

        sb.append("[raid]\n");
        sb.append("# /!\\ Système de raids nocturnes : EN COURS DE DÉVELOPPEMENT, NON FINALISÉ.\n");
        sb.append("# Il est fortement déconseillé de l'activer (enabled = true) en l'état : comportement\n");
        sb.append("# non garanti, bugs possibles. À activer uniquement pour tester/contribuer au développement.\n");
        sb.append("enabled = ").append(c.enabled).append("\n");
        sb.append("# Rayon (blocs) autour du centre du village pour considérer un joueur 'dans le village'\n");
        sb.append("village_radius = ").append((int) c.villageRadius).append("\n");
        sb.append("# Tick de déclenchement de la phase 1 (début de nuit)\n");
        sb.append("phase1_tick = ").append(c.phase1Tick).append("\n");
        sb.append("# Tick de déclenchement de la phase 2 (milieu de nuit)\n");
        sb.append("phase2_tick = ").append(c.phase2Tick).append("\n\n");

        sb.append("[difficulty]\n");
        sb.append("# Système de raids : budget de points pour un raid aléatoire en phase 1\n");
        sb.append("phase1_points = ").append(c.phase1Points).append("\n");
        sb.append("# Système de raids : budget de points pour un raid aléatoire en phase 2\n");
        sb.append("phase2_points = ").append(c.phase2Points).append("\n");
        sb.append("# Système de raids : points bonus par jour mondial (scaling progressif)\n");
        sb.append("points_per_day = ").append(c.pointsPerDay).append("\n\n");

        sb.append("[mob_costs]\n");
        sb.append("# Système de raids : coût en points de chaque type de monstre (pour les raids aléatoires)\n");
        c.mobCosts.forEach((mob, cost) -> sb.append(mob).append(" = ").append(cost).append("\n"));
        sb.append("\n");

        sb.append("[mob_weights]\n");
        sb.append("# Système de raids : poids de sélection aléatoire (plus le chiffre est élevé, plus le mob apparaît souvent)\n");
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
