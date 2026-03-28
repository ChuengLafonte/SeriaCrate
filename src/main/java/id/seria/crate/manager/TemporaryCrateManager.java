package id.seria.crate.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
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
   private final Map<UUID, TemporaryCrateManager.ActiveCrate> activeCrates = new HashMap();

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
      
      // [PERBAIKAN] Cek Hologram Toggle & Durasi
      boolean useHolo = config.getBoolean("crate-settings.hologram", true);
      int duration = config.getInt("crate-settings.duration", 180); // Mengambil durasi dari Editor UI
      
      Hologram hologram = null;
      List<String> customLines = new ArrayList<>();

      if (useHolo) {
          FileConfiguration holoConfig = this.plugin.getConfigManager().getHologram();
          customLines = holoConfig.getStringList("holograms." + bossName);
          if (customLines.isEmpty()) {
             customLines = Arrays.asList("&e&l" + bossName.toUpperCase() + " CRATE", "&7Menghilang dalam: &c%timer%");
          }

          String holoName = "crate_" + crateId.toString().substring(0, 8);
          Location holoLoc = loc.clone().add(0.5D, 2.2D, 0.5D);
          TextHologramData holoData = new TextHologramData(holoName, holoLoc);
          List<String> initialText = new ArrayList<>();
          
          for(String line : customLines) {
             initialText.add(ChatColor.translateAlternateColorCodes('&', line.replace("%timer%", "Menghitung...")));
          }

          holoData.setText(initialText);
          hologram = FancyHologramsPlugin.get().getHologramManager().create(holoData);
          FancyHologramsPlugin.get().getHologramManager().addHologram(hologram);
          hologram.createHologram();
          hologram.showHologram(Bukkit.getOnlinePlayers());
      }

      TemporaryCrateManager.ActiveCrate crate = new ActiveCrate(crateId, loc, bossName, hologram, duration, customLines);
      this.activeCrates.put(crateId, crate);
   }

   private void startHologramUpdater() {
      (new BukkitRunnable() {
         public void run() {
            Iterator iterator = TemporaryCrateManager.this.activeCrates.entrySet().iterator();

            while(true) {
               while(iterator.hasNext()) {
                  Entry<UUID, TemporaryCrateManager.ActiveCrate> entry = (Entry)iterator.next();
                  TemporaryCrateManager.ActiveCrate crate = (TemporaryCrateManager.ActiveCrate)entry.getValue();
                  --crate.timeLeft;
                  if (crate.timeLeft <= 0) {
                     crate.location.getBlock().setType(Material.AIR);
                     if (crate.hologram != null) crate.hologram.deleteHologram(); // <- Perbaikan disini
                     iterator.remove();
                  } else {
                     if (crate.hologram != null) { // <- Perbaikan disini
                        int min = crate.timeLeft / 60;
                        int sec = crate.timeLeft % 60;
                     String timeStr = String.format("%02d:%02d", min, sec);
                     List<String> updatedLines = new ArrayList();
                     Iterator var8 = crate.customLines.iterator();

                     while(var8.hasNext()) {
                        String line = (String)var8.next();
                        updatedLines.add(ChatColor.translateAlternateColorCodes('&', line.replace("%timer%", timeStr)));
                     }

                     if (crate.hologram.getData() instanceof TextHologramData) {
                        TextHologramData textData = (TextHologramData)crate.hologram.getData();
                        textData.setText(updatedLines);
                        crate.hologram.forceUpdate();
                     }
                  }
               }

               return;
            }}
         }
      }).runTaskTimer(this.plugin, 0L, 20L);
   }

   public TemporaryCrateManager.ActiveCrate getCrateAt(Location loc) {
      Iterator var2 = this.activeCrates.values().iterator();

      TemporaryCrateManager.ActiveCrate crate;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         crate = (TemporaryCrateManager.ActiveCrate)var2.next();
      } while(crate.location.getBlockX() != loc.getBlockX() || crate.location.getBlockY() != loc.getBlockY() || crate.location.getBlockZ() != loc.getBlockZ());

      return crate;
   }

   public void forceClearAllCrates() {
      Iterator var1 = this.activeCrates.values().iterator();

      while(var1.hasNext()) {
         TemporaryCrateManager.ActiveCrate crate = (TemporaryCrateManager.ActiveCrate)var1.next();
         crate.location.getBlock().setType(Material.AIR);
         crate.hologram.deleteHologram();
      }

      this.activeCrates.clear();
   }

   public static class ActiveCrate {
        public UUID id;
        public Location location;
        public String bossName;
        public de.oliver.fancyholograms.api.hologram.Hologram hologram;
        public int timeLeft;
        public List<String> customLines; 
        public Set<UUID> claimedPlayers = new HashSet<>();

        public ActiveCrate(UUID id, Location location, String bossName, de.oliver.fancyholograms.api.hologram.Hologram hologram, int timeLeft, List<String> customLines) {
            this.id = id;
            this.location = location;
            this.bossName = bossName;
            this.hologram = hologram;
            this.timeLeft = timeLeft;
            this.customLines = customLines;
        }
    }
}
