package id.seria.crate.gui;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PreviewGUI {
   public static Inventory createPreview(String bossName) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§8Preview: " + bossName.toUpperCase() + " Crate");
      String[] tiers = new String[]{"s", "a", "b", "c", "d"};
      int slot = 0;
      String[] var4 = tiers;
      int var5 = tiers.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         String tier = var4[var6];
         List<Reward> rewards = SeriaCrate.getInstance().getRewardManager().getRewardsFor(bossName, tier);
         if (!rewards.isEmpty()) {
            int totalWeight = rewards.stream().mapToInt(Reward::getWeight).sum();

            for(Iterator var10 = rewards.iterator(); var10.hasNext(); ++slot) {
               Reward reward = (Reward)var10.next();
               if (slot >= 54) {
                  break;
               }

               ItemStack item = ItemUtils.buildRewardItem(reward);
               ItemMeta meta = item.getItemMeta();
               if (meta != null) {
                  List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList();
                  ((List)lore).add(" ");
                  double chance = (double)reward.getWeight() / (double)totalWeight * 100.0D;
                  String formattedChance = String.format("%.2f", chance);
                  String tierColor = getTierColor(tier);
                  ((List)lore).add("§7Tier: " + tierColor + "Tier " + tier.toUpperCase());
                  ((List)lore).add("§7Peluang di Tier ini: §a" + formattedChance + "%");
                  meta.setLore((List)lore);
                  item.setItemMeta(meta);
               }

               inv.setItem(slot, item);
            }
         }
      }

      return inv;
   }

   private static String getTierColor(String tier) {
      String var1 = tier.toLowerCase();
      byte var2 = -1;
      switch(var1.hashCode()) {
      case 97:
         if (var1.equals("a")) {
            var2 = 1;
         }
         break;
      case 98:
         if (var1.equals("b")) {
            var2 = 2;
         }
         break;
      case 99:
         if (var1.equals("c")) {
            var2 = 3;
         }
         break;
      case 115:
         if (var1.equals("s")) {
            var2 = 0;
         }
      }

      switch(var2) {
      case 0:
         return "§c§l";
      case 1:
         return "§6§l";
      case 2:
         return "§e§l";
      case 3:
         return "§b§l";
      default:
         return "§f§l";
      }
   }
}
