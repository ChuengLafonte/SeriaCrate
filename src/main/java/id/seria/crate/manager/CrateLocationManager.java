package id.seria.crate.manager;

import id.seria.crate.SeriaCrate;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;

public class CrateLocationManager {
   private final SeriaCrate plugin;
   private final File file;
   private FileConfiguration config;
   private final Map<Location, String> crateLocations = new HashMap();
   private final Map<Location, TextDisplay> activeHolograms = new HashMap();

   public CrateLocationManager(SeriaCrate plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "locations.yml");
      this.loadLocations();
   }

   public void loadLocations() {
      this.crateLocations.clear();
      this.clearHolograms();
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException var13) {
         }
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);
      if (this.config.getConfigurationSection("crates") != null) {
         Iterator var1 = this.config.getConfigurationSection("crates").getKeys(false).iterator();

         while(var1.hasNext()) {
            String key = (String)var1.next();
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
      int var10000 = loc.getBlockX();
      String key = var10000 + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
      this.config.set("crates." + key + ".world", loc.getWorld().getName());
      this.config.set("crates." + key + ".x", loc.getX());
      this.config.set("crates." + key + ".y", loc.getY());
      this.config.set("crates." + key + ".z", loc.getZ());
      this.config.set("crates." + key + ".boss", boss);

      try {
         this.config.save(this.file);
      } catch (IOException var5) {
         var5.printStackTrace();
      }

   }

   private void spawnHologram(Location loc, String boss) {
      Location holoLoc = loc.clone().add(0.5D, 1.2D, 0.5D);
      TextDisplay display = (TextDisplay)holoLoc.getWorld().spawn(holoLoc, TextDisplay.class);
      FileConfiguration holoConfig = this.plugin.getConfigManager().getHologram();
      List<String> lines = holoConfig.getStringList("holograms." + boss);
      StringBuilder sb = new StringBuilder();
      Iterator var8 = lines.iterator();

      while(var8.hasNext()) {
         String line = (String)var8.next();
         sb.append(ChatColor.translateAlternateColorCodes('&', line.replace("%timer%", "Permanen"))).append("\n");
      }

      display.setText(sb.toString().trim());
      display.setBillboard(Billboard.CENTER);
      display.setDefaultBackground(false);
      this.activeHolograms.put(loc, display);
   }

   public void clearHolograms() {
      Iterator var1 = this.activeHolograms.values().iterator();

      while(var1.hasNext()) {
         TextDisplay display = (TextDisplay)var1.next();
         if (display != null && display.isValid()) {
            display.remove();
         }
      }

      this.activeHolograms.clear();
   }

   public void removeCrateLocation(Location loc) {
      this.crateLocations.remove(loc);
      TextDisplay display = (TextDisplay)this.activeHolograms.remove(loc);
      if (display != null && display.isValid()) {
         display.remove();
      }

      int var10000 = loc.getBlockX();
      String key = var10000 + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
      this.config.set("crates." + key, (Object)null);

      try {
         this.config.save(this.file);
      } catch (IOException var5) {
      }

   }

   public String getCrateAt(Location loc) {
      return (String)this.crateLocations.get(loc);
   }
}
