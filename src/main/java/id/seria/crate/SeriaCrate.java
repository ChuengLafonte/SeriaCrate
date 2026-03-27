package id.seria.crate;

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
import id.seria.crate.util.ItemUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SeriaCrate extends JavaPlugin {
   private static SeriaCrate instance;
   private Economy econ = null;
   private ConfigManager configManager;
   private RewardManager rewardManager;
   private TemporaryCrateManager tempCrateManager;
   private CrateLocationManager locationManager;

   public void onEnable() {
      instance = this;
      this.configManager = new ConfigManager(this);
      this.configManager.setupDefaultRewards();
      if (!this.setupEconomy()) {
         this.getLogger().severe("Vault tidak ditemukan! Hadiah uang mungkin gagal.");
      }

      this.rewardManager = new RewardManager(this);
      this.rewardManager.loadRewards();
      this.tempCrateManager = new TemporaryCrateManager(this);
      this.locationManager = new CrateLocationManager(this);
      this.getServer().getPluginManager().registerEvents(new BlockListener(this), this);
      this.getServer().getPluginManager().registerEvents(new GUIListener(this), this);
      this.getServer().getPluginManager().registerEvents(new EditorListener(), this);
      this.getCommand("seriacrate").setTabCompleter(this);
      this.getLogger().info("SeriaCrate (Modular Version) Aktif!");
   }

   public void onDisable() {
      if (this.tempCrateManager != null) {
         this.tempCrateManager.forceClearAllCrates();
      }

      if (this.locationManager != null) {
         this.locationManager.clearHolograms();
      }

   }

   private boolean setupEconomy() {
      if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
         return false;
      } else {
         RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
         if (rsp == null) {
            return false;
         } else {
            this.econ = (Economy)rsp.getProvider();
            return this.econ != null;
         }
      }
   }

   public static SeriaCrate getInstance() {
      return instance;
   }

   public Economy getEconomy() {
      return this.econ;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public RewardManager getRewardManager() {
      return this.rewardManager;
   }

   public TemporaryCrateManager getTempCrateManager() {
      return this.tempCrateManager;
   }

   public CrateLocationManager getLocationManager() {
      return this.locationManager;
   }

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
                id.seria.crate.gui.editor.EditorMenuManager.openMainMenu((Player) sender);
            }
            return true;
        }

        // COMMAND: /scrate givecrate
        if (args[0].equalsIgnoreCase("givecrate") && args.length >= 2) {
            String targetBoss = args[1].toLowerCase();
            Player target = (args.length >= 3) ? Bukkit.getPlayer(args[2]) : (Player) sender;

            if (target != null) {
                target.getInventory().addItem(id.seria.crate.util.ItemUtils.getCrateItem(targetBoss));
                sender.sendMessage("§aBerhasil memberikan Crate Item " + targetBoss.toUpperCase() + " ke " + target.getName());
            } else {
                sender.sendMessage("§cPemain tidak ditemukan.");
            }
            return true;
        }

        // COMMAND: /scrate spawn
        if (args[0].equalsIgnoreCase("spawn") && args.length >= 5) {
            try {
                String spawnBoss = args[1].toLowerCase();
                double x = Double.parseDouble(args[2]);
                double y = Double.parseDouble(args[3]);
                double z = Double.parseDouble(args[4]);

                Location loc = new Location(((Player) sender).getWorld(), x, y, z);
                tempCrateManager.spawnTemporaryCrate(loc, spawnBoss);
                sender.sendMessage("§aBerhasil spawn Crate " + spawnBoss.toUpperCase() + " sementara!");
            } catch (Exception e) {
                sender.sendMessage("§cKoordinat harus berupa angka!");
            }
            return true;
        }

        // COMMAND: /scrate open
        if (args[0].equalsIgnoreCase("open") && args.length >= 4) {
            Player target = Bukkit.getPlayer(args[1]);
            String openBoss = args[2].toLowerCase();
            String tier = args[3].toLowerCase();

            if (target == null) return true;

            List<Reward> pool = rewardManager.getRewardsFor(openBoss, tier);
            if (pool.isEmpty()) {
                sender.sendMessage("§cHadiah kosong di tier ini!");
                return true;
            }

            Inventory inv = id.seria.crate.gui.CrateGUI.createOpeningGUI(openBoss, tier);
            target.openInventory(inv);
            new id.seria.crate.engine.RollingEngine(this).startRolling(target, inv, pool);
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

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      List<String> commands = Arrays.asList("spawn", "open", "reload", "editor", "givecrate");
      List<String> bosses = Arrays.asList("saok", "skeleton", "hallow");
      List<String> tiers = Arrays.asList("s", "a", "b", "c", "d");
      if (!sender.hasPermission("seriacrate.admin")) {
         return completions;
      } else {
         if (args.length == 1) {
            completions.addAll(commands);
         } else {
            Iterator var9;
            Player p;
            if (args.length == 2) {
               if (!args[0].equalsIgnoreCase("spawn") && !args[0].equalsIgnoreCase("givecrate")) {
                  if (args[0].equalsIgnoreCase("open")) {
                     var9 = Bukkit.getOnlinePlayers().iterator();

                     while(var9.hasNext()) {
                        p = (Player)var9.next();
                        completions.add(p.getName());
                     }
                  }
               } else {
                  completions.addAll(bosses);
               }
            } else if (args.length == 3) {
               if (args[0].equalsIgnoreCase("spawn")) {
                  completions.add("<x>");
                  if (sender instanceof Player) {
                     completions.add(String.valueOf(((Player)sender).getLocation().getBlockX()));
                  }
               } else if (args[0].equalsIgnoreCase("open")) {
                  completions.addAll(bosses);
               } else if (args[0].equalsIgnoreCase("givecrate")) {
                  var9 = Bukkit.getOnlinePlayers().iterator();

                  while(var9.hasNext()) {
                     p = (Player)var9.next();
                     completions.add(p.getName());
                  }
               }
            } else if (args.length == 4) {
               if (args[0].equalsIgnoreCase("open")) {
                  completions.addAll(tiers);
               } else if (args[0].equalsIgnoreCase("spawn")) {
                  completions.add("<y>");
               }
            } else if (args.length == 5 && args[0].equalsIgnoreCase("spawn")) {
               completions.add("<z>");
            }
         }

         String currentArg = args[args.length - 1].toLowerCase();
         return (List)completions.stream().filter((s) -> {
            return s.toLowerCase().startsWith(currentArg);
         }).collect(Collectors.toList());
      }
   }
}
