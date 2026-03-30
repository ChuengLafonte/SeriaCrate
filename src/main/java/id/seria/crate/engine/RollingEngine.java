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

    public void startRolling(final Player player, final Inventory inv, List<Reward> availableRewards, final String crateId, final String tierId) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGui();
        final List<Integer> rollingSlots = guiConfig.getIntegerList("rolling-gui.rolling-slots");
        if (rollingSlots.isEmpty()) {
            for (int i = 9; i <= 17; i++) rollingSlots.add(i);
        }

        final int windowSize = rollingSlots.size();
        final int maxSteps = 45; 
        final List<Reward> sequence = new ArrayList<>();
        for (int i = 0; i < maxSteps + windowSize; i++) {
            sequence.add(this.getRandomWeighted(availableRewards));
        }

        int midIndex = windowSize / 2;
        final Reward winningReward = sequence.get((maxSteps - 1) + midIndex);

        new BukkitRunnable() {
            int step = 0;
            int ticks = 0;
            int delay = 1; 

            @Override
            public void run() {
                if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inv)) {
                    giveReward(player, winningReward, crateId, tierId); 
                    this.cancel(); 
                    return;
                }

                ticks++;
                if (ticks >= delay) {
                    ticks = 0; 
                    for (int i = 0; i < windowSize; i++) {
                        int invSlot = rollingSlots.get(i);
                        if (invSlot < inv.getSize()) {
                            inv.setItem(invSlot, ItemUtils.buildRewardItem(sequence.get(step + i)));
                        }
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, 1.5F);
                    step++;

                    if (step >= maxSteps) {
                        this.cancel(); 
                        giveReward(player, winningReward, crateId, tierId); 
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inv)) {
                                    player.closeInventory();
                                }
                            }
                        }.runTaskLater(plugin, 30L);
                    } else if (step >= maxSteps * 0.85) { delay = 8; 
                    } else if (step >= maxSteps * 0.70) { delay = 5; 
                    } else if (step >= maxSteps * 0.50) { delay = 3;
                    } else if (step >= maxSteps * 0.30) { delay = 2; }
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
                String finalCmd = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                hasGivenAnything = true;
            }
        }
        if (!hasGivenAnything && win.getDisplayItem() != null) {
            ItemStack fallback = win.getDisplayItem().clone();
            fallback.setAmount(win.getAmount());
            player.getInventory().addItem(fallback);
        }

        // Ambil prefix & pesan dari ConfigManager (Component)
        Component prefix = plugin.getConfigManager().getMessage("prefix");
        Component winMsg = plugin.getConfigManager().getMessage("success.receive-reward");
        player.sendMessage(prefix.append(winMsg));

        if (win.isBroadcast()) {
            List<String> broadcastLines = plugin.getConfigManager().getMessages().getStringList("broadcast." + tierId.toLowerCase());
            if (broadcastLines.isEmpty()) broadcastLines = plugin.getConfigManager().getMessages().getStringList("broadcast.default");

            Component rewardName = Component.text("Item Misterius");
            if (win.getDisplayItem() != null && win.getDisplayItem().hasItemMeta()) {
                if (win.getDisplayItem().getItemMeta().hasDisplayName()) {
                    rewardName = win.getDisplayItem().getItemMeta().displayName();
                }
            }

            for (String line : broadcastLines) {
                Component formatted = TextUtils.format(line
                        .replace("%player%", player.getName())
                        .replace("%crate%", crateId.toUpperCase())
                        .replace("%tier%", tierId.toUpperCase())
                        .replace("%prefix%", ""));
                
                Component finalLine = line.contains("%prefix%") ? prefix.append(formatted) : formatted;
                
                // Replace %reward% dengan Component Nama Item
                final Component rName = rewardName;
                finalLine = finalLine.replaceText(b -> b.match("%reward%").replacement(rName));
                
                Bukkit.broadcast(finalLine);
            }
        }
    }
}