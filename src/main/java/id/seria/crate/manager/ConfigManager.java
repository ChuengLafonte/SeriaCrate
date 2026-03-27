package id.seria.crate.manager;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import id.seria.crate.SeriaCrate;

public class ConfigManager {

    private final SeriaCrate plugin;
    
    private File configFile, messagesFile, hologramFile, guiFile;
    private FileConfiguration config, messages, hologram, gui;
    
    private File rewardsFolder;

    public ConfigManager(SeriaCrate plugin) {
        this.plugin = plugin;
        loadAllConfigs();
        setupDefaultRewards(); // Panggil fungsi auto-copy saat start!
    }

    public void loadAllConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        // 1. Ekstrak file utama dari dalam JAR ke Server jika belum ada
        copyResourceIfNotExists("config.yml");
        copyResourceIfNotExists("messages.yml");
        copyResourceIfNotExists("hologram.yml");
        copyResourceIfNotExists("gui.yml");

        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        hologramFile = new File(plugin.getDataFolder(), "hologram.yml");
        guiFile = new File(plugin.getDataFolder(), "gui.yml");

        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        hologram = YamlConfiguration.loadConfiguration(hologramFile);
        gui = YamlConfiguration.loadConfiguration(guiFile);

        // 2. Setup Folder Rewards
        rewardsFolder = new File(plugin.getDataFolder(), "rewards");
        if (!rewardsFolder.exists()) {
            rewardsFolder.mkdirs();
        }
    }

    // FUNGSI BARU: Memastikan setiap isi folder terekstrak dengan benar!
    public void setupDefaultRewards() {
        String[] defaultRewards = {"saok.yml", "skeleton.yml", "hallow.yml"};
        
        for (String fileName : defaultRewards) {
            File file = new File(rewardsFolder, fileName);
            // Pengecekan ada di level FILE, bukan FOLDER
            if (!file.exists()) {
                try {
                    plugin.saveResource("rewards/" + fileName, false);
                    plugin.getLogger().info("Berhasil meng-copy default crate: " + fileName);
                } catch (Exception e) {
                    plugin.getLogger().warning("Gagal menemukan " + fileName + " di dalam JAR (Resources).");
                }
            }
        }
    }

    // Helper: Meng-copy file resources standar dengan aman
    private void copyResourceIfNotExists(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false);
            } catch (Exception e) {
                // Jika file tidak ada di src/main/resources, buat file kosong
                try { file.createNewFile(); } catch (IOException ignored) {}
            }
        }
    }

    // ================= GETTER =================
    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getMessages() { return messages; }
    public FileConfiguration getHologram() { return hologram; }
    public FileConfiguration getGui() { return gui; }
    public File getRewardsFolder() { return rewardsFolder; }

    // ================= SAVER =================
    public void saveHologram() {
        try { hologram.save(hologramFile); } catch (IOException ignored) {}
    }
    
    public void saveGui() {
        try { gui.save(guiFile); } catch (IOException ignored) {}
    }
}