package id.seria.crate.manager;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import id.seria.crate.SeriaCrate;

public class ConfigManager {

    private final SeriaCrate plugin;
    
    private File configFile, messagesFile, hologramFile, guiFile, commandsFile;
    private FileConfiguration config, messages, hologram, gui, commands;
    
    private File rewardsFolder;

    public ConfigManager(SeriaCrate plugin) {
        this.plugin = plugin;
        loadAllConfigs();
    }

    public void loadAllConfigs() {
        // Buat folder utama jika belum ada
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        // Setup File Global
        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        hologramFile = new File(plugin.getDataFolder(), "hologram.yml");
        guiFile = new File(plugin.getDataFolder(), "gui.yml");
        commandsFile = new File(plugin.getDataFolder(), "commands.yml");

        // Bikin file jika belum ada (Bisa diganti saveResource jika ada file default di src/main/resources)
        createFileIfNotExists(configFile);
        createFileIfNotExists(messagesFile);
        createFileIfNotExists(hologramFile);
        createFileIfNotExists(guiFile);
        createFileIfNotExists(commandsFile);

        // Load config ke memory
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        hologram = YamlConfiguration.loadConfiguration(hologramFile);
        gui = YamlConfiguration.loadConfiguration(guiFile);
        commands = YamlConfiguration.loadConfiguration(commandsFile);

        // Setup Folder Rewards
        rewardsFolder = new File(plugin.getDataFolder(), "rewards");
        if (!rewardsFolder.exists()) {
            rewardsFolder.mkdir();
        }
    }

    private void createFileIfNotExists(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Gagal membuat file: " + file.getName());
            }
        }
    }

    // ================= GETTER =================
    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getMessages() { return messages; }
    public FileConfiguration getHologram() { return hologram; }
    public FileConfiguration getGui() { return gui; }
    public FileConfiguration getCommands() { return commands; }
    public File getRewardsFolder() { return rewardsFolder; }

    // ================= SAVER =================
    public void saveHologram() {
        try { hologram.save(hologramFile); } catch (IOException ignored) {}
    }
    
    public void saveGui() {
        try { gui.save(guiFile); } catch (IOException ignored) {}
    }
    // Di dalam ConfigManager.java, tambahkan fungsi ini:
    public void setupDefaultRewards() {
        File rewardsFolder = new File(plugin.getDataFolder(), "rewards");
        if (!rewardsFolder.exists()) {
            rewardsFolder.mkdirs();
            
            // List file default yang ingin kamu keluarkan dari JAR ke Server
            String[] defaultRewards = {"saok.yml", "skeleton.yml", "hallow.yml"};
            
            for (String fileName : defaultRewards) {
                File file = new File(rewardsFolder, fileName);
                if (!file.exists()) {
                    // Perintah untuk meng-copy dari src/main/resources/rewards/ ke folder server
                    plugin.saveResource("rewards/" + fileName, false);
                }
            }
        }
    }
}