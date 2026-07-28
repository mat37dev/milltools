package com.millenaire.milltools.raid.defense;

import com.millenaire.milltools.MillToolsMod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.millenaire.entity.MillVillager;

import java.util.List;

public class GuardEquipmentManager {

    // Ordered worst → best (index = tier, used for comparison)
    private static final List<Item> VANILLA_SWORDS = List.of(
        Items.WOODEN_SWORD,   // tier 0
        Items.GOLDEN_SWORD,   // tier 1
        Items.STONE_SWORD,    // tier 2
        Items.IRON_SWORD,     // tier 3
        Items.DIAMOND_SWORD,  // tier 4
        Items.NETHERITE_SWORD // tier 5
    );

    // L'armure n'est PAS gérée ici : MillVillager.getItemBySlot() recalcule dynamiquement la
    // meilleure pièce possédée depuis tool_categories.json/inventaire à chaque lecture (rendu,
    // calcul de protection...), en ignorant totalement ce qu'on poserait via setItemSlot. Toute
    // tentative d'équiper manuellement une armure ici (y compris pour du Netherite, absent du
    // JSON) est donc invisible en jeu — cause du "les gardes n'équipent pas leur armure" observé.

    public static void equipBestAvailable(MillVillager villager) {
        equipWeapon(villager);
        MillToolsMod.LOGGER.debug("[MillTools] Équipement mis à jour pour garde {}",
                villager.getUUID());
    }

    // ── Weapon ───────────────────────────────────────────────────────────────

    private static void equipWeapon(MillVillager villager) {
        // Millenaire gère sa propre sélection d'arme (poignard, arbalète, etc.)
        villager.ensureCombatWeaponEquipped();

        // Chercher la meilleure épée vanilla dans l'inventaire (y compris Netherite, absent
        // de tool_categories.json donc jamais choisi par ensureCombatWeaponEquipped)
        Item bestVanilla = bestOwnedByTier(villager, VANILLA_SWORDS);
        if (bestVanilla == null) return;

        int vanillaTier = VANILLA_SWORDS.indexOf(bestVanilla);
        ItemStack current = villager.getMainHandItem();
        int currentTier = current.isEmpty() ? -1 : VANILLA_SWORDS.indexOf(current.getItem());

        // Remplacer si l'épée vanilla est meilleure que ce qui est équipé
        if (vanillaTier > currentTier) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(bestVanilla));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Retourne le meilleur item possédé selon l'ordre de la liste (dernier = meilleur). */
    private static Item bestOwnedByTier(MillVillager villager, List<Item> orderedList) {
        if (orderedList == null) return null;
        Item best = null;
        for (Item item : orderedList) {
            if (villager.getInventory().getCount(item) > 0) {
                best = item; // dernière correspondance = tier le plus élevé
            }
        }
        return best;
    }
}
