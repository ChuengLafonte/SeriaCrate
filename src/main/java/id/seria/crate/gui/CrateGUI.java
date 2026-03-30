package id.seria.crate.gui;

import java.util.List;

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

    /**
     * Buat GUI Rolling Crate.
     *
     * Layout 3 baris (27 slot), sesuai logika Skript:
     *
     *  [ 0][ 1][ 2][ 3][ 4][ 5][ 6][ 7][ 8]   ← baris 1 (filler)
     *  [ 9][10][11][12][13][14][15][16][17]   ← baris 2 (rolling items)
     *  [18][19][20][21][22][23][24][25][26]   ← baris 3 (filler)
     *
     *  Pointer (Hopper ▼ ROLLING ▼) : slot 4 (atas-tengah) & slot 22 (bawah-tengah)
     *  Rolling items                 : slot 9-17 (baris tengah)
     *  Item pemenang berhenti di     : slot 13 (tengah baris rolling)
     */
    public static Inventory createOpeningGUI(String bossName, String tier) {
        FileConfiguration guiConfig = SeriaCrate.getInstance().getConfigManager().getGui();

        // Judul GUI
        String titleRaw = guiConfig.getString("rolling-gui.title", "&8Reward Crate %boss% - Tier %tier%")
                .replace("%tier%", tier.toUpperCase())
                .replace("%boss%", bossName.toUpperCase());
        Component title = TextUtils.format(titleRaw);

        int size = guiConfig.getInt("rolling-gui.size", 27);
        if (size < 9 || size % 9 != 0) size = 27;

        Inventory inv = Bukkit.createInventory(null, size, title);

        // ── Filler (semua slot diisi dulu) ──────────────────────────────
        String fillerMatStr = guiConfig.getString("rolling-gui.filler.material", "BLACK_STAINED_GLASS_PANE");
        Material fillerMat = Material.matchMaterial(fillerMatStr);
        ItemStack filler = new ItemStack(fillerMat != null ? fillerMat : Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            String fillerName = guiConfig.getString("rolling-gui.filler.name", " ");
            fillerMeta.displayName(TextUtils.format(fillerName.isEmpty() ? " " : fillerName));
            List<String> fillerLore = guiConfig.getStringList("rolling-gui.filler.lore");
            if (!fillerLore.isEmpty()) fillerMeta.lore(TextUtils.formatList(fillerLore));
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

        // ── Pointer (Hopper ▼ ROLLING ▼) ────────────────────────────────
        // Slot pointer: 4 (baris atas, tengah) dan 22 (baris bawah, tengah)
        String pointerMatStr = guiConfig.getString("rolling-gui.pointer.material", "HOPPER");
        Material pointerMat = Material.matchMaterial(pointerMatStr);
        ItemStack pointer = new ItemStack(pointerMat != null ? pointerMat : Material.HOPPER);
        ItemMeta pointerMeta = pointer.getItemMeta();
        if (pointerMeta != null) {
            String pointerName = guiConfig.getString("rolling-gui.pointer.name", "&e▼ &6ROLLING &e▼");
            pointerMeta.displayName(TextUtils.format(pointerName));
            List<String> pointerLore = guiConfig.getStringList("rolling-gui.pointer.lore");
            if (!pointerLore.isEmpty()) pointerMeta.lore(TextUtils.formatList(pointerLore));
            pointer.setItemMeta(pointerMeta);
        }

        List<Integer> pointerSlots = guiConfig.getIntegerList("rolling-gui.pointer.slots");
        if (pointerSlots.isEmpty()) {
            // Default: slot 4 (atas) dan 22 (bawah) sesuai logika Skript
            pointerSlots = java.util.Arrays.asList(4, 22);
        }
        for (int slot : pointerSlots) {
            if (slot >= 0 && slot < size) inv.setItem(slot, pointer);
        }

        // Slot rolling (baris tengah) dibiarkan kosong dulu —
        // RollingEngine akan mengisinya saat animasi dimulai
        List<Integer> rollingSlots = guiConfig.getIntegerList("rolling-gui.rolling-slots");
        if (rollingSlots.isEmpty()) {
            for (int i = 9; i <= 17; i++) rollingSlots.add(i);
        }
        for (int slot : rollingSlots) {
            if (slot >= 0 && slot < size) inv.setItem(slot, null);
        }

        return inv;
    }
}
