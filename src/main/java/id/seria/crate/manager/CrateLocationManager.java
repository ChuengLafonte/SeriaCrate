package id.seria.crate.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import id.seria.crate.SeriaCrate;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;

public class CrateLocationManager {
    private final SeriaCrate plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<Location, String> crateLocations = new HashMap<>();
    private final Map<Location, TextDisplay> activeHolograms = new HashMap<>();
    private final Map<Location, ItemDisplay> activeItems = new HashMap<>();
    private final Map<Location, TextDisplay> activeItemLabels = new HashMap<>(); 

    private float rotationYaw = 0f;

    // Helper class untuk menyimpan pasangan Reward dan Tier ID-nya
    private static class RewardTierPair {
        final Reward reward;
        final String tierId;
        RewardTierPair(Reward reward, String tierId) {
            this.reward = reward;
            this.tierId = tierId;
        }
    }

    public CrateLocationManager(SeriaCrate plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "locations.yml");

        Bukkit.getScheduler().runTaskLater(plugin, this::loadLocations, 20L);

        // Task untuk memutar item display dan cycle item/label setiap detik
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            rotationYaw += 0.05f;
            long timeSeconds = System.currentTimeMillis() / 2000;
            String[] tiersList = {"s", "a", "b", "c", "d"};

            for (Map.Entry<Location, ItemDisplay> entry : activeItems.entrySet()) {
                ItemDisplay itemDisplay = entry.getValue();
                Location loc = entry.getKey();
                String boss = crateLocations.get(loc);
                
                if (itemDisplay.isValid() && boss != null) {
                    // 1. Putar item
                    Transformation transform = itemDisplay.getTransformation();
                    transform.getLeftRotation().set(new AxisAngle4f(rotationYaw, 0, 1, 0));
                    itemDisplay.setTransformation(transform);

                    // 2. Cycle Logic
                    List<RewardTierPair> allRewardsWithTiers = new ArrayList<>();
                    for (String t : tiersList) {
                        List<Reward> rewardsOfTier = plugin.getRewardManager().getRewardsFor(boss, t);
                        for (Reward r : rewardsOfTier) {
                            allRewardsWithTiers.add(new RewardTierPair(r, t));
                        }
                    }
                    
                    if (!allRewardsWithTiers.isEmpty()) {
                        int index = (int) (timeSeconds % allRewardsWithTiers.size());
                        RewardTierPair current = allRewardsWithTiers.get(index);
                        Reward reward = current.reward;
                        String tierId = current.tierId;

                        // Tampilkan item
                        ItemStack previewItem = ItemUtils.buildRewardItem(reward);
                        itemDisplay.setItemStack(previewItem);

                        // 3. Tampilkan Teks Label (format: "1x Coal [Tier S]")
                        TextDisplay labelDisplay = activeItemLabels.get(loc);
                        if (labelDisplay != null && labelDisplay.isValid()) {
                            
                            String coloredItemName;
                            // Cek apakah item memiliki Custom Name (Meta), jika tidak, gunakan Vanilla Name
                            if (previewItem.hasItemMeta() && previewItem.getItemMeta().hasDisplayName()) {
                                coloredItemName = previewItem.getItemMeta().getDisplayName();
                            } else {
                                // Format material vanilla (misal: DIAMOND_SWORD menjadi DIAMOND SWORD)
                                coloredItemName = "§f" + previewItem.getType().name().replace("_", " ");
                            }
                            
                            // Ambil jumlah (Amount) yang benar dari model Reward
                            String quantity = "§f" + reward.getAmount() + "x";
                            String tierLabel = "§8[Tier " + getTierColorLegacy(tierId) + tierId.toUpperCase() + "§8]";
                            
                            labelDisplay.setText(quantity + " " + coloredItemName + " " + tierLabel);
                        }
                    }
                }
            }
        }, 20L, 1L);
    }

    public void loadLocations() {
        this.crateLocations.clear();
        this.clearHolograms();
        if (!this.file.exists()) {
            try { this.file.createNewFile(); } catch (IOException ignored) {}
        }

        this.config = YamlConfiguration.loadConfiguration(this.file);
        if (this.config.getConfigurationSection("crates") != null) {
            Iterator<String> var1 = this.config.getConfigurationSection("crates").getKeys(false).iterator();

            while(var1.hasNext()) {
                String key = var1.next();
                String worldName = this.config.getString("crates." + key + ".world");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = this.config.getDouble("crates." + key + ".x");
                    double y = this.config.getDouble("crates." + key + ".y");
                    double z = this.config.getDouble("crates." + key + ".z");
                    String boss = this.config.getString("crates." + key + ".boss");
                    Location loc = new Location(world, x, y, z);
                    this.crateLocations.put(loc, boss);
                    this.spawnHologram(loc, boss);
                }
            }
        }
    }

    public void setCrateLocation(Location loc, String boss) {
        this.crateLocations.put(loc, boss);
        this.spawnHologram(loc, boss);
        String key = loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        this.config.set("crates." + key + ".world", loc.getWorld().getName());
        this.config.set("crates." + key + ".x", loc.getX());
        this.config.set("crates." + key + ".y", loc.getY());
        this.config.set("crates." + key + ".z", loc.getZ());
        this.config.set("crates." + key + ".boss", boss);
        try { this.config.save(this.file); } catch (IOException var5) { var5.printStackTrace(); }
    }

    private void spawnHologram(Location loc, String boss) {
        File crateFile = new File(this.plugin.getConfigManager().getRewardsFolder(), boss + ".yml");
        FileConfiguration crateConfig = YamlConfiguration.loadConfiguration(crateFile);
        if (!crateConfig.getBoolean("crate-settings.hologram", true)) return;

        double textOffset = crateConfig.getDouble("crate-settings.text-offset", 1.2D);
        double itemOffset = crateConfig.getDouble("crate-settings.item-offset", 1.8D);

        // A. Spawn Text Hologram Utama Crate
        Location holoLoc = loc.clone().add(0.5D, textOffset, 0.5D);
        TextDisplay display = holoLoc.getWorld().spawn(holoLoc, TextDisplay.class);
        FileConfiguration holoConfig = this.plugin.getConfigManager().getHologram();
        List<String> lines = holoConfig.getStringList("holograms." + boss);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(ChatColor.translateAlternateColorCodes('&', line.replace("%timer%", "Permanen"))).append("\n");
        }
        display.setText(sb.toString().trim());
        display.setBillboard(Billboard.CENTER);
        display.setDefaultBackground(false);
        this.activeHolograms.put(loc, display);

        // B. Spawn Item Display Melayang
        Location itemLoc = loc.clone().add(0.5D, itemOffset, 0.5D);
        ItemDisplay itemDisplay = itemLoc.getWorld().spawn(itemLoc, ItemDisplay.class);
        itemDisplay.setItemStack(new ItemStack(Material.CHEST));
        itemDisplay.setBillboard(Billboard.FIXED);
        
        Transformation transformation = itemDisplay.getTransformation();
        transformation.getScale().set(new Vector3f(0.7f, 0.7f, 0.7f));
        itemDisplay.setTransformation(transformation);
        this.activeItems.put(loc, itemDisplay);

        // C. Spawn Hologram Label Item (ditaruh sedikit di atas item)
        Location labelLoc = loc.clone().add(0.5D, itemOffset + 0.3D, 0.5D); 
        TextDisplay itemLabel = labelLoc.getWorld().spawn(labelLoc, TextDisplay.class);
        itemLabel.setText("§7Memuat...");
        itemLabel.setBillboard(Billboard.CENTER);
        itemLabel.setDefaultBackground(false);
        // Atur ukuran teks label agar lebih kecil sedikit dari teks utama
        Transformation textTransform = itemLabel.getTransformation();
        textTransform.getScale().set(new Vector3f(0.8f, 0.8f, 0.8f));
        itemLabel.setTransformation(textTransform);

        this.activeItemLabels.put(loc, itemLabel);
    }

    public void clearHolograms() {
        for (TextDisplay display : activeHolograms.values()) if (display != null && display.isValid()) display.remove();
        for (ItemDisplay display : activeItems.values()) if (display != null && display.isValid()) display.remove();
        for (TextDisplay label : activeItemLabels.values()) if (label != null && label.isValid()) label.remove();

        this.activeHolograms.clear();
        this.activeItems.clear();
        this.activeItemLabels.clear();
    }

    public void removeCrateLocation(Location loc) {
        this.crateLocations.remove(loc);
        TextDisplay display = this.activeHolograms.remove(loc);
        if (display != null && display.isValid()) display.remove();
        ItemDisplay item = this.activeItems.remove(loc);
        if (item != null && item.isValid()) item.remove();
        TextDisplay label = this.activeItemLabels.remove(loc);
        if (label != null && label.isValid()) label.remove();

        String key = loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        this.config.set("crates." + key, null);
        try { this.config.save(this.file); } catch (IOException ignored) {}
    }

    public String getCrateAt(Location loc) { return this.crateLocations.get(loc); }

    // Helper untuk warna legacy (agar sinkron dengan PreviewGUI.java)
    private static String getTierColorLegacy(String tier) {
        switch (tier.toLowerCase()) {
            case "s": return "§c§l";
            case "a": return "§6§l";
            case "b": return "§e§l";
            case "c": return "§b§l";
            default: return "§f§l";
        }
    }
}