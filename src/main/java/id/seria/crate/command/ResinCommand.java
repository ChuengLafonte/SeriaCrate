package id.seria.crate.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import id.seria.crate.SeriaCrate;
import id.seria.crate.util.TextUtils;
import net.kyori.adventure.text.Component;

public class ResinCommand implements CommandExecutor, TabCompleter {

    private final SeriaCrate plugin;

    public ResinCommand(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Component prefix = plugin.getConfigManager().getMessage("prefix");

        // ==================================
        // COMMAND: /resin
        // ==================================
        if (command.getName().equalsIgnoreCase("resin")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Hanya player yang bisa menggunakan command ini."));
                return true;
            }
            Player player = (Player) sender;
            int current = plugin.getResinManager().getResin(player.getUniqueId());
            int max = plugin.getConfigManager().getConfig().getInt("resin.max", 160);
            String timer = plugin.getResinManager().getRegenTimeFormatted(player.getUniqueId());
            player.sendMessage(prefix.append(TextUtils.format("§6" + current + "§7/§6" + max + " 🔲 §7| §e" + timer)));
            return true;
        }

        // ==================================
        // COMMAND: /resinadmin
        // ==================================
        if (command.getName().equalsIgnoreCase("resinadmin")) {
            if (!sender.hasPermission("seriacrate.admin")) {
                sender.sendMessage(prefix.append(TextUtils.format("§cKamu tidak punya izin!")));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(prefix.append(TextUtils.format("§cGunakan: /resinadmin <set|add|take|check> <player> [jumlah]")));
                return true;
            }

            String action     = args[0].toLowerCase();
            String targetName = args[1];

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) target = Bukkit.getPlayer(targetName);

            OfflinePlayer offlineTarget = null;
            if (target == null) {
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    if (op.getName() != null && op.getName().equalsIgnoreCase(targetName)) {
                        offlineTarget = op;
                        break;
                    }
                }
            }

            if (target == null && offlineTarget == null) {
                sender.sendMessage(prefix.append(TextUtils.format("§cPlayer §e" + targetName + "§c tidak ditemukan!")));
                return true;
            }

            UUID    targetUUID  = target != null ? target.getUniqueId() : offlineTarget.getUniqueId();
            String  displayName = target != null ? target.getName() : offlineTarget.getName();
            boolean isOnline    = target != null;

            if (action.equals("check")) {
                int current = plugin.getResinManager().getResin(targetUUID);
                int max = plugin.getConfigManager().getConfig().getInt("resin.max", 160);
                String status = isOnline ? "§a[Online] " : "§8[Offline] ";
                sender.sendMessage(prefix.append(TextUtils.format(status + "§b" + displayName
                        + " §7memiliki §6" + current + "§7/§6" + max + " 🔲 Resin.")));
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(prefix.append(TextUtils.format("§cHarap masukkan jumlahnya!")));
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(prefix.append(TextUtils.format("§cJumlah harus berupa angka bulat!")));
                return true;
            }

            int max = plugin.getConfigManager().getConfig().getInt("resin.max", 160);

            switch (action) {
                case "set":
                    plugin.getResinManager().setResin(targetUUID, amount);
                    sender.sendMessage(prefix.append(TextUtils.format("§aBerhasil mengatur resin §b" + displayName
                            + " §amenjadi §6" + amount + (isOnline ? "" : " §8(offline — tersimpan)"))));
                    if (isOnline) target.sendMessage(prefix.append(TextUtils.format(
                            "§eAdmin mengatur resin kamu menjadi §6" + amount + " 🔲§e.")));
                    break;

                case "add":
                    plugin.getResinManager().addResin(targetUUID, amount);
                    int currentAdd = plugin.getResinManager().getResin(targetUUID);
                    sender.sendMessage(prefix.append(TextUtils.format("§aBerhasil menambah §6" + amount
                            + " §aresin ke §b" + displayName + (isOnline ? "" : " §8(offline — tersimpan)"))));
                    if (isOnline) target.sendMessage(prefix.append(TextUtils.format("§6+" + amount
                            + " 🔲 §7(§6" + currentAdd + "§7/§6" + max + "§7)")));
                    break;

                case "take":
                    plugin.getResinManager().takeResin(targetUUID, amount);
                    sender.sendMessage(prefix.append(TextUtils.format("§aBerhasil mengurangi §6" + amount
                            + " §aresin dari §b" + displayName + (isOnline ? "" : " §8(offline — tersimpan)"))));
                    if (isOnline) {
                        int currentAfter = plugin.getResinManager().getResin(targetUUID);
                        target.sendMessage(prefix.append(TextUtils.format("§c-" + amount
                                + " 🔲 §7(§6" + currentAfter + "§7/§6" + max + "§7)")));
                    }
                    break;

                default:
                    sender.sendMessage(prefix.append(TextUtils.format("§cTipe aksi tidak diketahui. Gunakan: set, add, take, check.")));
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
                String typed = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(typed)) completions.add(p.getName());
                }
            } else if (args.length == 3 && !args[0].equalsIgnoreCase("check")) {
                completions.add("10"); completions.add("20");
                completions.add("50"); completions.add("160");
            }
        }
        return completions;
    }
}