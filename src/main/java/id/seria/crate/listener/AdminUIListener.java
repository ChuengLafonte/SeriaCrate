package id.seria.crate.listener;

import id.seria.crate.SeriaCrate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class AdminUIListener implements Listener {
   private final SeriaCrate plugin;

   public AdminUIListener(SeriaCrate plugin) {
      this.plugin = plugin;
   }

   public static void openEditor(Player player, String boss, String tier) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§cEditor: " + boss + " | " + tier);
      player.openInventory(inv);
   }

   @EventHandler
   public void onEditorClose(InventoryCloseEvent event) {
      String title = event.getView().getTitle();
      if (title.startsWith("§cEditor: ")) {
         Player player = (Player)event.getPlayer();
         String raw = ChatColor.stripColor(title).replace("Editor: ", "");
         String[] split = raw.split(" \\| ");
         String boss = split[0].toLowerCase();
         String tier = split[1].toLowerCase();
         String var10001 = boss.toUpperCase();
         player.sendMessage("§e[SeriaCrate] Menyimpan perubahan untuk " + var10001 + " Tier " + tier.toUpperCase() + "...");
         Inventory inv = event.getInventory();
         int slotIndex = 1;
         ItemStack[] var10 = inv.getContents();
         int var11 = var10.length;

         for(int var12 = 0; var12 < var11; ++var12) {
            ItemStack item = var10[var12];
            if (item != null && !item.getType().isAir()) {
               player.sendMessage("§7- Mendeteksi item di slot " + slotIndex);
               ++slotIndex;
            }
         }

         player.sendMessage("§aPenyimpanan selesai! (Logika Write YAML menyusul)");
      }

   }
}
