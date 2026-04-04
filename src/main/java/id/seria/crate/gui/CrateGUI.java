package id.seria.crate.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;

import id.seria.crate.SeriaCrate;
import id.seria.crate.util.ItemUtils;
import id.seria.crate.util.TextUtils;
import net.kyori.adventure.text.Component;

public class CrateGUI {

    public static Inventory createOpeningGUI(String bossName, String tier) {
        FileConfiguration guiConfig = SeriaCrate.getInstance().getConfigManager().getGui();

        // 1. PENANGANAN JUDUL DINAMIS (Bug Fix)
        // Jika parameter tier adalah "?", ubah teks menjadi "Rolling Tier..."
        String titleRaw = guiConfig.getString("rolling-gui.title", "&8| Reward Crate %boss% - Tier %tier%");
        if (tier.equals("?")) {
            // Me-replace bagian "Tier %tier%" agar rapi saat rolling awal
            titleRaw = titleRaw.replace("Tier %tier%", "Rolling Tier...").replace("%tier%", "Rolling...");
        } else {
            titleRaw = titleRaw.replace("%tier%", tier.toUpperCase());
        }
        titleRaw = titleRaw.replace("%boss%", bossName.toUpperCase());
        
        // Menggunakan TextUtils Component bawaan format seriacrate milikmu
        Component title = TextUtils.format(titleRaw);

        int size = guiConfig.getInt("rolling-gui.size", 27);
        if (size < 9 || size % 9 != 0) size = 27;

        Inventory inv = Bukkit.createInventory(null, size, title);

        // 2. LOAD FILLER & POINTER DARI GUI.YML (Ultimate GUI System)
        List<ItemUtils.GUIItem> allItems = new ArrayList<>();
        allItems.addAll(ItemUtils.loadGUIItems(guiConfig.getConfigurationSection("rolling-gui.fillers")));
        
        // Urutkan dari priority terkecil ke terbesar
        allItems.sort(java.util.Comparator.comparingInt(a -> a.priority));

        // Ambil daftar slot yang akan digunakan untuk rolling animasi
        List<Integer> rollingSlots = guiConfig.getIntegerList("rolling-gui.rolling-slots");
        if (rollingSlots.isEmpty()) {
            for (int i = 9; i <= 17; i++) rollingSlots.add(i);
        }

        // 3. TERAPKAN ITEM KE DALAM INVENTORY
        for (ItemUtils.GUIItem gItem : allItems) {
            for (int slot : gItem.slots) {
                // Pastikan filler/pointer tidak menabrak slot tempat item bergulir
                if (slot < size && !rollingSlots.contains(slot)) {
                    inv.setItem(slot, gItem.item.clone());
                }
            }
        }

        return inv;
    }
}