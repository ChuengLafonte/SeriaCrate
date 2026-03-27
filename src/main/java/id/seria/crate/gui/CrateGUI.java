package id.seria.crate.gui;

import id.seria.crate.SeriaCrate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CrateGUI {
   public static Inventory createOpeningGUI(String bossName, String tier) {
      String title = ChatColor.translateAlternateColorCodes('&', SeriaCrate.getInstance().getConfig().getString("GUI.TitleFormat").replace("%tier%", tier.toUpperCase()).replace("%boss%", bossName.toUpperCase()));
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, title);
      ItemStack frame = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
      ItemMeta frameMeta = frame.getItemMeta();
      if (frameMeta != null) {
         frameMeta.setDisplayName(" ");
         frame.setItemMeta(frameMeta);
      }

      for(int i = 0; i < 27; ++i) {
         inv.setItem(i, frame);
      }

      ItemStack pointer = new ItemStack(Material.HOPPER);
      ItemMeta pointerMeta = pointer.getItemMeta();
      if (pointerMeta != null) {
         String var10001 = String.valueOf(ChatColor.YELLOW);
         pointerMeta.setDisplayName(var10001 + "▼ " + String.valueOf(ChatColor.GOLD) + "ROLLING " + String.valueOf(ChatColor.YELLOW) + "▼");
         pointer.setItemMeta(pointerMeta);
      }

      inv.setItem(4, pointer);
      inv.setItem(22, pointer);
      return inv;
   }
}
