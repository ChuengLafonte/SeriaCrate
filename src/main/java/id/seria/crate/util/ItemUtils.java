package id.seria.crate.util;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;

public class ItemUtils {

    public static ItemStack buildRewardItem(Reward reward) {
        ItemStack itemStack = null;
        String path = reward.getMaterialPath();

        // 1. Jika ini adalah PET dan punya wujud asli dari GUI Editor
        if (reward.getType().equals("PET") && reward.getGuiItem() != null) {
            itemStack = reward.getGuiItem().clone();
            itemStack.setAmount(reward.getAmount());
        } 
        // 2. FALLBACK: Jika ini PET tapi ditulis manual di config (belum ada wujud fisik)
        else if (path.startsWith("ecopet:")) {
            itemStack = new ItemStack(Material.WOLF_SPAWN_EGG); // Ubah jadi telur, bukan batu
        }
        // 3. Logika MMOItems
        else if (path.startsWith("mmoitems-")) {
            String raw = path.replace("mmoitems-", "");
            String[] split = raw.split(":");
            if (split.length == 2) {
                net.Indyuce.mmoitems.api.Type type = net.Indyuce.mmoitems.MMOItems.plugin.getTypes().get(split[0].toUpperCase());
                itemStack = net.Indyuce.mmoitems.MMOItems.plugin.getItem(type, split[1].toUpperCase());
            }
        } 
        // 4. Logika Vanilla
        else {
            Material mat = Material.matchMaterial(path.toUpperCase());
            itemStack = new ItemStack(mat != null ? mat : Material.STONE);
        }

        if (itemStack == null) itemStack = new ItemStack(Material.BARRIER);
        itemStack.setAmount(reward.getAmount());

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (reward.getDisplayName() != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', reward.getDisplayName()));
            }

            if (!reward.getLore().isEmpty()) {
                List<String> coloredLore = reward.getLore().stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .collect(Collectors.toList());
                meta.setLore(coloredLore);
            }

            // Injeksi PDC agar bisa dibaca ulang oleh Editor
            NamespacedKey typeKey = new NamespacedKey(SeriaCrate.getInstance(), "reward_type");
            NamespacedKey matKey = new NamespacedKey(SeriaCrate.getInstance(), "reward_material");
            NamespacedKey weightKey = new NamespacedKey(SeriaCrate.getInstance(), "reward_weight");
            
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, reward.getType());
            meta.getPersistentDataContainer().set(matKey, PersistentDataType.STRING, reward.getMaterialPath());
            meta.getPersistentDataContainer().set(weightKey, PersistentDataType.INTEGER, reward.getWeight());

            if (reward.getCommand() != null) {
                NamespacedKey cmdKey = new NamespacedKey(SeriaCrate.getInstance(), "reward_cmd");
                meta.getPersistentDataContainer().set(cmdKey, PersistentDataType.STRING, reward.getCommand());
            }

            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }
    // Di dalam ItemUtils.java, ubah fungsi getCrateItem:
    public static ItemStack getCrateItem(String bossName) {
        java.io.File file = new java.io.File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), bossName + ".yml");
        org.bukkit.configuration.file.FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        String blockName = config.getString("crate-settings.block", "ENDER_CHEST");
        Material mat = Material.matchMaterial(blockName);
        if (mat == null) mat = Material.ENDER_CHEST;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§l" + bossName.toUpperCase() + " CRATE");
            meta.setLore(java.util.Arrays.asList("§7Letakkan blok ini di lantai", "§7untuk membuat Crate permanen."));
            NamespacedKey key = new NamespacedKey(SeriaCrate.getInstance(), "crate_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, bossName.toLowerCase());
            item.setItemMeta(meta);
        }
        return item;
    }
}