package id.seria.crate.listener;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;

import id.seria.crate.SeriaCrate;
import id.seria.crate.manager.TemporaryCrateManager;
import id.seria.crate.model.Reward;

public class BlockListener implements Listener {

    private final SeriaCrate plugin;

    public BlockListener(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCrateInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        Location loc = event.getClickedBlock().getLocation();
        Player player = event.getPlayer();
        
        // 1. CEK APAKAH INI CRATE (Bisa Permanen atau Sementara)
        String bossName = plugin.getLocationManager().getCrateAt(loc); // Cek Permanen
        boolean isTemporary = false;
        
        if (bossName == null) {
            // Jika tidak ada di permanen, cek di sementara
            TemporaryCrateManager.ActiveCrate tempCrate = plugin.getTempCrateManager().getCrateAt(loc);
            if (tempCrate != null) {
                bossName = tempCrate.bossName;
                isTemporary = true;
            }
        }

        if (bossName == null) return; // Bukan crate, abaikan.

        event.setCancelled(true);

        // 2. LOGIKA KLIK KIRI (PREVIEW)
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            player.openInventory(id.seria.crate.gui.PreviewGUI.createPreview(bossName));
            return;
        }

        // 3. LOGIKA KLIK KANAN (BUKA GACHA)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            // Cek apakah sudah diklaim (Khusus Crate Sementara)
            if (isTemporary) {
                TemporaryCrateManager.ActiveCrate tempCrate = plugin.getTempCrateManager().getCrateAt(loc);
                if (tempCrate.claimedPlayers.contains(player.getUniqueId())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getConfig().getString("settings.prefix") + "&cKamu sudah mengambil hadiah!"));
                    return;
                }
                tempCrate.claimedPlayers.add(player.getUniqueId());
            }

            // AMBIL POOL HADIAH (Default ke Tier S atau buat logika gacha tier di sini)
            String tierToRoll = "s"; 
            List<Reward> pool = plugin.getRewardManager().getRewardsFor(bossName, tierToRoll);

            if (pool.isEmpty()) {
                player.sendMessage("§cHadiah untuk crate ini belum diatur di tier " + tierToRoll);
                return;
            }

            // BUKA GUI DAN JALANKAN ROLLING
            Inventory inv = id.seria.crate.gui.CrateGUI.createOpeningGUI(bossName, tierToRoll);
            player.openInventory(inv);
            
            // PENTING: Gunakan plugin instance agar RollingEngine bisa berjalan
            new id.seria.crate.engine.RollingEngine(plugin).startRolling(player, inv, pool);
        }
    }
    // FUNGSI BARU: Saat admin meletakkan Crate Item di lantai
    @EventHandler
    public void onCratePlace(org.bukkit.event.block.BlockPlaceEvent event) {
        org.bukkit.inventory.ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;

        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "crate_id");
        if (item.getItemMeta().getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            String boss = item.getItemMeta().getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);

            // Langsung daftarkan sebagai crate permanen!
            plugin.getLocationManager().setCrateLocation(event.getBlock().getLocation(), boss);
            event.getPlayer().sendMessage("§a[SeriaCrate] Crate permanen " + boss.toUpperCase() + " berhasil diletakkan!");
        }
    }

    // FUNGSI BARU: Saat admin menghancurkan Crate untuk menghapusnya
    @EventHandler
    public void onCrateBreak(org.bukkit.event.block.BlockBreakEvent event) {
        String bossName = plugin.getLocationManager().getCrateAt(event.getBlock().getLocation());
        if (bossName != null) {
            // Jika pemain adalah admin dan sedang SNEAK (Jongkok), hapus crate
            if (event.getPlayer().hasPermission("seriacrate.admin") && event.getPlayer().isSneaking()) {
                plugin.getLocationManager().removeCrateLocation(event.getBlock().getLocation());
                event.getPlayer().sendMessage("§c[SeriaCrate] Crate " + bossName.toUpperCase() + " berhasil dihapus dari dunia!");
            } else {
                // Cegah pemain biasa menghancurkan Crate
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cIni adalah Crate! (Admin: Tahan SHIFT + Hancurkan untuk menghapus)");
            }
        }
    }
}