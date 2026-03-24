package id.seria.crate.gui;

import id.seria.crate.SeriaCrate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CrateGUI {

    public static Inventory createOpeningGUI(String bossName, String tier) {
        String title = ChatColor.translateAlternateColorCodes('&', 
            SeriaCrate.getInstance().getConfig().getString("GUI.TitleFormat")
                .replace("%tier%", tier.toUpperCase())
                .replace("%boss%", bossName.toUpperCase()));
        
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // 1. Isi Frame dengan Black Stained Glass Pane
        ItemStack frame = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta frameMeta = frame.getItemMeta();
        if (frameMeta != null) {
            frameMeta.setDisplayName(" ");
            frame.setItemMeta(frameMeta);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, frame);
        }

        // 2. Pasang Penunjuk (Hopper) di slot 4 (atas) dan 22 (bawah)
        ItemStack pointer = new ItemStack(Material.HOPPER);
        ItemMeta pointerMeta = pointer.getItemMeta();
        if (pointerMeta != null) {
            pointerMeta.setDisplayName(ChatColor.YELLOW + "▼ " + ChatColor.GOLD + "ROLLING " + ChatColor.YELLOW + "▼");
            pointer.setItemMeta(pointerMeta);
        }
        inv.setItem(4, pointer);
        inv.setItem(22, pointer);

        return inv;
    }
}