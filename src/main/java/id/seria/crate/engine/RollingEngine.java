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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;
import id.seria.crate.util.TextUtils;
import net.kyori.adventure.text.Component;

public class RollingEngine {

    private final SeriaCrate plugin;
    private final Random random = new Random();
    private static final int CENTER_INDEX = 4;

    public RollingEngine(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    public Reward getRandomWeighted(List<Reward> rewards) {
        int totalWeight = rewards.stream().mapToInt(Reward::getWeight).sum();
        if (totalWeight <= 0) return rewards.get(0);
        int rand = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (Reward reward : rewards) {
            currentWeight += reward.getWeight();
            if (rand < currentWeight) return reward;
        }
        return rewards.get(0);
    }

    // Helper untuk membuat dummy item visual representasi Tier saat gacha tahap 1
    private ItemStack getTierDisplayItem(String tier) {
        Material[] mats = {Material.NETHER_STAR, Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT, Material.COAL};
        String[] tiers = {"s", "a", "b", "c", "d"};
        Material mat = Material.PAPER;
        String color = "§f§l";
        
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i].equalsIgnoreCase(tier)) {
                mat = mats[i];
                if (tier.equals("s")) color = "§c§l";
                else if (tier.equals("a")) color = "§6§l";
                else if (tier.equals("b")) color = "§e§l";
                else if (tier.equals("c")) color = "§b§l";
                break;
            }
        }
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(id.seria.crate.util.TextUtils.format(color + "Mendapatkan Tier " + tier.toUpperCase() + "..."));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void startRolling(final Player player, final Inventory inv,
                             List<Reward> availableRewards,
                             final String crateId, final String tierId) {

        FileConfiguration guiConfig = plugin.getConfigManager().getGui();
        final List<Integer> rollingSlots = new ArrayList<>(guiConfig.getIntegerList("rolling-gui.rolling-slots"));
        if (rollingSlots.isEmpty()) {
            for (int i = 9; i <= 17; i++) rollingSlots.add(i);
        }
        final int windowSize = rollingSlots.size(); // 9

        // ==================================================
        // [BARU] Terapkan Fillers ke Background Rolling Gacha
        // ==================================================
        List<ItemUtils.GUIItem> fillers = ItemUtils.loadGUIItems(guiConfig.getConfigurationSection("rolling-gui.fillers"));
        fillers.sort(java.util.Comparator.comparingInt(a -> a.priority));
        for (ItemUtils.GUIItem gItem : fillers) {
            for (int s : gItem.slots) {
                // Pastikan filler tidak menimpa slot rolling item
                if (s < inv.getSize() && !rollingSlots.contains(s)) {
                    inv.setItem(s, gItem.item.clone());
                }
            }
        }

        // ----------------------------------------------------
        // PERSIAPAN TAHAP 1: TIER ROLLING & TAHAP 2: ITEM ROLLING
        // ----------------------------------------------------
        final int tierSteps = 30; // Lama animasi tier
        final int itemSteps = 40; // Lama animasi item
        
        // Sequence Tier (Acak, tapi berujung pada tierId yang asli)
        final List<String> tierSequence = new ArrayList<>();
        String[] allTiers = {"s", "a", "b", "c", "d"};
        for (int i = 0; i < tierSteps + windowSize; i++) {
            tierSequence.add(allTiers[random.nextInt(allTiers.length)]);
        }
        tierSequence.set((tierSteps - 1) + CENTER_INDEX, tierId.toLowerCase()); // Fix target tier

        // Sequence Item Pemenang
        final List<Reward> itemSequence = new ArrayList<>();
        for (int i = 0; i < itemSteps + windowSize; i++) {
            itemSequence.add(this.getRandomWeighted(availableRewards));
        }
        final Reward winningReward = itemSequence.get((itemSteps - 1) + CENTER_INDEX);

        // --- MULAI TAHAP 1: ROLLING TIER ---
        new BukkitRunnable() {
            int step = 0; int ticks = 0; int delay = 1;

            @Override
            public void run() {
                if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inv)) {
                    giveReward(player, winningReward, crateId, tierId);
                    this.cancel();
                    return;
                }

                ticks++;
                if (ticks < delay) return;
                ticks = 0;

                for (int i = 0; i < windowSize; i++) {
                    int invSlot = rollingSlots.get(i);
                    if (invSlot < inv.getSize()) inv.setItem(invSlot, getTierDisplayItem(tierSequence.get(step + i)));
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 0.8F, 1.2F);
                step++;

                if (step >= tierSteps) {
                    this.cancel();
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
                    
                    // Transisi Jeda Sejenak (1 detik) sebelum masuk ke Tahap 2
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            startItemPhase(player, inv, rollingSlots, windowSize, itemSequence, itemSteps, winningReward, crateId, tierId);
                        }
                    }.runTaskLater(plugin, 20L);

                } else if (step >= tierSteps * 0.85) { delay = 6;
                } else if (step >= tierSteps * 0.60) { delay = 3;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // --- MULAI TAHAP 2: ROLLING ITEM ---
    private void startItemPhase(final Player player, final Inventory inv, final List<Integer> rollingSlots, final int windowSize, 
                                final List<Reward> sequence, final int maxSteps, final Reward winningReward, 
                                final String crateId, final String tierId) {
        
        new BukkitRunnable() {
            int step = 0; int ticks = 0; int delay = 1;

            @Override
            public void run() {
                if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inv)) {
                    giveReward(player, winningReward, crateId, tierId);
                    this.cancel();
                    return;
                }

                ticks++;
                if (ticks < delay) return;
                ticks = 0;

                for (int i = 0; i < windowSize; i++) {
                    int invSlot = rollingSlots.get(i);
                    if (invSlot < inv.getSize()) inv.setItem(invSlot, ItemUtils.buildRewardItem(sequence.get(step + i)));
                }

                float pitch = 0.5f + (1.5f * step / maxSteps);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, pitch);
                step++;

                if (step >= maxSteps) {
                    this.cancel();
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            giveReward(player, winningReward, crateId, tierId);
                            if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inv)) player.closeInventory();
                        }
                    }.runTaskLater(plugin, 30L);

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
