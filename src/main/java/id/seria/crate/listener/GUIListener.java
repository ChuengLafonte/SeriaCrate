package id.seria.crate.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import id.seria.crate.SeriaCrate;

public class GUIListener implements Listener {

    private final SeriaCrate plugin;
    private final Set<UUID> isRolling = new HashSet<>();

    public GUIListener(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Abaikan jika ini adalah menu Editor (sudah ditangani di EditorListener)
        if (event.getInventory().getHolder() instanceof id.seria.crate.gui.editor.EditorHolder) return;

        Component titleComp = event.getView().title();
        String title = PlainTextComponentSerializer.plainText().serialize(titleComp);
        
        // Deteksi GUI berdasarkan kata kunci yang ada di config gui.yml
        if (title.contains("Pilih Tier") || title.contains("Reward:") || title.contains("Rolling Crate") || title.contains("Reward Crate")) {
            event.setCancelled(true);
            
            // Logika navigasi untuk Preview GUI (Pilih Tier -> Buka Reward List)
            if (title.contains("Pilih Tier")) {
                org.bukkit.inventory.ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                    String itemName = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());
                    String bossName = title.replace("Pilih Tier: ", "").trim();
                    
                    org.bukkit.inventory.Inventory newInv = null;
                    if (itemName.contains("Tier S")) newInv = id.seria.crate.gui.PreviewGUI.createRewardPreview(bossName, "s");
                    else if (itemName.contains("Tier A")) newInv = id.seria.crate.gui.PreviewGUI.createRewardPreview(bossName, "a");
                    else if (itemName.contains("Tier B")) newInv = id.seria.crate.gui.PreviewGUI.createRewardPreview(bossName, "b");
                    else if (itemName.contains("Tier C")) newInv = id.seria.crate.gui.PreviewGUI.createRewardPreview(bossName, "c");
                    else if (itemName.contains("Tier D")) newInv = id.seria.crate.gui.PreviewGUI.createRewardPreview(bossName, "d");

                    // [PERBAIKAN] Buka GUI baru melalui Scheduler agar Minecraft Client tidak glitch (menutup GUI secara acak)
                    if (newInv != null) {
                        final org.bukkit.inventory.Inventory finalInv = newInv;
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> event.getWhoClicked().openInventory(finalInv));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (isRolling.contains(uuid)) {
            isRolling.remove(uuid);
        }
    }

    public Set<UUID> getRollingPlayers() {
        return isRolling;
    }
}