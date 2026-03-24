package id.seria.crate.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
        startHologramUpdater();
    }

    public void spawnTemporaryCrate(Location loc, String bossName) {
        UUID crateId = UUID.randomUUID();
        
        // 1. Baca Blok dari file spesifik
        java.io.File file = new java.io.File(plugin.getConfigManager().getRewardsFolder(), bossName + ".yml");
        org.bukkit.configuration.file.FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        String blockName = config.getString("crate-settings.block", "ENDER_CHEST");
        Material mat = Material.matchMaterial(blockName);
        loc.getBlock().setType(mat != null ? mat : Material.ENDER_CHEST);

        // 2. Baca Teks dari hologram.yml
        org.bukkit.configuration.file.FileConfiguration holoConfig = plugin.getConfigManager().getHologram();
        List<String> customLines = holoConfig.getStringList("holograms." + bossName);
        if (customLines.isEmpty()) {
            customLines = java.util.Arrays.asList("&e&l" + bossName.toUpperCase() + " CRATE", "&7Menghilang dalam: &c%timer%");
        }

        String holoName = "crate_" + crateId.toString().substring(0, 8);
        Location holoLoc = loc.clone().add(0.5, 2.2, 0.5); 
        
        TextHologramData holoData = new TextHologramData(holoName, holoLoc);
        
        // Format teks awal untuk dimunculkan pertama kali
        List<String> initialText = new ArrayList<>();
        for (String line : customLines) {
            initialText.add(ChatColor.translateAlternateColorCodes('&', line.replace("%timer%", "03:00")));
        }
        holoData.setText(initialText);

        Hologram hologram = FancyHologramsPlugin.get().getHologramManager().create(holoData);
        FancyHologramsPlugin.get().getHologramManager().addHologram(hologram); 
        hologram.createHologram();
        hologram.showHologram(Bukkit.getOnlinePlayers());

        // Simpan customLines ke dalam ActiveCrate agar tidak perlu baca file YAML tiap detik
        ActiveCrate crate = new ActiveCrate(crateId, loc, bossName, hologram, 180, customLines);
        activeCrates.put(crateId, crate);
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
                        crate.hologram.deleteHologram();
                        iterator.remove();
                    } else {
                        // 3. Update Timer pada baris yang mengandung %timer%
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
                            crate.hologram.forceUpdate(); 
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public ActiveCrate getCrateAt(Location loc) {
        for (ActiveCrate crate : activeCrates.values()) {
            if (crate.location.getBlockX() == loc.getBlockX() &&
                crate.location.getBlockY() == loc.getBlockY() &&
                crate.location.getBlockZ() == loc.getBlockZ()) {
                return crate;
            }
        }
        return null;
    }

    public void forceClearAllCrates() {
        for (ActiveCrate crate : activeCrates.values()) {
            crate.location.getBlock().setType(Material.AIR);
            crate.hologram.deleteHologram();
        }
        activeCrates.clear();
    }

    public class ActiveCrate {
        public UUID id;
        public Location location;
        public String bossName;
        public Hologram hologram;
        public int timeLeft;
        public List<String> customLines; // Menyimpan format asli dari config
        public Set<UUID> claimedPlayers = new HashSet<>();

        public ActiveCrate(UUID id, Location loc, String boss, Hologram holo, int time, List<String> lines) {
            this.id = id;
            this.location = loc;
            this.bossName = boss;
            this.hologram = holo;
            this.timeLeft = time;
            this.customLines = lines;
        }
    }
}