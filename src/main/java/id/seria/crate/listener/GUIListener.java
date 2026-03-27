package id.seria.crate.listener;

import id.seria.crate.SeriaCrate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GUIListener implements Listener {
   private final SeriaCrate plugin;
   private final Set<UUID> isRolling = new HashSet();

   public GUIListener(SeriaCrate plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      String title = event.getView().getTitle();
      String configTitle = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("GUI.TitleFormat"));
      if (title.contains("Reward Crate") || title.contains("Chest Reward")) {
         event.setCancelled(true);
      }

   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      UUID uuid = event.getPlayer().getUniqueId();
      if (this.isRolling.contains(uuid)) {
         this.isRolling.remove(uuid);
      }

   }

   public Set<UUID> getRollingPlayers() {
      return this.isRolling;
   }
}
