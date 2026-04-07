package id.seria.crate.listener;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import id.seria.crate.util.TextUtils;
import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;

public class AdminUIListener implements Listener {

    public AdminUIListener(SeriaCrate plugin) {
    }

    // Fungsi untuk membuka GUI Editor
    public static void openEditor(Player player, String boss, String tier) {
        // Judul ini SANGAT PENTING sebagai penanda untuk dibaca saat ditutup
        Component title = TextUtils.format("§cEditor: " + boss + " | " + tier);
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        // Memuat item yang sudah ada di rewards.yml ke dalam GUI ini
        List<Reward> rewards = SeriaCrate.getInstance().getRewardManager().getRewardsFor(boss, tier);
        int slot = 0;
        if (rewards != null) {
            for (Reward reward : rewards) {
                if (slot >= 54) break;
                if (reward.getDisplayItem() != null) {
                    inv.setItem(slot, reward.getDisplayItem().clone());
                }
                slot++;
            }
        }
        
        player.openInventory(inv);
    }

    @EventHandler
    public void onEditorClose(InventoryCloseEvent event) {
        Component titleComp = event.getView().title();
        String title = PlainTextComponentSerializer.plainText().serialize(titleComp);
        
        // Cek apakah inventory yang ditutup adalah Editor kita
        if (title != null && title.startsWith("Editor: ")) {
            Player player = (Player) event.getPlayer();
            
            // Ekstrak nama boss dan tier dari judul GUI
            String raw = title.replace("Editor: ", "");
            String[] split = raw.split(" \\| ");
            if (split.length < 2) return;
            String boss = split[0].toLowerCase();
            String tier = split[1].toLowerCase();

            player.sendMessage("§e[SeriaCrate] Menyimpan perubahan untuk " + boss.toUpperCase() + " Tier " + tier.toUpperCase() + "...");

            // Looping seluruh isi inventory yang baru saja ditutup
            Inventory inv = event.getInventory();
            int slotIndex = 1;
            
            for (ItemStack item : inv.getContents()) {
                if (item == null || item.getType().isAir()) {
                    slotIndex++;
                    continue;
                }

                // Menyimpan perubahan 'item' ini ke rewards.yml
                SeriaCrate.getInstance().getRewardManager().updateRewardItem(boss, tier, slotIndex, item);
                slotIndex++;
            }
            
            player.sendMessage("§a[SeriaCrate] Berhasil menyimpan " + (slotIndex - 1) + " item ke database.");
        }
    }
}