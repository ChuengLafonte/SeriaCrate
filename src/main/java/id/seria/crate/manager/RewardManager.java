package id.seria.crate.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;

public class RewardManager {

    private final SeriaCrate plugin;
    private final Map<String, Map<String, List<Reward>>> crateRewards = new HashMap<>();

    public RewardManager(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    public void loadRewards() {
        crateRewards.clear(); // Bersihkan memori lama
        
        File rewardsFolder = plugin.getConfigManager().getRewardsFolder();
        if (rewardsFolder == null || !rewardsFolder.exists() || !rewardsFolder.isDirectory()) {
            plugin.getLogger().warning("Folder rewards tidak ditemukan!");
            return;
        }

        // Ambil semua file berakhiran .yml di dalam folder rewards/
        File[] files = rewardsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("Belum ada file crate di dalam folder rewards. Silakan buat via GUI Editor.");
            return;
        }

        for (File file : files) {
            // Nama file (tanpa .yml) akan menjadi ID Crate. Contoh: saok.yml -> saok
            String crateId = file.getName().replace(".yml", "").toLowerCase();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            ConfigurationSection tiersSection = config.getConfigurationSection("tiers");
            if (tiersSection == null) continue;

            Map<String, List<Reward>> tiersMap = new HashMap<>();

            for (String tierId : tiersSection.getKeys(false)) {
                ConfigurationSection itemsSection = tiersSection.getConfigurationSection(tierId);
                if (itemsSection == null) continue;

                List<Reward> rewardList = new ArrayList<>();

                for (String itemKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemData = itemsSection.getConfigurationSection(itemKey);
                    if (itemData == null) continue;

                    String type = itemData.getString("type", "ITEM").toUpperCase();
                    String material = itemData.getString("material", "STONE");
                    int weight = itemData.getInt("weight", 1);
                    int amount = itemData.getInt("amount", 1);
                    String displayName = itemData.getString("display_name", null);
                    List<String> lore = itemData.getStringList("lore");
                    String enchant = itemData.getString("enchant", null);
                    boolean hideEnchant = itemData.getBoolean("hide_enchant", false);
                    String command = itemData.getString("cmd", null);
                    ItemStack guiItem = itemData.getItemStack("gui_item");

                    Reward reward = new Reward(type, material, weight, amount, displayName, lore, enchant, hideEnchant, command, guiItem);
                    rewardList.add(reward);
                }
                
                tiersMap.put(tierId.toLowerCase(), rewardList);
            }
            crateRewards.put(crateId, tiersMap);
        }
        
        plugin.getLogger().info("Berhasil memuat " + crateRewards.size() + " Crate Data dari folder rewards!");
    }

    public List<Reward> getRewardsFor(String bossName, String tier) {
        Map<String, List<Reward>> tiers = crateRewards.get(bossName.toLowerCase());
        if (tiers != null && tiers.containsKey(tier.toLowerCase())) {
            return tiers.get(tier.toLowerCase());
        }
        return new ArrayList<>();
    }

    public void saveRewardsFromEditor(String crateId, String tierId, ItemStack[] contents) {
        // Tembak langsung ke file crate spesifik!
        File file = new File(plugin.getConfigManager().getRewardsFolder(), crateId.toLowerCase() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Path-nya sekarang lebih pendek karena filenya terpisah
        String basePath = "tiers." + tierId.toLowerCase();
        config.set(basePath, null); // Bersihkan tier ini sebelum rewrite

        NamespacedKey typeKey = new NamespacedKey(plugin, "reward_type");
        NamespacedKey matKey = new NamespacedKey(plugin, "reward_material");
        NamespacedKey weightKey = new NamespacedKey(plugin, "reward_weight");
        NamespacedKey cmdKey = new NamespacedKey(plugin, "reward_cmd");
        NamespacedKey ecoPetKey = new NamespacedKey("ecopets", "pet");

        int index = 1;
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) continue;

            String itemPath = basePath + "." + index;
            String typeStr = "ITEM";
            String materialStr = item.getType().name();
            String cmdStr = null;
            int weight = 1;

            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                if (meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                    typeStr = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
                    materialStr = meta.getPersistentDataContainer().get(matKey, PersistentDataType.STRING);
                    weight = meta.getPersistentDataContainer().getOrDefault(weightKey, PersistentDataType.INTEGER, 1);
                    if (meta.getPersistentDataContainer().has(cmdKey, PersistentDataType.STRING)) {
                        cmdStr = meta.getPersistentDataContainer().get(cmdKey, PersistentDataType.STRING);
                    }
                } else if (meta.getPersistentDataContainer().has(ecoPetKey, PersistentDataType.STRING)) {
                    typeStr = "PET";
                    String petId = meta.getPersistentDataContainer().get(ecoPetKey, PersistentDataType.STRING);
                    materialStr = "ecopet:" + petId;
                } else {
                    io.lumine.mythic.lib.api.item.NBTItem nbtItem = io.lumine.mythic.lib.api.item.NBTItem.get(item);
                    if (nbtItem.hasType() && nbtItem.getString("MMOITEMS_ITEM_ID") != null && !nbtItem.getString("MMOITEMS_ITEM_ID").isEmpty()) {
                        materialStr = "mmoitems-" + nbtItem.getType() + ":" + nbtItem.getString("MMOITEMS_ITEM_ID");
                    }
                }

                if (meta.hasDisplayName()) {
                    config.set(itemPath + ".display_name", meta.getDisplayName().replace("§", "&"));
                }
                if (meta.hasLore()) {
                    List<String> rawLore = new ArrayList<>();
                    for (String l : meta.getLore()) {
                        rawLore.add(l.replace("§", "&"));
                    }
                    config.set(itemPath + ".lore", rawLore);
                }
            }

            config.set(itemPath + ".type", typeStr);
            config.set(itemPath + ".material", materialStr);
            config.set(itemPath + ".weight", weight);
            config.set(itemPath + ".amount", item.getAmount());
            if (cmdStr != null) config.set(itemPath + ".cmd", cmdStr);

            if (typeStr.equals("PET")) {
                config.set(itemPath + ".gui_item", item.clone());
            }

            index++;
        }

        try {
            config.save(file);
            loadRewards(); 
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan rewards.yml ke " + file.getName());
        }
    }
}