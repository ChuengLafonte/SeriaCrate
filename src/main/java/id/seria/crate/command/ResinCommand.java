package id.seria.crate.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import id.seria.crate.SeriaCrate;

public class ResinCommand implements CommandExecutor, TabCompleter {

    private final SeriaCrate plugin;

    public ResinCommand(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getConfig().getString("settings.prefix", "&8[&eSeriaCrate&8] "));

        // ==================================
        // COMMAND: /resin
        // ==================================
        if (command.getName().equalsIgnoreCase("resin")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Hanya player yang bisa menggunakan command ini.");
                return true;
            }
            Player player = (Player) sender;
            int current = plugin.getResinManager().getResin(player.getUniqueId());
            int max = plugin.getConfigManager().getConfig().getInt("resin.max", 160);
            String timer = plugin.getResinManager().getRegenTimeFormatted(player.getUniqueId());

            player.sendMessage(prefix + "§6" + current + "§7/§6" + max + " 🔲 §7| §e" + timer);
            return true;
        }

        // ==================================
        // COMMAND: /resinadmin
        // ==================================
        if (command.getName().equalsIgnoreCase("resinadmin")) {
            if (!sender.hasPermission("seriacrate.admin")) {
                sender.sendMessage(prefix + "§cKamu tidak punya izin!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(prefix + "§cGunakan: /resinadmin <set|add|take|check> <player> [jumlah]");
                return true;
            }

            String action = args[0].toLowerCase();
            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                sender.sendMessage(prefix + "§cPlayer tidak ditemukan!");
                return true;
            }

            if (action.equals("check")) {
                int current = plugin.getResinManager().getResin(target.getUniqueId());
                int max = plugin.getConfigManager().getConfig().getInt("resin.max", 160);
                sender.sendMessage(prefix + "§b" + target.getName() + " §7memiliki §6" + current + "§7/§6" + max + " 🔲 Resin.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(prefix + "§cHarap masukkan jumlahnya!");
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(prefix + "§cJumlah harus berupa angka bulat!");
                return true;
            }

            int max = plugin.getConfigManager().getConfig().getInt("resin.max", 160);

            switch (action) {
                case "set":
                    plugin.getResinManager().setResin(target.getUniqueId(), amount);
                    sender.sendMessage(prefix + "§aBerhasil mengatur resin §b" + target.getName() + " §amenjadi §6" + amount);
                    break;
                case "add":
                    plugin.getResinManager().addResin(target.getUniqueId(), amount);
                    int currentAdd = plugin.getResinManager().getResin(target.getUniqueId());
                    sender.sendMessage(prefix + "§aBerhasil menambah §6" + amount + " §aresin ke §b" + target.getName());
                    target.sendMessage(prefix + "§6+" + amount + " 🔲 §7(§6" + currentAdd + "§7/§6" + max + "§7)");
                    break;
                case "take":
                    plugin.getResinManager().takeResin(target.getUniqueId(), amount);
                    sender.sendMessage(prefix + "§aBerhasil mengurangi §6" + amount + " §aresin dari §b" + target.getName());
                    break;
                default:
                    sender.sendMessage(prefix + "§cTipe aksi tidak diketahui. Gunakan: set, add, take, check.");
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("resinadmin") && sender.hasPermission("seriacrate.admin")) {
            if (args.length == 1) {
                completions.add("set"); completions.add("add");
                completions.add("take"); completions.add("check");
            } else if (args.length == 2) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            } else if (args.length == 3 && !args[0].equalsIgnoreCase("check")) {
                completions.add("10"); completions.add("20"); completions.add("50"); completions.add("160");
            }
        }
        return completions;
    }
}