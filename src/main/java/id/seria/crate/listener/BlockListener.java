package id.seria.crate.listener;

import java.io.File;
import java.util.List;

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
                        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + "&cKamu sudah mengambil hadiah di crate ini!"));
                        return;
                     }
                  }

                  java.io.File file = new java.io.File(this.plugin.getConfigManager().getRewardsFolder(), bossName + ".yml");
                  org.bukkit.configuration.file.FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);

                  // 1. PENGURANGAN RESIN SEBELUM GACHA
                  int resinCost = config.getInt("crate-settings.resin-cost", 0);
                  if (resinCost > 0) {
                      if (!this.plugin.getResinManager().hasResin(player.getUniqueId(), resinCost)) {
                          int currentResin = this.plugin.getResinManager().getResin(player.getUniqueId());
                          String prefix = this.plugin.getConfigManager().getConfig().getString("settings.prefix", "&8[&eSeriaCrate&8] ");
                          player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + "&cResin tidak cukup! Butuh &6" + resinCost + " 🔲 &c(Milikmu: &6" + currentResin + "&c)"));
                          return; // Tolak pemain membuka crate!
                      }
                  }

                  // 2. GACHA TIER
                  String tierToRoll = "d"; 
                  double chanceS = config.getDouble("crate-settings.tier-chance.s", 5.0);  
                  double chanceA = config.getDouble("crate-settings.tier-chance.a", 15.0); 
                  double chanceB = config.getDouble("crate-settings.tier-chance.b", 25.0); 
                  double chanceC = config.getDouble("crate-settings.tier-chance.c", 25.0); 
                  double chanceD = config.getDouble("crate-settings.tier-chance.d", 30.0); 

                  double totalWeight = chanceS + chanceA + chanceB + chanceC + chanceD;
                  double randomVal = Math.random() * totalWeight;

                  if (randomVal < chanceS) tierToRoll = "s";
                  else if (randomVal < chanceS + chanceA) tierToRoll = "a";
                  else if (randomVal < chanceS + chanceA + chanceB) tierToRoll = "b";
                  else if (randomVal < chanceS + chanceA + chanceB + chanceC) tierToRoll = "c";
                  else tierToRoll = "d";

                  List<Reward> pool = this.plugin.getRewardManager().getRewardsFor(bossName, tierToRoll);
                  if (pool == null || pool.isEmpty()) {
                     player.sendMessage("§cHadiah untuk crate ini belum diatur di tier " + tierToRoll.toUpperCase());
                     return; 
                  }

                  // 3. PROSES PEMBUKAAN (INSTANT & ANIMASI)
                  try {
                      id.seria.crate.engine.RollingEngine engine = new id.seria.crate.engine.RollingEngine(this.plugin);
                      
                      if (player.isSneaking()) {
                          // BUKA INSTAN SHIFT KLIK
                          if (resinCost > 0) {
                              this.plugin.getResinManager().consumeResin(player.getUniqueId(), resinCost);
                              player.sendMessage("§c-" + resinCost + " 🔲 Resin");
                          }
                          
                          Reward instantWin = engine.getRandomWeighted(pool);
                          engine.giveReward(player, instantWin, bossName, tierToRoll);
                          player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                          
                          if (isTemporary) tempCrate.claimedPlayers.add(player.getUniqueId());
                          return;
                      }
                      
                      // BUKA NORMAL (ANIMASI)
                      Inventory inv = id.seria.crate.gui.CrateGUI.createOpeningGUI(bossName, tierToRoll);
                      player.openInventory(inv);
                      
                      if (resinCost > 0) {
                          this.plugin.getResinManager().consumeResin(player.getUniqueId(), resinCost);
                          player.sendMessage("§c-" + resinCost + " 🔲 Resin");
                      }
                      
                      engine.startRolling(player, inv, pool, bossName, tierToRoll);
                      if (isTemporary) tempCrate.claimedPlayers.add(player.getUniqueId());
                  } catch (Exception e) {
                      player.sendMessage("§cTerjadi kesalahan saat membuka Crate!");
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