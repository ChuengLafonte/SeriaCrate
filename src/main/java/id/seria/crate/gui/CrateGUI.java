package id.seria.crate.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.seria.crate.SeriaCrate;

public class CrateGUI {

    public static Inventory createOpeningGUI(String bossName, String tier) {
        FileConfiguration guiConfig = SeriaCrate.getInstance().getConfigManager().getGui();

        // 1. Ambil Judul & Ukuran
        String titleRaw = guiConfig.getString("rolling-gui.title", "&8Reward Crate %boss% - Tier %tier%");
        String title = ChatColor.translateAlternateColorCodes('&', titleRaw
                .replace("%tier%", tier.toUpperCase())
                .replace("%boss%", bossName.toUpperCase()));
        
        int size = guiConfig.getInt("rolling-gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        // 2. Setup Filler (Kaca)
        String fillerMatStr = guiConfig.getString("rolling-gui.filler.material", "BLACK_STAINED_GLASS_PANE");
        Material fillerMat = Material.matchMaterial(fillerMatStr);
        if (fillerMat == null) fillerMat = Material.BLACK_STAINED_GLASS_PANE;

        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', guiConfig.getString("rolling-gui.filler.name", " ")));
            List<String> lore = new ArrayList<>();
            for (String l : guiConfig.getStringList("rolling-gui.filler.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            fillerMeta.setLore(lore);
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

        // 3. Setup Pointer (Hopper / Panah)
        String pointerMatStr = guiConfig.getString("rolling-gui.pointer.material", "HOPPER");
        Material pointerMat = Material.matchMaterial(pointerMatStr);
        if (pointerMat == null) pointerMat = Material.HOPPER;

        ItemStack pointer = new ItemStack(pointerMat);
        ItemMeta pointerMeta = pointer.getItemMeta();
        if (pointerMeta != null) {
            pointerMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', guiConfig.getString("rolling-gui.pointer.name", "&e▼ &6ROLLING &e▼")));
            List<String> lore = new ArrayList<>();
            for (String l : guiConfig.getStringList("rolling-gui.pointer.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            pointerMeta.setLore(lore);
            pointer.setItemMeta(pointerMeta);
        }

        // Letakkan Pointer sesuai dengan slot yang diatur di gui.yml
        List<Integer> pointerSlots = guiConfig.getIntegerList("rolling-gui.pointer.slots");
        if (pointerSlots.isEmpty()) {
            pointerSlots.add(4);
            pointerSlots.add(22);
        }

        for (int pointerSlot : pointerSlots) {
            if (pointerSlot < size) {
                inv.setItem(pointerSlot, pointer);
            }
        }

        return inv;
    }
}