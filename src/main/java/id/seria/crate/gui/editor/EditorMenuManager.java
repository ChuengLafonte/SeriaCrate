package id.seria.crate.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;

public class EditorMenuManager {

    private static ItemStack createIcon(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String l : lore) loreList.add(l);
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final List<Material> VALID_BLOCKS = new ArrayList<>();
    static {
        for (Material mat : Material.values()) {
            if (mat.isBlock() && mat.isItem() && !mat.isAir() && !mat.name().startsWith("LEGACY_")) {
                VALID_BLOCKS.add(mat);
            }
        }
    }

    public static void openMainMenu(Player player) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.MAIN_MENU, null, null, -1);
        FileConfiguration guiConfig = SeriaCrate.getInstance().getConfigManager().getGui();
        int size = guiConfig.getInt("editor-main.size", 54);
        String title = org.bukkit.ChatColor.translateAlternateColorCodes('&', guiConfig.getString("editor-main.title", "Editor > Crates"));
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        List<ItemUtils.GUIItem> allItems = new ArrayList<>();
        allItems.addAll(ItemUtils.loadGUIItems(guiConfig.getConfigurationSection("editor-main.fillers")));
        allItems.addAll(ItemUtils.loadGUIItems(guiConfig.getConfigurationSection("editor-main.items")));
        allItems.sort(java.util.Comparator.comparingInt(a -> a.priority));

        for (ItemUtils.GUIItem gItem : allItems) {
            for (int slot : gItem.slots) {
                if (slot < size) inv.setItem(slot, gItem.item.clone());
            }
        }

        // Load Crate Files ke Slot Kosong
        File folder = SeriaCrate.getInstance().getConfigManager().getRewardsFolder();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        int currentSlot = 0;
        if (files != null) {
            for (File file : files) {
                while (currentSlot < size && inv.getItem(currentSlot) != null && inv.getItem(currentSlot).getType() != Material.AIR) {
                    currentSlot++;
                }
                if (currentSlot >= size) break;

                String crateId = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                Material mat = Material.matchMaterial(config.getString("crate-settings.block", "ENDER_CHEST"));
                inv.setItem(currentSlot, createIcon(mat != null ? mat : Material.ENDER_CHEST, "§a" + crateId.toUpperCase()));
                currentSlot++;
            }
        }
        player.openInventory(inv);
    }

    public static void openCrateSettings(Player player, String crateId) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.CRATE_SETTINGS, crateId, null, -1);
        FileConfiguration guiConfig = SeriaCrate.getInstance().getConfigManager().getGui();
        int size = guiConfig.getInt("editor-settings.size", 54);
        String title = org.bukkit.ChatColor.translateAlternateColorCodes('&', guiConfig.getString("editor-settings.title", "Editor > ... > Crate"));
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        // Ambil Data Asli dari Crate Config
        File file = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), crateId + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean isTemp = config.getBoolean("crate-settings.is-temporary", false);
        int duration = config.getInt("crate-settings.duration", 180);
        boolean isHoloEnabled = config.getBoolean("crate-settings.hologram", true);
        int resinCost = config.getInt("crate-settings.resin-cost", 0);
        double textOffset = config.getDouble("crate-settings.text-offset", 1.2D);
        double itemOffset = config.getDouble("crate-settings.item-offset", 1.8D);

        // Terapkan Layout dari GUI.yml
        List<ItemUtils.GUIItem> allItems = new ArrayList<>();
        allItems.addAll(ItemUtils.loadGUIItems(guiConfig.getConfigurationSection("editor-settings.fillers")));
        allItems.addAll(ItemUtils.loadGUIItems(guiConfig.getConfigurationSection("editor-settings.items")));
        allItems.sort(java.util.Comparator.comparingInt(a -> a.priority));

        for (ItemUtils.GUIItem gItem : allItems) {
            ItemStack item = gItem.item.clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String name = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                
                // INJECT DYNAMIC DATA
                if (name.contains("Tipe Crate")) {
                    lore.add(0, "§7Saat ini: " + (isTemp ? "§cSementara" : "§aPermanen"));
                    lore.add("§7Klik untuk mengubah.");
                } else if (name.contains("Durasi")) {
                    lore.add("§7Saat ini: §f" + duration + "s");
                    lore.add("§7Klik untuk mengubah.");
                } else if (name.contains("Biaya Resin")) {
                    lore.add("§7Saat ini: §b" + resinCost + " 🔲");
                    lore.add(" "); lore.add("§7Klik untuk mengatur biaya resin"); lore.add("§7saat membuka crate.");
                } else if (name.contains("Hologram") && !name.contains("Tinggi")) {
                    lore.add("§7Status: " + (isHoloEnabled ? "§aAKTIF" : "§cMATI"));
                    lore.add(" "); lore.add("§7Klik untuk Mengaktifkan/Mematikan");
                } else if (name.contains("Tinggi Teks Hologram")) {
                    lore.add("§7Saat ini: §f" + textOffset);
                    lore.add("§7Klik untuk mengubah (misal: 1.2).");
                } else if (name.contains("Tinggi Item Melayang")) {
                    lore.add("§7Saat ini: §f" + itemOffset);
                    lore.add("§7Klik untuk mengubah (misal: 2.0).");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            for (int slot : gItem.slots) {
                if (slot < size) inv.setItem(slot, item.clone());
            }
        }
        player.openInventory(inv);
    }

    public static void openTierSelection(Player player, String crateId) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.TIER_SELECTION, crateId, null, -1);
        Inventory inv = Bukkit.createInventory(holder, 27, "Editor > ... > Tiers");
        holder.setInventory(inv);

        inv.setItem(11, createIcon(Material.NETHER_STAR, "§cTIER S"));
        inv.setItem(12, createIcon(Material.GOLD_INGOT, "§6TIER A"));
        inv.setItem(13, createIcon(Material.IRON_INGOT, "§eTIER B"));
        inv.setItem(14, createIcon(Material.COAL, "§bTIER C"));
        inv.setItem(15, createIcon(Material.DIRT, "§fTIER D"));
        inv.setItem(26, createIcon(Material.BARRIER, "§cKembali"));
        player.openInventory(inv);
    }

    public static void openRewardList(Player player, String crateId, String tierId) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.REWARD_LIST, crateId, tierId, -1);
        Inventory inv = Bukkit.createInventory(holder, 54, "Editor > ... > Rewards");
        holder.setInventory(inv);

        File file = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), crateId + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        org.bukkit.configuration.ConfigurationSection items = config.getConfigurationSection("tiers." + tierId);

        int slot = 9;
        if (items != null) {
            NamespacedKey idKey = new NamespacedKey(SeriaCrate.getInstance(), "gui_reward_id");

            for (String key : items.getKeys(false)) {
                if (slot >= 44) break;
                Reward reward = SeriaCrate.getInstance().getRewardManager().getRewardsFor(crateId, tierId).stream().skip(Integer.parseInt(key) - 1).findFirst().orElse(null);

                if (reward != null) {
                    ItemStack display = ItemUtils.buildRewardItem(reward);
                    ItemMeta m = display.getItemMeta();
                    m.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, key);

                    List<String> lore = m.getLore() == null ? new ArrayList<>() : m.getLore();
                    lore.add(" ");
                    lore.add("§8ID: " + key);
                    lore.add("§fWeight/Peluang: §e" + reward.getWeight());
                    lore.add("§fJumlah GUI: §a" + reward.getAmount());
                    lore.add(" ");
                    lore.add("§a[KLIK KIRI] §7Edit Detail");
                    lore.add("§c[SHIFT + KLIK KANAN] §7Hapus Reward");
                    m.setLore(lore);
                    display.setItemMeta(m);
                    inv.setItem(slot, display);
                    slot++;
                }
            }
        }

        inv.setItem(slot, createIcon(Material.LIME_STAINED_GLASS_PANE, "§a+ Tambah Reward", "§7Klik untuk membuat slot baru."));

        ItemStack filler = createIcon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        inv.setItem(46, createIcon(Material.REPEATER, "§eRefresh"));
        inv.setItem(49, createIcon(Material.FEATHER, "§fInfo"));
        inv.setItem(52, createIcon(Material.BARRIER, "§cKembali"));
        player.openInventory(inv);
    }

    public static void openRewardEdit(Player player, String crateId, String tierId, int index) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.REWARD_EDIT, crateId, tierId, index);
        Inventory inv = Bukkit.createInventory(holder, 54, "Editor > ... > Reward");
        holder.setInventory(inv);

        File file = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), crateId + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "tiers." + tierId + "." + index;

        Reward reward = SeriaCrate.getInstance().getRewardManager().getRewardsFor(crateId, tierId).stream().skip(index - 1).findFirst().orElse(null);
        if (reward != null && reward.getDisplayItem().getType() != Material.STONE) {
            inv.setItem(4, ItemUtils.buildRewardItem(reward)); 
        } else {
            inv.setItem(4, createIcon(Material.IRON_BARS, "§b§lTARIK ITEM KESINI", "§7Tarik item dari inventory-mu", "§7lalu letakkan di sini untuk", "§7Auto-Generate Config Bersih."));
        }

        int weight = config.getInt(path + ".weight", 1);
        int amount = config.getInt(path + ".amount", 1);
        boolean isBroadcastEnabled = config.getBoolean(path + ".broadcast", false);
        
        List<String> winItems = config.getStringList(path + ".win_items");
        List<String> commands = config.getStringList(path + ".commands");
        String cmdStr = config.getString(path + ".cmd", "");
        int totalCmds = commands.size() + (cmdStr.isEmpty() ? 0 : 1);

        inv.setItem(20, createIcon(Material.ORANGE_SHULKER_BOX, "§6Item Fisik Reward", "§7Berisi: §f" + winItems.size() + " Item", " ", "§7Klik untuk membuka menu drag-and-drop", "§7item yang akan masuk ke inventory pemain."));
        inv.setItem(21, createIcon(Material.COMMAND_BLOCK, "§cCommand Reward", "§7Berisi: §f" + totalCmds + " Command", " ", "§7Klik untuk menambah perintah yang akan", "§7dieksekusi console (Gunakan %player_name%)."));
        inv.setItem(22, createIcon(Material.GOAT_HORN, "§9Broadcast Message", "§7Status: " + (isBroadcastEnabled ? "§aAKTIF" : "§cMATI"), " ", "§7Klik untuk Mengaktifkan/Mematikan."));
        inv.setItem(23, createIcon(Material.GOLD_INGOT, "§ePeluang (Weight)", "§7Saat ini: §f" + weight, " ", "§7Klik untuk mengatur angka via chat."));
        inv.setItem(24, createIcon(Material.EMERALD, "§aJumlah (Amount)", "§7Saat ini: §f" + amount, " ", "§7Klik untuk mengatur angka via chat."));

        ItemStack filler = createIcon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        inv.setItem(49, createIcon(Material.FEATHER, "§fInfo"));
        inv.setItem(51, createIcon(Material.BARRIER, "§cKembali"));
        player.openInventory(inv);
    }

    public static void openRewardWinItemsMenu(Player player, String crateId, String tierId, int index) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.REWARD_WIN_ITEMS, crateId, tierId, index);
        Inventory inv = Bukkit.createInventory(holder, 54, "Edit > Fisik Reward");
        holder.setInventory(inv);

        File file = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), crateId + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> items = config.getStringList("tiers." + tierId + "." + index + ".win_items");

        for (int i = 0; i < items.size(); i++) {
            if (i >= 45) break;
            ItemStack item = ItemUtils.deserializeItemClean(items.get(i));
            if (item != null) {
                ItemMeta m = item.getItemMeta();
                List<String> lore = m.hasLore() ? m.getLore() : new ArrayList<>();
                lore.add(" "); lore.add("§8Data ID: " + i); lore.add("§c[SHIFT + KLIK KANAN] §7Hapus Item Ini");
                m.setLore(lore);
                m.getPersistentDataContainer().set(new NamespacedKey(SeriaCrate.getInstance(), "gui_list_index"), PersistentDataType.INTEGER, i);
                item.setItemMeta(m);
                inv.setItem(i, item);
            }
        }

        ItemStack filler = createIcon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        inv.setItem(49, createIcon(Material.LIME_DYE, "§a+ Tarik Item ke Menu Ini", "§7Tarik dari inventory-mu, letakkan", "§7di slot kosong area atas."));
        inv.setItem(51, createIcon(Material.BARRIER, "§cKembali"));
        player.openInventory(inv);
    }

    public static void openRewardCommandsMenu(Player player, String crateId, String tierId, int index) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.REWARD_COMMANDS, crateId, tierId, index);
        Inventory inv = Bukkit.createInventory(holder, 54, "Edit > Commands");
        holder.setInventory(inv);

        File file = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), crateId + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "tiers." + tierId + "." + index;
        
        List<String> cmds = new ArrayList<>();
        if (config.contains(path + ".cmd") && config.getString(path + ".cmd") != null) cmds.add(config.getString(path + ".cmd"));
        if (config.contains(path + ".commands")) cmds.addAll(config.getStringList(path + ".commands"));

        for (int i = 0; i < cmds.size(); i++) {
            if (i >= 45) break;
            ItemStack paper = createIcon(Material.NAME_TAG, "§eCommand #" + (i + 1), "§f/" + cmds.get(i), " ", "§c[KLIK KIRI] §7Hapus Command Ini");
            ItemMeta m = paper.getItemMeta();
            m.getPersistentDataContainer().set(new NamespacedKey(SeriaCrate.getInstance(), "gui_list_index"), PersistentDataType.INTEGER, i);
            paper.setItemMeta(m);
            inv.setItem(i, paper);
        }

        ItemStack filler = createIcon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        inv.setItem(48, createIcon(Material.LIME_STAINED_GLASS_PANE, "§aTambah Command"));
        inv.setItem(50, createIcon(Material.BARRIER, "§cKembali"));
        player.openInventory(inv);
    }

    public static void openBlockEditor(Player player, String crateId, int page) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.BLOCK_EDITOR, crateId, null, page);
        Inventory inv = Bukkit.createInventory(holder, 54, "Pilih Blok (Hal " + (page + 1) + ")");
        holder.setInventory(inv);

        int startIndex = page * 45;
        for (int i = 0; i < 45; i++) {
            int listIndex = startIndex + i;
            if (listIndex >= VALID_BLOCKS.size()) break; 

            Material mat = VALID_BLOCKS.get(listIndex);
            inv.setItem(i, createIcon(mat, "§e" + mat.name(), " ", "§a[KLIK] §7Jadikan wujud Crate"));
        }

        ItemStack filler = createIcon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        if (page > 0) {
            inv.setItem(45, createIcon(Material.ARROW, "§aHalaman Sebelumnya"));
        }
        if ((page + 1) * 45 < VALID_BLOCKS.size()) {
            inv.setItem(53, createIcon(Material.ARROW, "§aHalaman Berikutnya"));
        }

        inv.setItem(49, createIcon(Material.BARRIER, "§cKembali"));
        player.openInventory(inv);
    }
}