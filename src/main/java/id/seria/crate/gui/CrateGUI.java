package id.seria.crate.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.seria.crate.SeriaCrate;
import id.seria.crate.util.TextUtils;
import net.kyori.adventure.text.Component;

public class CrateGUI {

    public static Inventory createOpeningGUI(String bossName, String tier) {
        FileConfiguration guiConfig = SeriaCrate.getInstance().getConfigManager().getGui();

        // Judul Component (Support MiniMessage & HEX)
        Component title = TextUtils.format(guiConfig.getString("rolling-gui.title", "<gray>Reward %boss% - %tier%")
                .replace("%tier%", tier.toUpperCase())
                .replace("%boss%", bossName.toUpperCase()));
        
        int size = guiConfig.getInt("rolling-gui.size", 27);
        // Paper API memungkinkan penggunaan Component sebagai judul
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Filler
        String fillerMatStr = guiConfig.getString("rolling-gui.filler.material", "BLACK_STAINED_GLASS_PANE");
        Material fillerMat = Material.matchMaterial(fillerMatStr);
        ItemStack filler = new ItemStack(fillerMat != null ? fillerMat : Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(TextUtils.format(guiConfig.getString("rolling-gui.filler.name", " ")));
            fillerMeta.lore(TextUtils.formatList(guiConfig.getStringList("rolling-gui.filler.lore")));
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

        // Pointer
        String pointerMatStr = guiConfig.getString("rolling-gui.pointer.material", "HOPPER");
        Material pointerMat = Material.matchMaterial(pointerMatStr);
        ItemStack pointer = new ItemStack(pointerMat != null ? pointerMat : Material.HOPPER);
        ItemMeta pointerMeta = pointer.getItemMeta();
        if (pointerMeta != null) {
            pointerMeta.displayName(TextUtils.format(guiConfig.getString("rolling-gui.pointer.name", "<yellow>▼ <gold>ROLLING <yellow>▼")));
            pointerMeta.lore(TextUtils.formatList(guiConfig.getStringList("rolling-gui.pointer.lore")));
            pointer.setItemMeta(pointerMeta);
        }

        for (int pointerSlot : guiConfig.getIntegerList("rolling-gui.pointer.slots")) {
            if (pointerSlot < size) inv.setItem(pointerSlot, pointer);
        }

        return inv;
    }
}