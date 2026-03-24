package id.seria.crate.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;

public class PreviewGUI {

    public static Inventory createPreview(String bossName) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Preview: " + bossName.toUpperCase() + " Crate");

        String[] tiers = {"s", "a", "b", "c", "d"};
        int slot = 0;

        for (String tier : tiers) {
            List<Reward> rewards = SeriaCrate.getInstance().getRewardManager().getRewardsFor(bossName, tier);
            if (rewards.isEmpty()) continue;

            // Hitung total weight di tier ini untuk mencari persentase
            int totalWeight = rewards.stream().mapToInt(Reward::getWeight).sum();

            for (Reward reward : rewards) {
                if (slot >= 54) break; // Cegah error jika hadiah lebih dari 54

                ItemStack item = ItemUtils.buildRewardItem(reward);
                ItemMeta meta = item.getItemMeta();
                
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    lore.add(" ");
                    
                    // Kalkulasi persentase: (weight / total) * 100
                    double chance = ((double) reward.getWeight() / totalWeight) * 100;
                    String formattedChance = String.format("%.2f", chance); // 2 angka di belakang koma
                    
                    String tierColor = getTierColor(tier);
                    lore.add("§7Tier: " + tierColor + "Tier " + tier.toUpperCase());
                    lore.add("§7Peluang di Tier ini: §a" + formattedChance + "%");
                    
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                
                inv.setItem(slot, item);
                slot++;
            }
        }
        return inv;
    }

    private static String getTierColor(String tier) {
        switch (tier.toLowerCase()) {
            case "s": return "§c§l"; // Merah muda/Tebal
            case "a": return "§6§l"; // Emas
            case "b": return "§e§l"; // Kuning
            case "c": return "§b§l"; // Biru Muda
            default: return "§f§l";  // Putih
        }
    }
}