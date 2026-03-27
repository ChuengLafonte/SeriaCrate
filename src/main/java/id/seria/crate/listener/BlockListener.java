package id.seria.crate.listener;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
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
            TemporaryCrateManager.ActiveCrate tempCrate;
            if (bossName == null) {
               tempCrate = this.plugin.getTempCrateManager().getCrateAt(loc);
               if (tempCrate != null) {
                  bossName = tempCrate.bossName;
                  isTemporary = true;
               }
            }

            if (bossName != null) {
               event.setCancelled(true);
               if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                  player.openInventory(PreviewGUI.createPreview(bossName));
               } else {
                  if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                     if (isTemporary) {
                        tempCrate = this.plugin.getTempCrateManager().getCrateAt(loc);
                        if (tempCrate.claimedPlayers.contains(player.getUniqueId())) {
                           FileConfiguration var10002 = this.plugin.getConfigManager().getConfig();
                           player.sendMessage(ChatColor.translateAlternateColorCodes('&', var10002.getString("settings.prefix") + "&cKamu sudah mengambil hadiah!"));
                           return;
                        }

                        tempCrate.claimedPlayers.add(player.getUniqueId());
                     }

                     String tierToRoll = "s";
                     List<Reward> pool = this.plugin.getRewardManager().getRewardsFor(bossName, tierToRoll);
                     if (pool.isEmpty()) {
                        player.sendMessage("§cHadiah untuk crate ini belum diatur di tier " + tierToRoll);
                        return;
                     }

                     Inventory inv = CrateGUI.createOpeningGUI(bossName, tierToRoll);
                     player.openInventory(inv);
                     (new RollingEngine(this.plugin)).startRolling(player, inv, pool);
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
            String boss = (String)item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            this.plugin.getLocationManager().setCrateLocation(event.getBlock().getLocation(), boss);
            event.getPlayer().sendMessage("§a[SeriaCrate] Crate permanen " + boss.toUpperCase() + " berhasil diletakkan!");
         }

      }
   }

   @EventHandler
   public void onCrateBreak(BlockBreakEvent event) {
      String bossName = this.plugin.getLocationManager().getCrateAt(event.getBlock().getLocation());
      if (bossName != null) {
         if (event.getPlayer().hasPermission("seriacrate.admin") && event.getPlayer().isSneaking()) {
            this.plugin.getLocationManager().removeCrateLocation(event.getBlock().getLocation());
            event.getPlayer().sendMessage("§c[SeriaCrate] Crate " + bossName.toUpperCase() + " berhasil dihapus dari dunia!");
         } else {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cIni adalah Crate! (Admin: Tahan SHIFT + Hancurkan untuk menghapus)");
         }
      }

   }
}
