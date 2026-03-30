package id.seria.crate.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    public void startRolling(final Player player, final Inventory inv, List<Reward> availableRewards) {
        final int maxSteps = 45; 
        final List<Reward> sequence = new ArrayList<>();
        
        for (int i = 0; i < maxSteps + 9; i++) {
            sequence.add(this.getRandomWeighted(availableRewards));
        }

        // Item pemenang ditentukan di awal, sebelum animasi berjalan
        final Reward winningReward = sequence.get(maxSteps + 3);

        new BukkitRunnable() {
            int step = 0;
            int ticks = 0;
            int delay = 1; 

            @Override
            public void run() {
                // ANTI-EXPLOIT: Jika pemain DC atau dengan sengaja menekan 'ESC' / menutup inventory
                if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inv)) {
                    giveReward(player, winningReward); // Berikan item pemenang secara instan
                    this.cancel(); // Matikan task
                    return;
                }

                ticks++;
                
                if (ticks >= delay) {
                    ticks = 0; 
                    
                    for (int i = 0; i < 9; i++) {
                        inv.setItem(9 + i, ItemUtils.buildRewardItem(sequence.get(step + i)));
                    }
                    
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, 1.5F);
                    step++;

                    // Saat putaran selesai
                    if (step >= maxSteps) {
                        this.cancel(); // Matikan task
                        
                        giveReward(player, winningReward); // Berikan hadiah
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                        
                        // Berikan jeda 1.5 detik untuk melihat item kemenangannya sebelum GUI tertutup
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inv)) {
                                    player.closeInventory();
                                }
                            }
                        }.runTaskLater(plugin, 30L);
                        
                    } else if (step >= maxSteps * 0.85) {
                        delay = 8; 
                    } else if (step >= maxSteps * 0.70) {
                        delay = 5; 
                    } else if (step >= maxSteps * 0.50) {
                        delay = 3;
                    } else if (step >= maxSteps * 0.30) {
                        delay = 2;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); 
    }

    public void giveReward(Player player, Reward win) {
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
                String finalCmd = cmd.replace("%player%", player.getName()).replace("%player_name%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                hasGivenAnything = true;
            }
        }

        if (!hasGivenAnything && win.getDisplayItem() != null) {
            ItemStack fallback = win.getDisplayItem().clone();
            fallback.setAmount(win.getAmount());
            player.getInventory().addItem(fallback);
        }

        String prefix = org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getConfig().getString("settings.prefix", "&8[&eSeriaCrate&8] "));
        player.sendMessage(prefix + "§fSelamat! Kamu memenangkan hadiah dari crate!");
    }
}