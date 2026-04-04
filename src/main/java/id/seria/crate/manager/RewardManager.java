package id.seria.crate.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;

public class RewardManager {

    private final SeriaCrate plugin;
    private final Map<String, Map<String, List<Reward>>> crateRewards = new HashMap<>();

    public RewardManager(SeriaCrate plugin) { this.plugin = plugin; }

    public void loadRewards() {
        crateRewards.clear();
        File rewardsFolder = plugin.getConfigManager().getRewardsFolder();
        if (rewardsFolder == null || !rewardsFolder.exists()) return;

        File[] files = rewardsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
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

                    int weight = itemData.getInt("weight", 1);
                    int amount = itemData.getInt("amount", 1);
                    String materialStr = itemData.getString("material", "STONE");

                    // 1. TAMPILAN BERDASARKAN TEXTURE (ANTI-STEVE)
                    ItemStack displayItem;
                    if (itemData.contains("texture")) {
                        displayItem = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta sm = (SkullMeta) displayItem.getItemMeta();
                        ItemUtils.applyTexture(sm, itemData.getString("texture"));
                        displayItem.setItemMeta(sm);
                    } else if (materialStr.startsWith("mmoitems-")) {
                        displayItem = new ItemStack(Material.STONE);
                        try {
                            String[] parts = materialStr.substring(9).split(":");
                            ItemStack mi = net.Indyuce.mmoitems.MMOItems.plugin.getItem(net.Indyuce.mmoitems.api.Type.get(parts[0]), parts[1]);
                            if (mi != null) displayItem = mi;
                        } catch (Exception ignored) {}
                    } else {
                        Material mat = Material.matchMaterial(materialStr);
                        displayItem = new ItemStack(mat != null ? mat : Material.STONE);
                    }

                    // 2. Terapkan Nama & Lore
                    ItemMeta meta = displayItem.getItemMeta();
                    if (meta != null) {
                        if (itemData.contains("display_name")) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemData.getString("display_name")));
                        if (itemData.contains("lore")) {
                            List<String> lore = new ArrayList<>();
                            for (String l : itemData.getStringList("lore")) lore.add(ChatColor.translateAlternateColorCodes('&', l));
                            meta.setLore(lore);
                        }
                        if (itemData.contains("enchant")) {
                            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                            if (itemData.getBoolean("hide_enchant", false)) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        }
                        displayItem.setItemMeta(meta);
                    }

                    // 3. Command List Bersih
                    List<String> commands = new ArrayList<>();
                    if (itemData.contains("cmd") && itemData.getString("cmd") != null) commands.add(itemData.getString("cmd"));
                    if (itemData.contains("commands")) commands.addAll(itemData.getStringList("commands"));

                    List<String> winItemsClean = itemData.getStringList("win_items");
                    boolean broadcast = itemData.getBoolean("broadcast", false);

                    rewardList.add(new Reward(weight, amount, displayItem, commands, winItemsClean, broadcast));
                }
                tiersMap.put(tierId.toLowerCase(), rewardList);
            }
            crateRewards.put(crateId, tiersMap);
        }
    }

    public List<Reward> getRewardsFor(String bossName, String tier) {
        Map<String, List<Reward>> tiers = crateRewards.get(bossName.toLowerCase());
        return (tiers != null && tiers.containsKey(tier.toLowerCase())) ? tiers.get(tier.toLowerCase()) : new ArrayList<>();
    }

    public int createDefaultReward(String crateId, String tierId) {
        File file = new File(plugin.getConfigManager().getRewardsFolder(), crateId.toLowerCase() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String basePath = "tiers." + tierId.toLowerCase();
        int newIndex = 1;
        if (config.getConfigurationSection(basePath) != null) {
            for (String key : config.getConfigurationSection(basePath).getKeys(false)) {
                try { int id = Integer.parseInt(key); if (id >= newIndex) newIndex = id + 1; } catch (Exception ignored) {}
            }
        }
        config.set(basePath + "." + newIndex + ".type", "ITEM");
        config.set(basePath + "." + newIndex + ".material", "IRON_BARS");
        config.set(basePath + "." + newIndex + ".weight", 1);
        config.set(basePath + "." + newIndex + ".amount", 1);
        try { config.save(file); loadRewards(); } catch (IOException ignored) {}
        return newIndex;
    }

    // AUTO-GENERATOR CONFIG SUPER RAPI (DENGAN TEXTURE SUPPORT)
    public void updateRewardItem(String crateId, String tierId, int index, ItemStack item) {
        File file = new File(plugin.getConfigManager().getRewardsFolder(), crateId.toLowerCase() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "tiers." + tierId.toLowerCase() + "." + index;

        String cleanData = ItemUtils.serializeItemClean(item);
        String[] parts = cleanData.split(":");
        String typeStr = "ITEM";
        String materialStr = "";
        String cmdStr = "";

        if (cleanData.startsWith("ecopets:")) {
            typeStr = "PET"; materialStr = "ecopets:" + parts[1];
            cmdStr = "ecopets giveegg %player_name% " + parts[1]; // Sesuai format yang kamu mau
        } else if (cleanData.startsWith("mmoitems:")) {
            materialStr = "mmoitems-" + parts[1] + ":" + parts[2];
            cmdStr = "mi give " + parts[1] + " " + parts[2] + " %player_name% " + item.getAmount();
        } else {
            materialStr = item.getType().name();
            cmdStr = "give %player_name% " + materialStr.toLowerCase() + " " + item.getAmount();
        }

        // Hapus path lama agar config tetap bersih
        config.set(path + ".cmd", null);
        config.set(path + ".gui_item_b64", null);
        config.set(path + ".win_items_b64", null);
        
        config.set(path + ".type", typeStr);
        config.set(path + ".material", materialStr);
        config.set(path + ".commands", java.util.Arrays.asList(cmdStr));

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // EKSTRAK TEXTURE BASE64 (Mencegah Steve Head)
            if (item.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
                String tex = ItemUtils.getTexture((SkullMeta) meta);
                if (tex != null) config.set(path + ".texture", tex);
            } else {
                config.set(path + ".texture", null);
            }

            if (meta.hasDisplayName()) config.set(path + ".display_name", meta.getDisplayName().replace("§", "&"));
            if (meta.hasLore()) {
                List<String> cleanLore = new ArrayList<>();
                for (String l : meta.getLore()) cleanLore.add(l.replace("§", "&"));
                config.set(path + ".lore", cleanLore);
            }
            if (meta.hasEnchants()) {
                config.set(path + ".enchant", "unbreaking");
                config.set(path + ".hide_enchant", meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));
            }
        }
        try { config.save(file); loadRewards(); } catch (IOException ignored) {}
    }

    public void updateRewardTextData(String crateId, String tierId, int index, String key, String value) {
        File file = new File(plugin.getConfigManager().getRewardsFolder(), crateId.toLowerCase() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("tiers." + tierId.toLowerCase() + "." + index + "." + key, (value.equalsIgnoreCase("hapus")) ? null : value);
        try { config.save(file); loadRewards(); } catch (IOException ignored) {}
    }

    public void deleteReward(String crateId, String tierId, int index) {
        File file = new File(plugin.getConfigManager().getRewardsFolder(), crateId.toLowerCase() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("tiers." + tierId.toLowerCase() + "." + index, null);
        try { config.save(file); loadRewards(); } catch (IOException ignored) {}
    }

    public void addRewardCommand(String crateId, String tierId, int index, String command) {
        File file = new File(plugin.getConfigManager().getRewardsFolder(), crateId.toLowerCase() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "tiers." + tierId.toLowerCase() + "." + index + ".commands";
        List<String> cmds = config.getStringList(path);
        cmds.add(command); config.set(path, cmds);
        config.set("tiers." + tierId.toLowerCase() + "." + index + ".cmd", null);
        try { config.save(file); loadRewards(); } catch (IOException ignored) {}
    }

    public void removeRewardCommand(String crateId, String tierId, int index, int cmdIndex) {
        File file = new File(plugin.getConfigManager().getRewardsFolder(), crateId.toLowerCase() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "tiers." + tierId.toLowerCase() + "." + index + ".commands";
        List<String> cmds = config.getStringList(path);
        if (cmdIndex >= 0 && cmdIndex < cmds.size()) {
            cmds.remove(cmdIndex); config.set(path, cmds.isEmpty() ? null : cmds);
            try { config.save(file); loadRewards(); } catch (IOException ignored) {}
        }
    }

    public void addRewardWinItem(String crateId, String tierId, int index, ItemStack item) {
        File file = new File(plugin.getConfigManager().getRewardsFolder(), crateId.toLowerCase() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "tiers." + tierId.toLowerCase() + "." + index + ".win_items";
        List<String> items = config.getStringList(path);
        items.add(ItemUtils.serializeItemClean(item));
        config.set(path, items);
        try { config.save(file); loadRewards(); } catch (IOException ignored) {}
    }

    public void removeRewardWinItem(String crateId, String tierId, int index, int itemIndex) {
        File file = new File(plugin.getConfigManager().getRewardsFolder(), crateId.toLowerCase() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "tiers." + tierId.toLowerCase() + "." + index + ".win_items";
        List<String> items = config.getStringList(path);
        if (itemIndex >= 0 && itemIndex < items.size()) {
            items.remove(itemIndex); config.set(path, items.isEmpty() ? null : items);
            try { config.save(file); loadRewards(); } catch (IOException ignored) {}
        }
    }

    public Reward getRewardExact(String crateId, String tierId, int rewardIndex) {
        List<Reward> rewards = getRewardsFor(crateId, tierId);
        if (rewardIndex >= 0 && rewardIndex < rewards.size()) {
            return rewards.get(rewardIndex);
        }
        return null;
    }

    String getFolder() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}