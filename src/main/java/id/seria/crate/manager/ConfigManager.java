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

        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        hologramFile = new File(plugin.getDataFolder(), "hologram.yml");
        guiFile = new File(plugin.getDataFolder(), "gui.yml");
        commandsFile = new File(plugin.getDataFolder(), "commands.yml");

        createFileIfNotExists(configFile);
        createFileIfNotExists(messagesFile);
        createFileIfNotExists(hologramFile);
        createFileIfNotExists(guiFile);
        createFileIfNotExists(commandsFile);

        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        hologram = YamlConfiguration.loadConfiguration(hologramFile);
        gui = YamlConfiguration.loadConfiguration(guiFile);
        commands = YamlConfiguration.loadConfiguration(commandsFile);

        rewardsFolder = new File(plugin.getDataFolder(), "rewards");
        if (!rewardsFolder.exists()) rewardsFolder.mkdir();
    }

    private void createFileIfNotExists(File file) {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { plugin.getLogger().severe("Gagal membuat file: " + file.getName()); }
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
            String[] defaultRewards = {"saok.yml", "skeleton.yml", "hallow.yml"};
            for (String fileName : defaultRewards) {
                File file = new File(rewardsFolder, fileName);
                if (!file.exists()) plugin.saveResource("rewards/" + fileName, false);
            }
        }
    }
}