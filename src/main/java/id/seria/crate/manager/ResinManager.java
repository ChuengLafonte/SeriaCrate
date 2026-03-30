package id.seria.crate.manager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import id.seria.crate.SeriaCrate;

public class ResinManager {
    private final SeriaCrate plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    private final Map<UUID, Integer> resinMap = new HashMap<>();
    private final Map<UUID, Long> lastCheckMap = new HashMap<>();

    public ResinManager(SeriaCrate plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/resin.yml");
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                resinMap.put(uuid, dataConfig.getInt("players." + key + ".amount"));
                lastCheckMap.put(uuid, dataConfig.getLong("players." + key + ".last-check"));
            }
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, Integer> entry : resinMap.entrySet()) {
            String path = "players." + entry.getKey().toString();
            dataConfig.set(path + ".amount", entry.getValue());
            dataConfig.set(path + ".last-check", lastCheckMap.getOrDefault(entry.getKey(), System.currentTimeMillis()));
        }
        try { dataConfig.save(dataFile); } catch (IOException ignored) {}
    }

    public void updateResin(UUID uuid) {
        int max = plugin.getConfigManager().getConfig().getInt("resin.max", 160);
        int regenMinutes = plugin.getConfigManager().getConfig().getInt("resin.regen-minutes", 5);

        // Jika player baru, set base stat
        if (!resinMap.containsKey(uuid)) resinMap.put(uuid, 0);
        if (!lastCheckMap.containsKey(uuid)) lastCheckMap.put(uuid, System.currentTimeMillis());

        int current = resinMap.get(uuid);

        if (current < max) {
            long lastCheck = lastCheckMap.get(uuid);
            long now = System.currentTimeMillis();
            long diffSeconds = (now - lastCheck) / 1000;
            long regenTimeSeconds = regenMinutes * 60L;

            if (diffSeconds >= regenTimeSeconds) {
                int intervals = (int) (diffSeconds / regenTimeSeconds);
                int newResin = current + intervals;

                if (newResin > max) {
                    resinMap.put(uuid, max);
                    lastCheckMap.put(uuid, now);
                } else {
                    resinMap.put(uuid, newResin);
                    long passedTimeSeconds = intervals * regenTimeSeconds;
                    // Sisakan detik sisa agar timer tetap akurat
                    long newLastCheck = now - ((diffSeconds - passedTimeSeconds) * 1000);
                    lastCheckMap.put(uuid, newLastCheck);
                }
            }
        } else {
            // Jika resin melebihi/sama dengan batas, stop timer regen
            lastCheckMap.put(uuid, System.currentTimeMillis());
        }
    }

    public int getResin(UUID uuid) {
        updateResin(uuid);
        return resinMap.getOrDefault(uuid, 0);
    }

    public boolean hasResin(UUID uuid, int amount) {
        return getResin(uuid) >= amount;
    }

    public boolean consumeResin(UUID uuid, int amount) {
        updateResin(uuid);
        int current = resinMap.getOrDefault(uuid, 0);
        if (current >= amount) {
            int max = plugin.getConfigManager().getConfig().getInt("resin.max", 160);
            // Mulai regen timer jika turun dari limit Max
            if (current >= max && (current - amount) < max) {
                lastCheckMap.put(uuid, System.currentTimeMillis());
            }
            resinMap.put(uuid, current - amount);
            return true;
        }
        return false;
    }

    public String getRegenTimeFormatted(UUID uuid) {
        updateResin(uuid);
        int max = plugin.getConfigManager().getConfig().getInt("resin.max", 160);
        if (resinMap.getOrDefault(uuid, 0) >= max) return "FULL";

        long lastCheck = lastCheckMap.getOrDefault(uuid, System.currentTimeMillis());
        long regenTimeSeconds = plugin.getConfigManager().getConfig().getInt("resin.regen-minutes", 5) * 60L;
        long diffSeconds = (System.currentTimeMillis() - lastCheck) / 1000;
        long remaining = regenTimeSeconds - diffSeconds;

        if (remaining <= 0) return "00m:00s";
        long m = remaining / 60;
        long s = remaining % 60;
        return String.format("%02dm:%02ds", m, s);
    }

    public void setResin(UUID uuid, int amount) {
        int max = plugin.getConfigManager().getConfig().getInt("resin.max", 160);
        int current = getResin(uuid);
        
        // HAPUS Math.min agar Admin bisa set resin hingga 500/160
        int finalAmount = Math.max(0, amount); 
        resinMap.put(uuid, finalAmount);
        
        if (current >= max && finalAmount < max) {
            lastCheckMap.put(uuid, System.currentTimeMillis());
        } else if (finalAmount >= max) {
            lastCheckMap.put(uuid, System.currentTimeMillis());
        }
        saveData();
    }

    public void addResin(UUID uuid, int amount) {
        setResin(uuid, getResin(uuid) + amount);
    }

    public void takeResin(UUID uuid, int amount) {
        setResin(uuid, getResin(uuid) - amount);
    }
}