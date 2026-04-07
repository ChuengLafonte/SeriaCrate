package id.seria.crate.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import com.destroystokyo.paper.profile.PlayerProfile;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;

public class ItemUtils {

    // ==========================================
    // [BARU] SISTEM ULTIMATE GUI PARSER
    // ==========================================
    public static class GUIItem {
        public int priority;
        public List<Integer> slots;
        public ItemStack item;
        public GUIItem(int priority, List<Integer> slots, ItemStack item) {
            this.priority = priority; this.slots = slots; this.item = item;
        }
    }

    public static List<Integer> parseSlots(List<String> slotStrings) {
        List<Integer> slots = new ArrayList<>();
        if (slotStrings == null) return slots;
        for (String s : slotStrings) {
            if (s.contains("-")) {
                try {
                    String[] parts = s.split("-");
                    int start = Integer.parseInt(parts[0].trim());
                    int end = Integer.parseInt(parts[1].trim());
                    for (int i = start; i <= end; i++) slots.add(i);
                } catch (Exception ignored) {}
            } else {
                try { slots.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
            }
        }
        return slots;
    }

    public static List<GUIItem> loadGUIItems(ConfigurationSection section) {
        List<GUIItem> guiItems = new ArrayList<>();
        if (section == null) return guiItems;

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemCfg = section.getConfigurationSection(key);
            if (itemCfg == null) continue;

            int priority = itemCfg.getInt("priority", 0);
            List<Integer> slots = parseSlots(itemCfg.getStringList("slots"));
            Material mat = Material.matchMaterial(itemCfg.getString("material", "STONE"));
            
            ItemStack item = new ItemStack(mat != null ? mat : Material.STONE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (itemCfg.contains("name")) {
                    meta.displayName(TextUtils.format(itemCfg.getString("name")));
                }
                if (itemCfg.contains("lore")) {
                    meta.lore(TextUtils.formatList(itemCfg.getStringList("lore")));
                }
                item.setItemMeta(meta);
            }
            guiItems.add(new GUIItem(priority, slots, item));
        }
        return guiItems;
    }
    // ==========================================

    public static ItemStack buildRewardItem(Reward reward) {
        if (reward.getDisplayItem() == null) return new ItemStack(Material.STONE);
        ItemStack item = reward.getDisplayItem().clone();
        item.setAmount(reward.getAmount());
        return item;
    }

    public static ItemStack getCrateItem(String bossName) {
        File file = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), bossName + ".yml");
        org.bukkit.configuration.file.FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        Material mat = Material.matchMaterial(config.getString("crate-settings.block", "ENDER_CHEST"));
        
        ItemStack item = new ItemStack(mat != null ? mat : Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtils.format("&l&e" + bossName.toUpperCase() + " CRATE"));
            meta.lore(java.util.Arrays.asList(
                TextUtils.format("&7Letakkan blok ini di lantai"),
                TextUtils.format("&7untuk membuat Crate permanen.")
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(SeriaCrate.getInstance(), "crate_id"), PersistentDataType.STRING, bossName.toLowerCase());
            item.setItemMeta(meta);
        }
        return item;
    }

    @SuppressWarnings("deprecation")
    public static String getTexture(SkullMeta meta) {
        try {
            org.bukkit.profile.PlayerProfile profile = meta.getPlayerProfile();
            if (profile != null && profile.getTextures().getSkin() != null) {
                String url = profile.getTextures().getSkin().toString();
                String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
                return java.util.Base64.getEncoder().encodeToString(json.getBytes());
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static void applyTexture(SkullMeta meta, String b64) {
        if (b64 == null || b64.isEmpty()) return;
        try {
            PlayerProfile profile = org.bukkit.Bukkit.createProfile(java.util.UUID.randomUUID());
            profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", b64));
            meta.setPlayerProfile(profile);
        } catch (Exception ignored) {}
    }

    public static String serializeItemClean(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey("ecopets", "pet_egg"), PersistentDataType.STRING)) {
            return "ecopets:" + meta.getPersistentDataContainer().get(new NamespacedKey("ecopets", "pet_egg"), PersistentDataType.STRING) + ":" + item.getAmount();
        }
        try {
            io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
            if (nbt.hasType() && nbt.getString("MMOITEMS_ITEM_ID") != null && !nbt.getString("MMOITEMS_ITEM_ID").isEmpty()) {
                return "mmoitems:" + nbt.getType() + ":" + nbt.getString("MMOITEMS_ITEM_ID") + ":" + item.getAmount();
            }
        } catch(Exception ignored) {}
        return "vanilla:" + item.getType().name() + ":" + item.getAmount();
    }

    public static ItemStack deserializeItemClean(String str) {
        if (str == null || str.isEmpty()) return new ItemStack(Material.STONE);
        String[] parts = str.split(":");
        int amount = 1;
        try { amount = Integer.parseInt(parts[parts.length - 1]); } catch(Exception ignored) {}
        if (str.startsWith("ecopets:")) {
            try {
                Object testableItem = Class.forName("com.willfp.eco.core.items.Items").getMethod("lookup", String.class).invoke(null, "ecopets:" + parts[1]);
                if (testableItem != null) {
                    ItemStack pet = (ItemStack) testableItem.getClass().getMethod("getItem").invoke(testableItem);
                    if (pet != null) { pet.setAmount(amount); return pet; }
                }
            } catch(Exception ignored) {}
            ItemStack fb = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta m = fb.getItemMeta(); 
            if (m != null) {
                m.displayName(TextUtils.format("&aEcoPets: " + parts[1])); 
                fb.setItemMeta(m);
            }
            return fb;
        } else if (str.startsWith("mmoitems:")) {
            try {
                ItemStack mi = net.Indyuce.mmoitems.MMOItems.plugin.getItem(net.Indyuce.mmoitems.api.Type.get(parts[1]), parts[2]);
                if (mi != null) { mi.setAmount(amount); return mi; }
            } catch(Exception ignored) {}
        } else if (str.startsWith("vanilla:")) {
            Material mat = Material.matchMaterial(parts[1]);
            if (mat != null) return new ItemStack(mat, amount);
        }
        return new ItemStack(Material.STONE);
    }
}