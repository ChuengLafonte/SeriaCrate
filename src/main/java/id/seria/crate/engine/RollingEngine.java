package id.seria.crate.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;

public class RollingEngine {

    private final SeriaCrate plugin;
    private final Random random = new Random();

    public RollingEngine(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    public void startRolling(Player player, Inventory inv, List<Reward> availableRewards) {
        List<Reward> sequence = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            sequence.add(getRandomWeighted(availableRewards));
        }

        new BukkitRunnable() {
            int step = 0;
            int maxSteps = 30;
            int currentDelay = 1;

            @Override
            public void run() {
                if (step >= maxSteps) {
                    Reward win = sequence.get(step + 3); // Ambil item tengah
                    giveReward(player, win);
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 7; i++) {
                    inv.setItem(10 + i, ItemUtils.buildRewardItem(sequence.get(step + i)));
                }
                
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f, 1.5f);
                step++;

                // Logika melambat sederhana
                if (step > 20) {
                    this.cancel();
                    int nextDelay = (step > 25) ? 5 : 3;
                    new BukkitRunnable() { 
                        @Override public void run() { 
                            // Ini memicu rekursi sederhana untuk simulasi melambat
                            continueRolling(this, player, inv, sequence, step, maxSteps, nextDelay);
                        }
                    }.runTaskLater(plugin, nextDelay);
                }
            }

            public int getCurrentDelay() {
                return currentDelay;
            }

            public void setCurrentDelay(int currentDelay) {
                this.currentDelay = currentDelay;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // Fungsi pembantu untuk melanjutkan rolling saat melambat
    private void continueRolling(BukkitRunnable task, Player p, Inventory inv, List<Reward> seq, int step, int max, int delay) {
        // Logika repetisi di sini... (untuk mempersingkat, pastikan variabel terdefinisi)
    }

    private Reward getRandomWeighted(List<Reward> rewards) {
        int total = rewards.stream().mapToInt(Reward::getWeight).sum();
        int r = random.nextInt(total);
        int current = 0;
        for (Reward re : rewards) {
            current += re.getWeight();
            if (r < current) return re;
        }
        return rewards.get(0);
    }

    private void giveReward(Player player, Reward reward) {
        if (reward.getCommand() != null && reward.getCommand().startsWith("eco give")) {
            // Tarik angka dari command (contoh: "eco give %player% 50000")
            String[] parts = reward.getCommand().split(" ");
            try {
                double amount = Double.parseDouble(parts[3]);
                SeriaCrate.getInstance().getEconomy().depositPlayer(player, amount);
            } catch (Exception ignored) {}
        } else if (reward.getCommand() != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.getCommand().replace("%player%", player.getName()));
        }

        if (reward.getType().equals("ITEM") || reward.getType().equals("PET")) {
            player.getInventory().addItem(ItemUtils.buildRewardItem(reward));
        }
        player.sendMessage("§a§lSeriaCrate §7» §fSelamat! Kamu dapet §e" + (reward.getDisplayName() != null ? reward.getDisplayName() : "Hadiah"));
    }
}