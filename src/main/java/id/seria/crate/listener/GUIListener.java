package id.seria.crate.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import id.seria.crate.SeriaCrate;

public class GUIListener implements Listener {

    private final SeriaCrate plugin;
    // Set untuk mencatat pemain yang sedang dalam proses rolling
    private final Set<UUID> isRolling = new HashSet<>();

    public GUIListener(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        String configTitle = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("GUI.TitleFormat"));

        // Proteksi: Jika judul inventory mengandung format title dari config
        // Kita gunakan .contains() karena title aslinya punya variabel %tier% dll
        if (title.contains("Reward Crate") || title.contains("Chest Reward")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        // Logika anti-scam: Jika pemain menutup inventory saat rolling,
        // kita akan memberikan hadiah terakhir yang terhitung di Fase 6 nanti.
        if (isRolling.contains(uuid)) {
            // (Akan diisi logika forced-reward di Fase 6)
            isRolling.remove(uuid);
        }
    }

    public Set<UUID> getRollingPlayers() {
        return isRolling;
    }
}