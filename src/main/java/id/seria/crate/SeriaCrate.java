package id.seria.crate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import id.seria.crate.command.ResinCommand;
import id.seria.crate.engine.RollingEngine;
import id.seria.crate.gui.CrateGUI;
import id.seria.crate.gui.editor.EditorListener;
import id.seria.crate.gui.editor.EditorMenuManager;
import id.seria.crate.listener.BlockListener;
import id.seria.crate.listener.GUIListener;
import id.seria.crate.listener.ResinItemListener;
import id.seria.crate.manager.ConfigManager;
import id.seria.crate.manager.CrateLocationManager;
import id.seria.crate.manager.ResinManager;
import id.seria.crate.manager.RewardManager;
import id.seria.crate.manager.TemporaryCrateManager;
import id.seria.crate.model.Reward;
import id.seria.crate.util.ItemUtils;
import id.seria.crate.util.TextUtils;
import net.milkbowl.vault.economy.Economy;

public class SeriaCrate extends JavaPlugin {

    private static SeriaCrate instance;
    private Economy econ = null;

    private ConfigManager configManager;
    private RewardManager rewardManager;
    private TemporaryCrateManager tempCrateManager;
    private CrateLocationManager locationManager;
    private ResinManager resinManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.setupDefaultRewards();

        this.resinManager = new ResinManager(this);

        if (!setupEconomy()) {
            getLogger().severe("Vault tidak ditemukan! Hadiah uang mungkin gagal.");
        }

        rewardManager = new RewardManager(this);
        rewardManager.loadRewards();

        tempCrateManager = new TemporaryCrateManager(this);
        locationManager = new CrateLocationManager(this);

        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new EditorListener(), this);
        getServer().getPluginManager().registerEvents(new ResinItemListener(this), this);

        getCommand("seriacrate").setTabCompleter(this);

        ResinCommand resinCmd = new ResinCommand(this);
        if (getCommand("resin") != null) {
            getCommand("resin").setExecutor(resinCmd);
        }
        if (getCommand("resinadmin") != null) {
            getCommand("resinadmin").setExecutor(resinCmd);
            getCommand("resinadmin").setTabCompleter(resinCmd);
        }

        // Hook ke PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new id.seria.crate.placeholder.SeriaCratePlaceholder(this).register();
            getLogger().info("PlaceholderAPI terdeteksi! Placeholder SeriaCrate aktif.");
        } else {
            getLogger().warning("PlaceholderAPI tidak ditemukan, fitur placeholder tidak akan bekerja.");
        }
        
        getLogger().info("SeriaCrate (Modular Version) Aktif!");
    }

    @Override
    public void onDisable() {
        if (tempCrateManager != null) tempCrateManager.forceClearAllCrates();
        if (locationManager != null) locationManager.clearHolograms();
        if (this.resinManager != null) this.resinManager.saveData();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    public static SeriaCrate getInstance() { return instance; }
    public Economy getEconomy() { return econ; }
    public ConfigManager getConfigManager() { return configManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public TemporaryCrateManager getTempCrateManager() { return tempCrateManager; }
    public CrateLocationManager getLocationManager() { return locationManager; }
    public ResinManager getResinManager() { return this.resinManager; }

    // ==========================================
    // COMMAND EXECUTOR
    // ==========================================
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("seriacrate.admin")) return true;

        if (args.length == 0) {
            sender.sendMessage(TextUtils.format("<yellow>/scrate spawn <boss>")); // Diubah agar lebih ringkas
            sender.sendMessage(TextUtils.format("<yellow>/scrate editor"));
            sender.sendMessage(TextUtils.format("<yellow>/scrate givecrate <boss> [player]"));
            sender.sendMessage(TextUtils.format("<yellow>/scrate open <player> <boss> <tier>"));
            sender.sendMessage(TextUtils.format("<yellow>/scrate reload"));
            return true;
        }

        // /scrate editor
        if (args[0].equalsIgnoreCase("editor") || args[0].equalsIgnoreCase("adminui")) {
            if (sender instanceof Player) {
                EditorMenuManager.openMainMenu((Player) sender);
            }
            return true;
        }

        // /scrate givecrate
        if (args[0].equalsIgnoreCase("givecrate") && args.length >= 2) {
            String boss = args[1].toLowerCase();
            Player target = (args.length >= 3) ? Bukkit.getPlayer(args[2]) : (Player) sender;
            if (target != null) {
                target.getInventory().addItem(ItemUtils.getCrateItem(boss));
                sender.sendMessage(TextUtils.format("<green>Berhasil memberikan Crate Item <bold>"
                        + boss.toUpperCase() + "</bold> ke " + target.getName()));
            }
            return true;
        }

        // /scrate spawn <boss> (Menggunakan lokasi player otomatis)
        if (args[0].equalsIgnoreCase("spawn") && args.length >= 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(TextUtils.format("<red>Command ini hanya bisa digunakan oleh player di dalam game!"));
                return true;
            }
            
            Player player = (Player) sender;
            String boss = args[1].toLowerCase();
            
            // Mengambil koordinat blok tempat player berdiri
            Location loc = player.getLocation().getBlock().getLocation();
            
            tempCrateManager.spawnTemporaryCrate(loc, boss);
            sender.sendMessage(TextUtils.format("<#00FF00>Berhasil spawn Crate <bold>"
                    + boss.toUpperCase() + "</bold> sementara di lokasimu!"));
            return true;
        }

        // /scrate open
        if (args[0].equalsIgnoreCase("open") && args.length >= 4) {
            Player target = Bukkit.getPlayer(args[1]);
            String boss = args[2].toLowerCase();
            String tier = args[3].toLowerCase();

            if (target == null) return true;

            List<Reward> pool = rewardManager.getRewardsFor(boss, tier);
            if (pool.isEmpty()) {
                sender.sendMessage(TextUtils.format("<red>Hadiah kosong di tier ini!"));
                return true;
            }

            Inventory inv = CrateGUI.createOpeningGUI(boss, tier);
            target.openInventory(inv);
            new RollingEngine(this).startRolling(target, inv, pool, boss, tier);
            return true;
        }

        // /scrate reload
        if (args[0].equalsIgnoreCase("reload")) {
            configManager.loadAllConfigs();
            rewardManager.loadRewards();
            sender.sendMessage(TextUtils.format("<green>[SeriaCrate] Config, Messages, GUI, & Rewards Reloaded!"));
            return true;
        }

        return true;
    }

    // ==========================================
    // TAB COMPLETER
    // ==========================================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = Arrays.asList("spawn", "open", "reload", "editor", "givecrate");
        List<String> bosses = Arrays.asList("saok", "skeleton", "hallow");
        List<String> tiers = Arrays.asList("s", "a", "b", "c", "d");

        if (!sender.hasPermission("seriacrate.admin")) return completions;

        if (args.length == 1) {
            completions.addAll(commands);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("givecrate")) {
                completions.addAll(bosses);
            } else if (args[0].equalsIgnoreCase("open")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("open")) {
                completions.addAll(bosses);
            } else if (args[0].equalsIgnoreCase("givecrate")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            }
            // Argumen X, Y, Z dihapus karena sekarang otomatis
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("open")) completions.addAll(tiers);
        }

        String currentArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList());
    }
}