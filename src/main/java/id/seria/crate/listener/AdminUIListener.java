package id.seria.crate.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.seria.crate.SeriaCrate;

public class AdminUIListener implements Listener {

    private final SeriaCrate plugin;

    public AdminUIListener(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    // Fungsi untuk membuka GUI Editor
    public static void openEditor(Player player, String boss, String tier) {
        // Judul ini SANGAT PENTING sebagai penanda untuk dibaca saat ditutup
        Inventory inv = Bukkit.createInventory(null, 54, "§cEditor: " + boss + " | " + tier);
        
        // TODO: Muat item yang sudah ada di rewards.yml ke dalam GUI ini
        
        player.openInventory(inv);
    }

    @EventHandler
    public void onEditorClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        
        // Cek apakah inventory yang ditutup adalah Editor kita
        if (title.startsWith("§cEditor: ")) {
            Player player = (Player) event.getPlayer();
            
            // Ekstrak nama boss dan tier dari judul GUI
            String raw = ChatColor.stripColor(title).replace("Editor: ", "");
            String[] split = raw.split(" \\| ");
            String boss = split[0].toLowerCase();
            String tier = split[1].toLowerCase();

            player.sendMessage("§e[SeriaCrate] Menyimpan perubahan untuk " + boss.toUpperCase() + " Tier " + tier.toUpperCase() + "...");

            // Looping seluruh isi inventory yang baru saja ditutup
            Inventory inv = event.getInventory();
            int slotIndex = 1;
            
            for (ItemStack item : inv.getContents()) {
                if (item == null || item.getType().isAir()) continue;

                // TODO: Di tahap selanjutnya, kita akan membedah 'item' ini.
                // Apakah ini MMOItems? Vanilla? Atau EcoPets Spawn Egg?
                // Lalu menuliskannya ke plugin.getConfig() atau rewards.yml
                
                player.sendMessage("§7- Mendeteksi item di slot " + slotIndex);
                slotIndex++;
            }
            
            player.sendMessage("§aPenyimpanan selesai! (Logika Write YAML menyusul)");
        }
    }
}