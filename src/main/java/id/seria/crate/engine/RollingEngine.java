package id.seria.crate.engine;

import java.util.ArrayList;
import java.util.List;

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

    public RollingEngine(SeriaCrate plugin) { this.plugin = plugin; }

    public Reward getRandomWeighted(List<Reward> rewards) {
        int totalWeight = 0;
        for (Reward reward : rewards) totalWeight += reward.getWeight();
        int random = new java.util.Random().nextInt(totalWeight);
        int currentWeight = 0;
        for (Reward reward : rewards) {
            currentWeight += reward.getWeight();
            if (random < currentWeight) return reward;
        }
        return rewards.get(0);
    }

    public void startRolling(final Player player, final Inventory inv, List<Reward> availableRewards) {
        final List<Reward> sequence = new ArrayList<>();
        for (int i = 0; i < 50; i++) sequence.add(this.getRandomWeighted(availableRewards));

        new BukkitRunnable() {
            int step = 0; final int maxSteps = 30;
            @Override
            public void run() {
                if (step >= maxSteps) {
                    Reward win = sequence.get(step + 3); 
                    giveReward(player, win);
                    this.cancel(); return;
                }
                for (int i = 0; i < 7; i++) inv.setItem(10 + i, ItemUtils.buildRewardItem(sequence.get(step + i)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
                step++;
                if (step > 20) {
                    this.cancel(); 
                    continueRolling(player, inv, sequence, step, maxSteps, step > 25 ? 5 : 3);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void continueRolling(Player player, Inventory inv, List<Reward> sequence, int currentStep, int maxSteps, int delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (currentStep >= maxSteps) {
                    Reward win = sequence.get(currentStep + 3);
                    giveReward(player, win); return; 
                }
                for (int i = 0; i < 7; i++) inv.setItem(10 + i, ItemUtils.buildRewardItem(sequence.get(currentStep + i)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.0F); 
                continueRolling(player, inv, sequence, currentStep + 1, maxSteps, (currentStep + 1) > 25 ? 5 : 3);
            }
        }.runTaskLater(plugin, delay);
    }

    public void giveReward(Player player, Reward win) {
        boolean hasGivenAnything = false;

        // Distribusikan Multi-Item
        for (String cleanStr : win.getWinItemsClean()) {
            ItemStack item = ItemUtils.deserializeItemClean(cleanStr);
            if (item != null && item.getType() != org.bukkit.Material.STONE) {
                player.getInventory().addItem(item);
                hasGivenAnything = true;
            }
        }

        // Eksekusi Multi-Command
        for (String cmd : win.getCommands()) {
            String finalCmd = cmd.replace("%player%", player.getName()).replace("%player_name%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            hasGivenAnything = true;
        }

        // Fallback jika Admin lupa ngisi
        if (!hasGivenAnything && win.getDisplayItem() != null) {
            ItemStack fallback = win.getDisplayItem().clone();
            fallback.setAmount(win.getAmount());
            player.getInventory().addItem(fallback);
        }

        String prefix = org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getConfig().getString("settings.prefix", "&a&lSeriaCrate &7» &f"));
        player.sendMessage(prefix + "§fSelamat! Kamu memenangkan hadiah dari crate!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
    }
}