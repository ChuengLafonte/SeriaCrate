package id.seria.crate.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import net.kyori.adventure.text.Component;
import id.seria.crate.util.TextUtils;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;

public class TemporaryCrateManager {
    private final SeriaCrate plugin;
    private final Map<UUID, ActiveCrate> activeCrates = new HashMap<>();
    private float rotationYaw = 0f;

    // Helper class (sama seperti CrateLocationManager)
    private static class RewardTierPair {
        final Reward reward;
        final String tierId;
        RewardTierPair(Reward reward, String tierId) {
            this.reward = reward;
            this.tierId = tierId;
        }
    }

    public TemporaryCrateManager(SeriaCrate plugin) {
        this.plugin = plugin;
        this.startHologramUpdater();
    }

    public void spawnTemporaryCrate(Location loc, String bossName) {
        UUID crateId = UUID.randomUUID();
        File file = new File(this.plugin.getConfigManager().getRewardsFolder(), bossName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        String blockName = config.getString("crate-settings.block", "ENDER_CHEST");
        Material mat = Material.matchMaterial(blockName);
        loc.getBlock().setType(mat != null ? mat : Material.ENDER_CHEST);
        
        boolean useHolo = config.getBoolean("crate-settings.hologram", true);
        int duration = config.getInt("crate-settings.duration", 180);
        
        TextDisplay textDisplay = null;
        ItemDisplay itemDisplay = null;
        TextDisplay itemLabel = null;
        List<String> customLines = new ArrayList<>();

        if (useHolo) {
            FileConfiguration holoConfig = this.plugin.getConfigManager().getHologram();
            customLines = holoConfig.getStringList("holograms." + bossName);
            if (customLines.isEmpty()) {
                customLines = java.util.Arrays.asList("&e&l" + bossName.toUpperCase() + " CRATE", "&7Menghilang dalam: &c%timer%");
            }

            double textOffset = config.getDouble("crate-settings.text-offset", 1.2D);
            double itemOffset = config.getDouble("crate-settings.item-offset", 1.8D);

            // 1. Text Display Utama (Hologram Timer)
            Location holoLoc = loc.clone().add(0.5D, textOffset, 0.5D);
            textDisplay = holoLoc.getWorld().spawn(holoLoc, TextDisplay.class);
            textDisplay.setBillboard(Billboard.CENTER);
            textDisplay.setDefaultBackground(false);

            // 2. Item Display (Animasi Melayang)
            Location itemLoc = loc.clone().add(0.5D, itemOffset, 0.5D);
            itemDisplay = itemLoc.getWorld().spawn(itemLoc, ItemDisplay.class);
            itemDisplay.setItemStack(new ItemStack(Material.CHEST));
            itemDisplay.setBillboard(Billboard.FIXED);
            Transformation transformation = itemDisplay.getTransformation();
            transformation.getScale().set(new Vector3f(0.7f, 0.7f, 0.7f));
            itemDisplay.setTransformation(transformation);

            // 3. Text Display Label (Nama Item)
            Location labelLoc = loc.clone().add(0.5D, itemOffset + 0.3D, 0.5D); 
            itemLabel = labelLoc.getWorld().spawn(labelLoc, TextDisplay.class);
            itemLabel.setBillboard(Billboard.CENTER);
            itemLabel.setDefaultBackground(false);
            Transformation textTransform = itemLabel.getTransformation();
            textTransform.getScale().set(new Vector3f(0.8f, 0.8f, 0.8f));
            itemLabel.setTransformation(textTransform);
        }

        ActiveCrate crate = new ActiveCrate(crateId, loc, bossName, duration, customLines, textDisplay, itemDisplay, itemLabel);
        this.activeCrates.put(crateId, crate);
    }

    private void startHologramUpdater() {
        // Scheduler berjalan 1 tick (sangat cepat) agar animasi putaran item mulus
        new BukkitRunnable() {
            @Override
            public void run() {
                rotationYaw += 0.05f;
                long timeSeconds = System.currentTimeMillis() / 2000; // Cycle item setiap 2 detik
                String[] tiersList = {"s", "a", "b", "c", "d"};

                Iterator<Map.Entry<UUID, ActiveCrate>> iterator = activeCrates.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<UUID, ActiveCrate> entry = iterator.next();
                    ActiveCrate crate = entry.getValue();

                    // --- LOGIKA HITUNG MUNDUR (DIKURANGI SETIAP 20 TICK) ---
                    // Menggunakan logic ini karena scheduler sekarang berjalan setiap 1 tick
                    if (crate.tickCounter >= 20) {
                        crate.timeLeft--;
                        crate.tickCounter = 0;
                    } else {
                        crate.tickCounter++;
                    }

                    // --- LOGIKA HANCUR ---
                    if (crate.timeLeft <= 0) {
                        crate.location.getBlock().setType(Material.AIR);
                        if (crate.textDisplay != null && crate.textDisplay.isValid()) crate.textDisplay.remove();
                        if (crate.itemDisplay != null && crate.itemDisplay.isValid()) crate.itemDisplay.remove();
                        if (crate.itemLabel != null && crate.itemLabel.isValid()) crate.itemLabel.remove();
                        iterator.remove();
                        continue;
                    }

                    // --- LOGIKA UPDATE TAMPILAN TEXT & ITEM ---
                    if (crate.textDisplay != null && crate.textDisplay.isValid()) {
                        
                        // 1. Update Timer Teks Hologram
                        int min = crate.timeLeft / 60;
                        int sec = crate.timeLeft % 60;
                        String timeStr = String.format("%02d:%02d", min, sec);
                        
                        Component finalHolo = Component.empty();
                        for (int i = 0; i < crate.customLines.size(); i++) {
                            finalHolo = finalHolo.append(TextUtils.format(crate.customLines.get(i).replace("%timer%", timeStr)));
                            if (i < crate.customLines.size() - 1) finalHolo = finalHolo.append(Component.newline());
                        }
                        crate.textDisplay.text(finalHolo);

                        // 2. Putar dan Ganti Item Melayang
                        if (crate.itemDisplay != null && crate.itemDisplay.isValid()) {
                            Transformation transform = crate.itemDisplay.getTransformation();
                            transform.getLeftRotation().set(new AxisAngle4f(rotationYaw, 0, 1, 0));
                            crate.itemDisplay.setTransformation(transform);

                            List<RewardTierPair> allRewardsWithTiers = new ArrayList<>();
                            for (String t : tiersList) {
                                List<Reward> rewardsOfTier = plugin.getRewardManager().getRewardsFor(crate.bossName, t);
                                for (Reward r : rewardsOfTier) {
                                    allRewardsWithTiers.add(new RewardTierPair(r, t));
                                }
                            }
                            
                            if (!allRewardsWithTiers.isEmpty()) {
                                int index = (int) (timeSeconds % allRewardsWithTiers.size());
                                RewardTierPair current = allRewardsWithTiers.get(index);
                                Reward reward = current.reward;
                                String tierId = current.tierId;

                                ItemStack previewItem = ItemUtils.buildRewardItem(reward);
                                crate.itemDisplay.setItemStack(previewItem);

                                // 3. Update Label Item
                                    // Modern way to get display name component
                                    if (previewItem.hasItemMeta() && previewItem.getItemMeta().hasDisplayName()) {
                                        crate.itemLabel.text(previewItem.getItemMeta().displayName().append(
                                            TextUtils.format(" &8[Tier " + getTierColorLegacy(tierId) + tierId.toUpperCase() + "§8]")
                                        ));
                                    } else {
                                        String coloredName = "§f" + previewItem.getType().name().replace("_", " ");
                                        String tierLabel = "§8[Tier " + getTierColorLegacy(tierId) + tierId.toUpperCase() + "§8]";
                                        crate.itemLabel.text(TextUtils.format("§f" + reward.getAmount() + "x " + coloredName + " " + tierLabel));
                                    }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L); // Berjalan 1 tick agar animasi putaran item mulus
    }

    public ActiveCrate getCrateAt(Location loc) {
        for (ActiveCrate crate : this.activeCrates.values()) {
            if (crate.location.getBlockX() == loc.getBlockX() && 
                crate.location.getBlockY() == loc.getBlockY() && 
                crate.location.getBlockZ() == loc.getBlockZ()) {
                return crate;
            }
        }
        return null;
    }

    public void forceClearAllCrates() {
        for (ActiveCrate crate : this.activeCrates.values()) {
            crate.location.getBlock().setType(Material.AIR);
            if (crate.textDisplay != null && crate.textDisplay.isValid()) crate.textDisplay.remove();
            if (crate.itemDisplay != null && crate.itemDisplay.isValid()) crate.itemDisplay.remove();
            if (crate.itemLabel != null && crate.itemLabel.isValid()) crate.itemLabel.remove();
        }
        this.activeCrates.clear();
    }

    private static String getTierColorLegacy(String tier) {
        switch (tier.toLowerCase()) {
            case "s": return "§c§l";
            case "a": return "§6§l";
            case "b": return "§e§l";
            case "c": return "§b§l";
            default: return "§f§l";
        }
    }

    public static class ActiveCrate {
        public UUID id;
        public Location location;
        public String bossName;
        public int timeLeft;
        public int tickCounter = 0; // Digunakan untuk menghitung tick per detik
        public List<String> customLines; 
        public Set<UUID> claimedPlayers = new HashSet<>();
        
        public TextDisplay textDisplay;
        public ItemDisplay itemDisplay;
        public TextDisplay itemLabel;

        public ActiveCrate(UUID id, Location location, String bossName, int timeLeft, List<String> customLines, 
                           TextDisplay textDisplay, ItemDisplay itemDisplay, TextDisplay itemLabel) {
            this.id = id;
            this.location = location;
            this.bossName = bossName;
            this.timeLeft = timeLeft;
            this.customLines = customLines;
            this.textDisplay = textDisplay;
            this.itemDisplay = itemDisplay;
            this.itemLabel = itemLabel;
        }
    }
}