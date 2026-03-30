package id.seria.crate.listener;

import java.io.File;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import id.seria.crate.SeriaCrate;
import id.seria.crate.engine.RollingEngine;
import id.seria.crate.gui.CrateGUI;
import id.seria.crate.gui.PreviewGUI;
import id.seria.crate.manager.TemporaryCrateManager;
import id.seria.crate.model.Reward;

public class BlockListener implements Listener {
   private final SeriaCrate plugin;

   public BlockListener(SeriaCrate plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onCrateInteract(PlayerInteractEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         if (event.getClickedBlock() != null) {
            Location loc = event.getClickedBlock().getLocation();
            Player player = event.getPlayer();
            String bossName = this.plugin.getLocationManager().getCrateAt(loc);
            boolean isTemporary = false;
            TemporaryCrateManager.ActiveCrate tempCrate = null;

            if (bossName == null) {
               tempCrate = this.plugin.getTempCrateManager().getCrateAt(loc);
               if (tempCrate != null) {
                  bossName = tempCrate.bossName;
                  isTemporary = true;
               }
            }

            if (bossName != null) {
               event.setCancelled(true); // Batalkan interaksi Vanilla (mencegah chest terbuka biasa)

               // ===========================================
               // [FITUR BARU] SHIFT + KLIK KIRI UNTUK ADMIN MENGHAPUS
               // ===========================================
               if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                  if (player.isSneaking() && (player.isOp() || player.hasPermission("seriacrate.admin"))) {
                     if (isTemporary) {
                        tempCrate.timeLeft = 0; // Memaksa updater menghancurkan Crate Sementara
                     } else {
                        this.plugin.getLocationManager().removeCrateLocation(loc); // Hapus Crate Permanen
                     }
                     loc.getBlock().setType(Material.AIR);
                     player.sendMessage("§c[SeriaCrate] Crate " + bossName.toUpperCase() + " berhasil dihancurkan paksa!");
                     return;
                  }
                  // Jika bukan admin/tidak shift, buka Preview
                  player.openInventory(PreviewGUI.createPreview(bossName));
               } 
               // ===========================================
               // KLIK KANAN UNTUK ROLL HADIAH
               // ===========================================
               else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                  if (isTemporary) {
                     if (tempCrate.claimedPlayers.contains(player.getUniqueId())) {
                        String prefix = this.plugin.getConfigManager().getConfig().getString("settings.prefix", "&8[&eSeriaCrate&8] ");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cKamu sudah mengambil hadiah di crate ini!"));
                        return;
                     }
                     // PENAMBAHAN CLAIMED PLAYERS DIHAPUS DARI SINI
                  }

                  // PERHATIAN: Saat ini tier di-hardcode ke "s". Pastikan di config rewards milikmu ada tier "s"
                  String tierToRoll = "s"; 
                  List<Reward> pool = this.plugin.getRewardManager().getRewardsFor(bossName, tierToRoll);
                  
                  if (pool == null || pool.isEmpty()) {
                     player.sendMessage("§cHadiah untuk crate ini belum diatur di tier " + tierToRoll.toUpperCase());
                     return; // Berhenti tanpa mencatat pemain ke daftar klaim
                  }

                  try {
                      Inventory inv = CrateGUI.createOpeningGUI(bossName, tierToRoll);
                      player.openInventory(inv);
                      new RollingEngine(this.plugin).startRolling(player, inv, pool);
                      
                      // BARU DICATAT SETELAH GUI BERHASIL TERBUKA DAN ROLLING DIMULAI
                      if (isTemporary) {
                         tempCrate.claimedPlayers.add(player.getUniqueId());
                      }
                  } catch (Exception e) {
                      player.sendMessage("§cTerjadi kesalahan saat membuka Crate! Silakan lapor ke Admin.");
                      e.printStackTrace();
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onCratePlace(BlockPlaceEvent event) {
      ItemStack item = event.getItemInHand();
      if (item != null && item.hasItemMeta()) {
         NamespacedKey key = new NamespacedKey(this.plugin, "crate_id");
         if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            String boss = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            
            // ===========================================
            // [PERBAIKAN] CEK APAKAH INI TEMPORARY/PERMANEN DARI CONFIG
            // ===========================================
            File file = new File(plugin.getConfigManager().getRewardsFolder(), boss + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            boolean isTemp = config.getBoolean("crate-settings.is-temporary", false);
            
            if (isTemp) {
               this.plugin.getTempCrateManager().spawnTemporaryCrate(event.getBlock().getLocation(), boss);
               event.getPlayer().sendMessage("§a[SeriaCrate] Crate SEMENTARA " + boss.toUpperCase() + " berhasil diletakkan!");
            } else {
               this.plugin.getLocationManager().setCrateLocation(event.getBlock().getLocation(), boss);
               event.getPlayer().sendMessage("§a[SeriaCrate] Crate PERMANEN " + boss.toUpperCase() + " berhasil diletakkan!");
            }
         }
      }
   }

   @EventHandler
   public void onCrateBreak(BlockBreakEvent event) {
      String bossName = this.plugin.getLocationManager().getCrateAt(event.getBlock().getLocation());
      TemporaryCrateManager.ActiveCrate temp = this.plugin.getTempCrateManager().getCrateAt(event.getBlock().getLocation());
      
      if (bossName != null || temp != null) {
         if (event.getPlayer().hasPermission("seriacrate.admin") && event.getPlayer().isSneaking()) {
            if (bossName != null) this.plugin.getLocationManager().removeCrateLocation(event.getBlock().getLocation());
            if (temp != null) temp.timeLeft = 0;
            event.getPlayer().sendMessage("§c[SeriaCrate] Crate berhasil dihapus dari dunia!");
         } else {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cIni adalah Crate! (Admin: Tahan SHIFT + Klik Kiri untuk menghancurkan)");
         }
      }
   }
}