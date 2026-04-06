package id.seria.crate.manager;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import id.seria.crate.SeriaCrate;
import id.seria.crate.util.TextUtils;
import net.kyori.adventure.text.Component;

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
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdir();

        // [TAMBAHAN] Auto-Cleanup: Hapus file rewards.yml usang di root folder
        File oldRewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (oldRewardsFile.exists()) {
            oldRewardsFile.delete();
            plugin.getLogger().info("File rewards.yml usang dihapus. Memindahkan sistem ke dalam folder rewards/...");
        }

        // Pastikan folder rewards/ dibuat terlebih dahulu
        rewardsFolder = new File(plugin.getDataFolder(), "rewards");
        if (!rewardsFolder.exists()) {
            rewardsFolder.mkdir();
        }

        saveResourceIfNotExists("config.yml");
        saveResourceIfNotExists("messages.yml");
        saveResourceIfNotExists("hologram.yml");
        saveResourceIfNotExists("gui.yml");
        saveResourceIfNotExists("command.yml");

        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        hologramFile = new File(plugin.getDataFolder(), "hologram.yml");
        guiFile = new File(plugin.getDataFolder(), "gui.yml");
        commandsFile = new File(plugin.getDataFolder(), "command.yml");

        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        hologram = YamlConfiguration.loadConfiguration(hologramFile);
        gui = YamlConfiguration.loadConfiguration(guiFile);
        commands = YamlConfiguration.loadConfiguration(commandsFile);
    }

    // Metode baru untuk menyalin file dari dalam file .jar plugin ke folder plugin di server
    private void saveResourceIfNotExists(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false); 
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("File default " + fileName + " tidak ditemukan di dalam JAR, membuat file kosong...");
                try { file.createNewFile(); } catch (IOException ex) { plugin.getLogger().severe("Gagal membuat file: " + file.getName()); }
            }
        }
    }

    public Component getMessage(String path) {
        String text = messages.getString(path);
        return text != null ? TextUtils.format(text) : Component.empty();
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getMessages() { return messages; }
    public FileConfiguration getHologram() { return hologram; }
    public FileConfiguration getGui() { return gui; }
    public FileConfiguration getCommands() { return commands; }
    public File getRewardsFolder() { return rewardsFolder; }

    public void saveHologram() { try { hologram.save(hologramFile); } catch (IOException ignored) {} }
    public void saveGui() { try { gui.save(guiFile); } catch (IOException ignored) {} }
    
    public void setupDefaultRewards() {
        File rewardsFolder = new File(plugin.getDataFolder(), "rewards");
        if (!rewardsFolder.exists()) {
            rewardsFolder.mkdirs();
        }
        
        String[] defaultRewards = {"malkor.yml", "shikanoko.yml", "skeleton.yml"};
        for (String fileName : defaultRewards) {
            File file = new File(rewardsFolder, fileName);
            if (!file.exists()) {
                plugin.saveResource("rewards/" + fileName, false);
            }
        }
    }
}