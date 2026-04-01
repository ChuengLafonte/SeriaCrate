package id.seria.crate.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import id.seria.crate.SeriaCrate;

public class ResinItemListener implements Listener {

    private final SeriaCrate plugin;

    // Cache cooldown per player agar tidak double-trigger (ticks)
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private static final long COOLDOWN_MS = 500;

    public ResinItemListener(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Hanya klik kanan, hanya tangan utama
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        Player player = event.getPlayer();

        // Cek semua resin-items di config
        ConfigurationSection section = plugin.getConfigManager().getConfig()
                .getConfigurationSection("resin-items");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            // Cocokkan item di tangan dengan material di config
            if (!matchesItem(item, entry.getString("material", ""))) continue;

            // Item cocok — cancel event agar tidak trigger aksi vanilla
            event.setCancelled(true);

            // Cooldown anti double-trigger
            long now = System.currentTimeMillis();
            if (cooldown.getOrDefault(player.getUniqueId(), 0L) + COOLDOWN_MS > now) return;
            cooldown.put(player.getUniqueId(), now);

            // Ambil jumlah resin dari config
            int amount = entry.getInt("amount", 20);

            // PENGECEKAN BATAS MAX RESIN DIHAPUS DI SINI AGAR BISA OVERCAP
            
            // Kurangi item di tangan sebanyak 1
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            // Eksekusi via console — player tidak bisa eksploitasi manual command
            // Command 'resinadmin add' akan menambah resin melewati batas karena Math.min sudah dihapus di ResinManager
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "resinadmin add " + player.getName() + " " + amount);

            // Pesan sukses (dihandle di ResinCommand, tapi bisa tambah di sini juga)
            return; // Stop setelah item pertama yang cocok
        }
    }

    /**
     * Cocokkan ItemStack dengan format material di config.
     * Format yang didukung:
     *   - "GOLD_NUGGET"               (vanilla)
     *   - "mmoitems-TYPE:ID"          (MMOItems)
     */
    private boolean matchesItem(ItemStack item, String materialStr) {
        if (materialStr == null || materialStr.isEmpty()) return false;

        if (materialStr.startsWith("mmoitems-")) {
            // Format: mmoitems-TYPE:ID
            try {
                String[] parts = materialStr.substring(9).split(":");
                if (parts.length < 2) return false;

                io.lumine.mythic.lib.api.item.NBTItem nbt =
                        io.lumine.mythic.lib.api.item.NBTItem.get(item);
                if (!nbt.hasType()) return false;

                String itemType = nbt.getType();
                String itemId   = nbt.getString("MMOITEMS_ITEM_ID");

                return parts[0].equalsIgnoreCase(itemType)
                        && parts[1].equalsIgnoreCase(itemId);
            } catch (Exception ignored) {
                return false;
            }
        }

        // Vanilla material
        Material mat = Material.matchMaterial(materialStr);
        return mat != null && item.getType() == mat;
    }
}