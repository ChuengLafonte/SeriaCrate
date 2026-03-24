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

import id.seria.crate.engine.RollingEngine;
import id.seria.crate.gui.CrateGUI;
import id.seria.crate.gui.editor.EditorListener;
import id.seria.crate.gui.editor.EditorMenuManager;
import id.seria.crate.listener.BlockListener;
import id.seria.crate.listener.GUIListener;
import id.seria.crate.manager.ConfigManager;
import id.seria.crate.manager.CrateLocationManager;
import id.seria.crate.manager.RewardManager;
import id.seria.crate.manager.TemporaryCrateManager;
import id.seria.crate.model.Reward;
import net.milkbowl.vault.economy.Economy;

public class SeriaCrate extends JavaPlugin {

    private static SeriaCrate instance;
    private Economy econ = null;
    
    // Managers
    private ConfigManager configManager;
    private RewardManager rewardManager;
    private TemporaryCrateManager tempCrateManager;
    private CrateLocationManager locationManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. INIT CONFIG PERTAMA KALI (Sangat Penting urutannya!)
        configManager = new ConfigManager(this);
        configManager.setupDefaultRewards(); // <--- Tambahkan ini

        // 2. Setup Vault
        if (!setupEconomy()) {
            getLogger().severe("Vault tidak ditemukan! Hadiah uang mungkin gagal.");
        }

        // 3. Load Rewards dari Folder
        rewardManager = new RewardManager(this);
        rewardManager.loadRewards();

        // 4. Init Manager Lainnya
        tempCrateManager = new TemporaryCrateManager(this);
        locationManager = new CrateLocationManager(this);


        // 5. Register Listeners
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new EditorListener(), this);

        // 6. Register Tab Completer
        getCommand("seriacrate").setTabCompleter(this);

        getLogger().info("SeriaCrate (Modular Version) Aktif!");
    }

    @Override
    public void onDisable() {
        if (tempCrateManager != null) tempCrateManager.forceClearAllCrates();
        if (locationManager != null) locationManager.clearHolograms();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    // Getters
    public static SeriaCrate getInstance() { return instance; }
    public Economy getEconomy() { return econ; }
    public ConfigManager getConfigManager() { return configManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public TemporaryCrateManager getTempCrateManager() { return tempCrateManager; }
    public CrateLocationManager getLocationManager() { return locationManager; }

    // ==========================================
    // BAGIAN COMMAND EXECUTOR
    // ==========================================
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("seriacrate.admin")) return true;

        if (args.length == 0) {
            sender.sendMessage("§e/scrate spawn <boss> <x> <y> <z>");
            sender.sendMessage("§e/scrate editor");
            sender.sendMessage("§e/scrate givecrate <boss> [player]");
            sender.sendMessage("§e/scrate open <player> <boss> <tier>");
            sender.sendMessage("§e/scrate reload");
            return true;
        }

        // COMMAND: /scrate editor
        if (args[0].equalsIgnoreCase("editor") || args[0].equalsIgnoreCase("adminui")) {
            if (sender instanceof Player) {
                EditorMenuManager.openMainMenu((Player) sender);
            }
            return true;
        }

        // COMMAND: /scrate givecrate
        if (args[0].equalsIgnoreCase("givecrate") && args.length >= 2) {
            String boss = args[1].toLowerCase();
            Player target = (args.length >= 3) ? Bukkit.getPlayer(args[2]) : (Player) sender;
            if (target != null) {
                target.getInventory().addItem(id.seria.crate.util.ItemUtils.getCrateItem(boss));
                sender.sendMessage("§aBerhasil memberikan Crate Item " + boss.toUpperCase() + " ke " + target.getName());
            }
            return true;
        }

        // COMMAND: /scrate spawn
        if (args[0].equalsIgnoreCase("spawn") && args.length >= 5) {
            try {
                String boss = args[1].toLowerCase();
                double x = Double.parseDouble(args[2]);
                double y = Double.parseDouble(args[3]);
                double z = Double.parseDouble(args[4]);

                Location loc = new Location(((Player) sender).getWorld(), x, y, z);
                tempCrateManager.spawnTemporaryCrate(loc, boss);
                sender.sendMessage("§aBerhasil spawn Crate " + boss.toUpperCase() + " sementara!");
            } catch (Exception e) {
                sender.sendMessage("§cKoordinat harus berupa angka!");
            }
            return true;
        }

        // COMMAND: /scrate open
        if (args[0].equalsIgnoreCase("open") && args.length >= 4) {
            Player target = Bukkit.getPlayer(args[1]);
            String boss = args[2].toLowerCase();
            String tier = args[3].toLowerCase();

            if (target == null) return true;

            List<Reward> pool = rewardManager.getRewardsFor(boss, tier);
            if (pool.isEmpty()) {
                sender.sendMessage("§cHadiah kosong di tier ini!");
                return true;
            }

            Inventory inv = CrateGUI.createOpeningGUI(boss, tier);
            target.openInventory(inv);
            new RollingEngine(this).startRolling(target, inv, pool);
            return true;
        }

        // COMMAND: /scrate reload
        if (args[0].equalsIgnoreCase("reload")) {
            configManager.loadAllConfigs();
            rewardManager.loadRewards();
            sender.sendMessage("§a[SeriaCrate] Config & Rewards Reloaded!");
            return true;
        }

        return true;
    }

    // ==========================================
    // BAGIAN TAB COMPLETER
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
            if (args[0].equalsIgnoreCase("spawn")) {
                completions.add("<x>");
                if (sender instanceof Player) completions.add(String.valueOf(((Player) sender).getLocation().getBlockX()));
            } else if (args[0].equalsIgnoreCase("open")) {
                completions.addAll(bosses);
            } else if (args[0].equalsIgnoreCase("givecrate")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("open")) completions.addAll(tiers);
            else if (args[0].equalsIgnoreCase("spawn")) completions.add("<y>");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("spawn")) {
            completions.add("<z>");
        }

        String currentArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList());
    }
}