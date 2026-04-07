package id.seria.crate.placeholder;

import org.bukkit.OfflinePlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import id.seria.crate.SeriaCrate;

public class SeriaCratePlaceholder extends PlaceholderExpansion {

    private final SeriaCrate plugin;

    public SeriaCratePlaceholder(SeriaCrate plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "seriacrate";
    }

    @Override
    public String getAuthor() {
        return "Project Seria Dev";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Menjaga placeholder tetap aktif walau PAPI di-reload (/papi reload)
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        // Placeholder: %seriacrate_resin%
        // Menampilkan jumlah resin saat ini
        if (params.equalsIgnoreCase("resin")) {
            return String.valueOf(plugin.getResinManager().getResin(player.getUniqueId()));
        }

        // Placeholder: %seriacrate_resin_max%
        // Menampilkan maksimal resin dari config otomatis
        if (params.equalsIgnoreCase("resin_max")) {
            return String.valueOf(plugin.getConfigManager().getConfig().getInt("resin.max", 160));
        }

        // Placeholder: %seriacrate_resin_time%
        // Menampilkan waktu refill (format: 00m:00s atau FULL)
        if (params.equalsIgnoreCase("resin_time")) {
            return plugin.getResinManager().getRegenTimeFormatted(player.getUniqueId());
        }

        // Placeholder: %seriacrate_resin_cost%
        // Menampilkan biaya/pemakaian resin per pembukaan crate
        if (params.equalsIgnoreCase("resin_cost")) {
            return String.valueOf(plugin.getConfigManager().getConfig().getInt("settings.crate-cost", 20));
        }

        return null; // Mengembalikan null jika format placeholder tidak dikenali
    }
}