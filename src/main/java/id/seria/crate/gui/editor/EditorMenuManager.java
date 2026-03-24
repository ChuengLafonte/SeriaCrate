package id.seria.crate.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

    public static void openMainMenu(Player player) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.MAIN_MENU, null, null);
        Inventory inv = Bukkit.createInventory(holder, 54, "§8Crate Editor: Main Menu");
        holder.setInventory(inv);

        File folder = SeriaCrate.getInstance().getConfigManager().getRewardsFolder();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        int slot = 0;
        if (files != null) {
            for (File file : files) {
                String crateId = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                String blockName = config.getString("crate-settings.block", "ENDER_CHEST");
                Material mat = Material.matchMaterial(blockName);
                if (mat == null) mat = Material.ENDER_CHEST;

                inv.setItem(slot, createIcon(mat, "§6§l" + crateId.toUpperCase(), "§7Klik untuk mengedit Crate ini."));
                slot++;
            }
        }

        inv.setItem(53, createIcon(Material.EMERALD_BLOCK, "§a§l+ Buat Crate Baru", "§7Klik untuk membuat Crate via Chat."));
        player.openInventory(inv);
    }

    public static void openCrateSettings(Player player, String crateId) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.CRATE_SETTINGS, crateId, null);
        Inventory inv = Bukkit.createInventory(holder, 27, "§8Settings: " + crateId.toUpperCase());
        holder.setInventory(inv);

        inv.setItem(11, createIcon(Material.DIAMOND_SWORD, "§a§lEdit Rewards", "§7Atur isi hadiah Crate ini."));
        inv.setItem(13, createIcon(Material.GRASS_BLOCK, "§e§lUbah Blok Fisik", "§7Ganti tampilan Crate di dunia."));
        inv.setItem(15, createIcon(Material.PAPER, "§b§lEdit Hologram", "§7Ubah teks melayang di atas crate."));
        inv.setItem(17, createIcon(Material.CHEST_MINECART, "§d§lAmbil Crate Item", "§7Dapatkan blok fisik Crate ini."));
        inv.setItem(26, createIcon(Material.ARROW, "§c§lKembali", "§7Ke Menu Utama"));

        player.openInventory(inv);
    }

    public static void openTierSelection(Player player, String crateId) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.TIER_SELECTION, crateId, null);
        Inventory inv = Bukkit.createInventory(holder, 27, "§8Tiers: " + crateId.toUpperCase());
        holder.setInventory(inv);

        inv.setItem(11, createIcon(Material.NETHER_STAR, "§c§lTIER S"));
        inv.setItem(12, createIcon(Material.GOLD_INGOT, "§6§lTIER A"));
        inv.setItem(13, createIcon(Material.IRON_INGOT, "§e§lTIER B"));
        inv.setItem(14, createIcon(Material.COAL, "§b§lTIER C"));
        inv.setItem(15, createIcon(Material.DIRT, "§f§lTIER D"));
        inv.setItem(26, createIcon(Material.ARROW, "§c§lKembali", "§7Ke Pengaturan Crate"));

        player.openInventory(inv);
    }

    public static void openItemEditor(Player player, String crateId, String tierId) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.ITEM_EDITOR, crateId, tierId);
        Inventory inv = Bukkit.createInventory(holder, 54, "§8Editor: " + crateId.toUpperCase() + " | " + tierId.toUpperCase());
        holder.setInventory(inv);

        List<Reward> existingRewards = SeriaCrate.getInstance().getRewardManager().getRewardsFor(crateId, tierId);
        int slot = 0;
        for (Reward reward : existingRewards) {
            if (slot >= 45) break;
            inv.setItem(slot, ItemUtils.buildRewardItem(reward));
            slot++;
        }

        ItemStack filler = createIcon(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 53; i++) inv.setItem(i, filler);
        inv.setItem(53, createIcon(Material.ARROW, "§c§lSimpan & Kembali", "§7Klik ini untuk menyimpan."));

        player.openInventory(inv);
    }

    public static void openBlockEditor(Player player, String crateId) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.BLOCK_EDITOR, crateId, null);
        Inventory inv = Bukkit.createInventory(holder, 27, "§8Ubah Blok: " + crateId.toUpperCase());
        holder.setInventory(inv);

        ItemStack filler = createIcon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i != 13) inv.setItem(i, filler);
        }

        File file = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), crateId + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String currentBlock = config.getString("crate-settings.block", "ENDER_CHEST");
        Material mat = Material.matchMaterial(currentBlock);
        
        if (mat != null) inv.setItem(13, new ItemStack(mat));

        inv.setItem(26, createIcon(Material.ARROW, "§c§lSimpan & Kembali", "§7Taruh 1 blok di tengah."));
        player.openInventory(inv);
    }

    public static void openHologramEditor(Player player, String crateId) {
        EditorHolder holder = new EditorHolder(EditorHolder.MenuType.HOLOGRAM_EDITOR, crateId, null);
        Inventory inv = Bukkit.createInventory(holder, 54, "§8Hologram: " + crateId.toUpperCase());
        holder.setInventory(inv);

        FileConfiguration config = SeriaCrate.getInstance().getConfigManager().getHologram();
        List<String> lines = config.getStringList("holograms." + crateId);
        
        if (lines.isEmpty()) {
            lines = java.util.Arrays.asList("&e&l" + crateId.toUpperCase() + " CRATE", "&7Menghilang dalam: &c%timer%", "&fKlik Kiri: &aPreview", "&fKlik Kanan: &eBuka");
            config.set("holograms." + crateId, lines);
            SeriaCrate.getInstance().getConfigManager().saveHologram();
        }

        int slot = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (slot >= 45) break;
            inv.setItem(slot, createIcon(Material.PAPER, "§eBaris " + (i + 1), 
                "§f" + org.bukkit.ChatColor.translateAlternateColorCodes('&', lines.get(i)), " ", "§aKlik KIRI §7untuk Edit", "§cKlik KANAN §7untuk Hapus"
            ));
            slot++;
        }

        inv.setItem(49, createIcon(Material.EMERALD_BLOCK, "§a§l+ Tambah Baris", "§7Ketik teks baru via chat."));
        inv.setItem(45, createIcon(Material.ARROW, "§c§lKembali", "§7Ke pengaturan Crate."));
        player.openInventory(inv);
    }
}