package id.seria.crate.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import id.seria.crate.SeriaCrate;

public class TemporaryCrateManager {
    private final SeriaCrate plugin;
    private final Map<UUID, ActiveCrate> activeCrates = new HashMap<>();

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
        
        Hologram hologram = null;
        List<String> customLines = new ArrayList<>();

        if (useHolo) {
            FileConfiguration holoConfig = this.plugin.getConfigManager().getHologram();
            customLines = holoConfig.getStringList("holograms." + bossName);
            if (customLines.isEmpty()) {
                customLines = Arrays.asList("&e&l" + bossName.toUpperCase() + " CRATE", "&7Menghilang dalam: &c%timer%");
            }

            // Ketinggian disamakan dengan Crate Permanen agar presisi
            Location holoLoc = loc.clone().add(0.5D, 1.2D, 0.5D);
            String holoName = "tempcrate_" + crateId.toString().substring(0, 8);
            
            TextHologramData holoData = new TextHologramData(holoName, holoLoc);
            
            List<String> initialText = new ArrayList<>();
            for (String line : customLines) {
                initialText.add(ChatColor.translateAlternateColorCodes('&', line.replace("%timer%", "Menghitung...")));
            }

            holoData.setText(initialText);
            holoData.setPersistent(false); // Mencegah hologram tersimpan permanen ke config FancyHolograms
            
            hologram = FancyHologramsPlugin.get().getHologramManager().create(holoData);
            
            // Cukup gunakan addHologram, FancyHolograms akan otomatis me-render ke player.
            FancyHologramsPlugin.get().getHologramManager().addHologram(hologram);
        }

        ActiveCrate crate = new ActiveCrate(crateId, loc, bossName, hologram, duration, customLines);
        this.activeCrates.put(crateId, crate);
    }

    private void startHologramUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, ActiveCrate>> iterator = activeCrates.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<UUID, ActiveCrate> entry = iterator.next();
                    ActiveCrate crate = entry.getValue();
                    crate.timeLeft--;

                    if (crate.timeLeft <= 0) {
                        crate.location.getBlock().setType(Material.AIR);
                        if (crate.hologram != null) {
                            // API Terbaru: Gunakan removeHologram dari manager
                            FancyHologramsPlugin.get().getHologramManager().removeHologram(crate.hologram);
                        }
                        iterator.remove();
                    } else {
                        if (crate.hologram != null) {
                            int min = crate.timeLeft / 60;
                            int sec = crate.timeLeft % 60;
                            String timeStr = String.format("%02d:%02d", min, sec);
                            
                            List<String> updatedLines = new ArrayList<>();
                            for (String line : crate.customLines) {
                                updatedLines.add(ChatColor.translateAlternateColorCodes('&', line.replace("%timer%", timeStr)));
                            }

                            if (crate.hologram.getData() instanceof TextHologramData) {
                                TextHologramData textData = (TextHologramData) crate.hologram.getData();
                                textData.setText(updatedLines);
                                crate.hologram.forceUpdate(); // Mengirim pembaruan teks (countdown timer) ke pemain
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this.plugin, 0L, 20L);
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
            if (crate.hologram != null) {
                FancyHologramsPlugin.get().getHologramManager().removeHologram(crate.hologram);
            }
        }
        this.activeCrates.clear();
    }

    public static class ActiveCrate {
        public UUID id;
        public Location location;
        public String bossName;
        public Hologram hologram;
        public int timeLeft;
        public List<String> customLines; 
        public Set<UUID> claimedPlayers = new HashSet<>();

        public ActiveCrate(UUID id, Location location, String bossName, Hologram hologram, int timeLeft, List<String> customLines) {
            this.id = id;
            this.location = location;
            this.bossName = bossName;
            this.hologram = hologram;
            this.timeLeft = timeLeft;
            this.customLines = customLines;
        }
    }
}