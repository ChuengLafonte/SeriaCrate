package id.seria.crate.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;
import id.seria.crate.util.TextUtils;
import net.kyori.adventure.text.Component;

public class RollingEngine {

    private final SeriaCrate plugin;
    private final Random random = new Random();

    // Slot tengah baris rolling (slot ke-5 dari 9 rolling slots → index 4 → slot inventory 13)
    // Layout 27 slot (3 baris):
    //  Baris 1 (slot 0-8)  : filler
    //  Baris 2 (slot 9-17) : rolling items (9 slot)
    //  Baris 3 (slot 18-26): filler, pointer di slot 22
    // Pointer atas  : slot 4
    // Pointer bawah : slot 22
    // Center rolling: slot 13 (index ke-4 dari rolling slots[9-17])
    private static final int CENTER_INDEX = 4; // index dalam rolling-slots list

    public RollingEngine(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    public Reward getRandomWeighted(List<Reward> rewards) {
        int totalWeight = 0;
        for (Reward reward : rewards) totalWeight += reward.getWeight();
        if (totalWeight <= 0) return rewards.get(0);

        int rand = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (Reward reward : rewards) {
            currentWeight += reward.getWeight();
            if (rand < currentWeight) return reward;
        }
        return rewards.get(0);
    }

    /**
     * Mulai animasi rolling.
     * Logika sama dengan Skript:
     *  - 27 slot, baris tengah (slot 9-17) bergulir
     *  - Pointer Hopper di slot 4 (atas) dan 22 (bawah)
     *  - Pemenang ditentukan di awal, ditaruh pada posisi CENTER_INDEX
     *    ketika animasi berhenti
     */
    public void startRolling(final Player player, final Inventory inv,
                             List<Reward> availableRewards,
                             final String crateId, final String tierId) {

        FileConfiguration guiConfig = plugin.getConfigManager().getGui();
        final List<Integer> rollingSlots = new ArrayList<>(guiConfig.getIntegerList("rolling-gui.rolling-slots"));
        if (rollingSlots.isEmpty()) {
            for (int i = 9; i <= 17; i++) rollingSlots.add(i);
        }

        final int windowSize = rollingSlots.size(); // 9
        // Total step animasi — item bergulir sebanyak ini
        final int maxSteps = 40;

        // Buat sequence item yang akan ditampilkan
        // Panjang: maxSteps + windowSize agar selalu ada cukup item
        final List<Reward> sequence = new ArrayList<>();
        for (int i = 0; i < maxSteps + windowSize; i++) {
            sequence.add(this.getRandomWeighted(availableRewards));
        }

        // Pemenang = item yang akan berhenti tepat di CENTER_INDEX
        // Ketika step = maxSteps - 1, tampilan window adalah sequence[maxSteps-1 .. maxSteps-1+windowSize-1]
        // Item di CENTER_INDEX pada frame terakhir = sequence[(maxSteps - 1) + CENTER_INDEX]
        final Reward winningReward = sequence.get((maxSteps - 1) + CENTER_INDEX);

        // Isi window awal sebelum animasi dimulai
        for (int i = 0; i < windowSize; i++) {
            int slot = rollingSlots.get(i);
            if (slot < inv.getSize()) {
                inv.setItem(slot, ItemUtils.buildRewardItem(sequence.get(i)));
            }
        }

        new BukkitRunnable() {
            int step = 0;
            int ticks = 0;
            int delay = 1; // tick per frame (makin besar = makin lambat)

            @Override
            public void run() {
                // Jika player tutup inventory sebelum selesai → beri reward langsung
                if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inv)) {
                    giveReward(player, winningReward, crateId, tierId);
                    this.cancel();
                    return;
                }

                ticks++;
                if (ticks < delay) return;
                ticks = 0;

                // Geser tampilan window 1 langkah ke depan
                for (int i = 0; i < windowSize; i++) {
                    int invSlot = rollingSlots.get(i);
                    if (invSlot < inv.getSize()) {
                        inv.setItem(invSlot, ItemUtils.buildRewardItem(sequence.get(step + i)));
                    }
                }

                // Bunyi rolling — makin lambat makin rendah pitchnya
                float pitch = 0.5f + (1.5f * step / maxSteps);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, pitch);

                step++;

                if (step >= maxSteps) {
                    // Selesai — pastikan frame terakhir tampil dengan benar
                    // (pemenang ada di CENTER_INDEX)
                    for (int i = 0; i < windowSize; i++) {
                        int invSlot = rollingSlots.get(i);
                        if (invSlot < inv.getSize()) {
                            inv.setItem(invSlot, ItemUtils.buildRewardItem(sequence.get((maxSteps - 1) + i)));
                        }
                    }

                    this.cancel();
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

                    // Tunda 1.5 detik sebelum beri reward & tutup inventory
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            giveReward(player, winningReward, crateId, tierId);
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inv)) {
                                        player.closeInventory();
                                    }
                                }
                            }.runTaskLater(plugin, 30L);
                        }
                    }.runTaskLater(plugin, 30L);

                // Perlambatan progresif (sama seperti Skript versi smooth)
                } else if (step >= maxSteps * 0.85) { delay = 8;
                } else if (step >= maxSteps * 0.70) { delay = 5;
                } else if (step >= maxSteps * 0.50) { delay = 3;
                } else if (step >= maxSteps * 0.30) { delay = 2;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void giveReward(Player player, Reward win, String crateId, String tierId) {
        boolean hasGivenAnything = false;

        if (win.getWinItemsClean() != null) {
            for (String cleanStr : win.getWinItemsClean()) {
                ItemStack item = ItemUtils.deserializeItemClean(cleanStr);
                if (item != null && item.getType() != Material.STONE) {
                    player.getInventory().addItem(item);
                    hasGivenAnything = true;
                }
            }
        }

        if (win.getCommands() != null) {
            for (String cmd : win.getCommands()) {
                String finalCmd = cmd
                    .replace("%player%", player.getName())
                    .replace("%player_name%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                hasGivenAnything = true;
            }
        }

        if (!hasGivenAnything && win.getDisplayItem() != null) {
            ItemStack fallback = win.getDisplayItem().clone();
            fallback.setAmount(win.getAmount());
            player.getInventory().addItem(fallback);
        }

        // Kirim pesan selamat
        Component prefix = plugin.getConfigManager().getMessage("prefix");
        Component winMsg = plugin.getConfigManager().getMessage("success.receive-reward");
        player.sendMessage(prefix.append(winMsg));

        // Broadcast jika reward ini punya broadcast: true
        if (win.isBroadcast()) {
            List<String> broadcastLines = plugin.getConfigManager().getMessages().getStringList("broadcast." + tierId.toLowerCase());
            if (broadcastLines.isEmpty()) {
                broadcastLines = plugin.getConfigManager().getMessages().getStringList("broadcast.default");
            }

            Component rewardName = Component.text("Item Misterius");
            if (win.getDisplayItem() != null && win.getDisplayItem().hasItemMeta()) {
                if (win.getDisplayItem().getItemMeta().hasDisplayName()) {
                    rewardName = win.getDisplayItem().getItemMeta().displayName();
                }
            }

            for (String line : broadcastLines) {
                Component formatted = TextUtils.format(line
                        .replace("%player%", player.getName())
                        .replace("%player_name%", player.getName())
                        .replace("%crate%", crateId.toUpperCase())
                        .replace("%tier%", tierId.toUpperCase())
                        .replace("%prefix%", ""));

                Component finalLine = line.contains("%prefix%") ? prefix.append(formatted) : formatted;
                final Component rName = rewardName;
                finalLine = finalLine.replaceText(b -> b.match("%reward%").replacement(rName));

                Bukkit.broadcast(finalLine);
            }
        }
    }
}
