package id.seria.crate.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;

public class PreviewGUI {

    public static Inventory createTierSelection(String bossName) {
        FileConfiguration guiConfig = SeriaCrate.getInstance().getConfigManager().getGui();
        String title = ChatColor.translateAlternateColorCodes('&', guiConfig.getString("preview-tier-gui.title", "&8Pilih Tier: %boss%").replace("%boss%", bossName.toUpperCase()));
        int size = guiConfig.getInt("preview-tier-gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        List<ItemUtils.GUIItem> allItems = new ArrayList<>();
        allItems.addAll(ItemUtils.loadGUIItems(guiConfig.getConfigurationSection("preview-tier-gui.fillers")));
        allItems.addAll(ItemUtils.loadGUIItems(guiConfig.getConfigurationSection("preview-tier-gui.items")));
        
        // Urutkan berdasarkan priority (0 ditimpa oleh 1, dst)
        allItems.sort(Comparator.comparingInt(a -> a.priority));

        for (ItemUtils.GUIItem gItem : allItems) {
            for (int slot : gItem.slots) {
                if (slot < size) inv.setItem(slot, gItem.item.clone());
            }
        }
        return inv;
    }

    public static Inventory createRewardPreview(String bossName, String tier) {
        FileConfiguration guiConfig = SeriaCrate.getInstance().getConfigManager().getGui();
        String title = ChatColor.translateAlternateColorCodes('&', guiConfig.getString("preview-reward-gui.title", "&8Reward: %boss% (%tier%)")
                .replace("%boss%", bossName.toUpperCase())
                .replace("%tier%", tier.toUpperCase()));
        int size = guiConfig.getInt("preview-reward-gui.size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);

        List<ItemUtils.GUIItem> allItems = new ArrayList<>();
        allItems.addAll(ItemUtils.loadGUIItems(guiConfig.getConfigurationSection("preview-reward-gui.fillers")));
        allItems.sort(Comparator.comparingInt(a -> a.priority));
        
        for (ItemUtils.GUIItem gItem : allItems) {
            for (int slot : gItem.slots) {
                if (slot < size) inv.setItem(slot, gItem.item.clone());
            }
        }

        // Generate Item Hadiah
        List<Reward> rewards = SeriaCrate.getInstance().getRewardManager().getRewardsFor(bossName, tier);
        int totalWeight = rewards.stream().mapToInt(Reward::getWeight).sum();
        int rewardIndex = 0;

        for (int i = 0; i < size; i++) {
            // Cari slot yang kosong untuk menaruh hadiah
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                if (rewardIndex >= rewards.size()) break;
                
                Reward reward = rewards.get(rewardIndex);
                ItemStack item = ItemUtils.buildRewardItem(reward);
                ItemMeta meta = item.getItemMeta();
                
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    lore.add(" ");
                    double chance = totalWeight > 0 ? ((double) reward.getWeight() / totalWeight) * 100 : 0;
                    
                    String tierColor = "&f";
                    if (guiConfig.contains("preview-tier-gui.items." + tier.toLowerCase() + ".name")) {
                        String name = guiConfig.getString("preview-tier-gui.items." + tier.toLowerCase() + ".name");
                        if (name.length() >= 4) tierColor = name.substring(0, 4);
                    }

                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Tier: " + tierColor + "Tier " + tier.toUpperCase()));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Peluang: &a" + String.format("%.2f", chance) + "%"));
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(i, item);
                rewardIndex++;
            }
        }
        return inv;
    }

    public static Inventory createPreview(String bossName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}