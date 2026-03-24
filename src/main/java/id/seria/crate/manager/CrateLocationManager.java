package id.seria.crate.manager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.TextDisplay;

import id.seria.crate.SeriaCrate;

public class CrateLocationManager {

    private final SeriaCrate plugin;
    private final File file;
    private FileConfiguration config;
    
    // Map untuk menyimpan Lokasi Blok -> Nama Boss (Crate)
    private final Map<Location, String> crateLocations = new HashMap<>();
    // Menyimpan referensi entitas TextDisplay agar bisa dihapus saat server mati
    private final Map<Location, TextDisplay> activeHolograms = new HashMap<>();

    public CrateLocationManager(SeriaCrate plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "locations.yml");
        loadLocations();
    }

    public void loadLocations() {
        crateLocations.clear();
        clearHolograms(); // Bersihkan hologram lama jika di-reload
        
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.getConfigurationSection("crates") != null) {
            for (String key : config.getConfigurationSection("crates").getKeys(false)) {
                String worldName = config.getString("crates." + key + ".world");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = config.getDouble("crates." + key + ".x");
                    double y = config.getDouble("crates." + key + ".y");
                    double z = config.getDouble("crates." + key + ".z");
                    String boss = config.getString("crates." + key + ".boss");
                    
                    Location loc = new Location(world, x, y, z);
                    crateLocations.put(loc, boss);
                    spawnHologram(loc, boss);
                }
            }
        }
    }

    public void setCrateLocation(Location loc, String boss) {
        crateLocations.put(loc, boss);
        spawnHologram(loc, boss);
        
        // Buat ID unik berdasarkan koordinat
        String key = loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        config.set("crates." + key + ".world", loc.getWorld().getName());
        config.set("crates." + key + ".x", loc.getX());
        config.set("crates." + key + ".y", loc.getY());
        config.set("crates." + key + ".z", loc.getZ());
        config.set("crates." + key + ".boss", boss);
        
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    // Di dalam fungsi spawnHologram:
    private void spawnHologram(Location loc, String boss) {
        Location holoLoc = loc.clone().add(0.5, 1.2, 0.5);
        org.bukkit.entity.TextDisplay display = holoLoc.getWorld().spawn(holoLoc, org.bukkit.entity.TextDisplay.class);
        
        org.bukkit.configuration.file.FileConfiguration holoConfig = plugin.getConfigManager().getHologram();
        List<String> lines = holoConfig.getStringList("holograms." + boss);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(org.bukkit.ChatColor.translateAlternateColorCodes('&', line.replace("%timer%", "Permanen"))).append("\n");
        }
        
        display.setText(sb.toString().trim());
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.setDefaultBackground(false);
        activeHolograms.put(loc, display);
    }

    public void clearHolograms() {
        for (TextDisplay display : activeHolograms.values()) {
            if (display != null && display.isValid()) display.remove();
        }
        activeHolograms.clear();
    }

    public void removeCrateLocation(Location loc) {
        crateLocations.remove(loc);
        TextDisplay display = activeHolograms.remove(loc);
        if (display != null && display.isValid()) display.remove();

        String key = loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        config.set("crates." + key, null);
        try { config.save(file); } catch (IOException ignored) {}
    }

    public String getCrateAt(Location loc) {
        return crateLocations.get(loc);
    }
}