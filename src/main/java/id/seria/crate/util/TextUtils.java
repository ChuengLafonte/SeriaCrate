package id.seria.crate.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class TextUtils {

    // Legacy serializer: support &x dan &#RRGGBB
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    // Pattern untuk deteksi HEX format &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");

    /**
     * Format text yang mendukung:
     * 1. MiniMessage: <red>, <bold>, <gradient:...>, <#FFD700>, dll
     * 2. Legacy: &c, &l, &r
     * 3. HEX Legacy: &#FFD700 (dikonversi ke format § Bukkit)
     * 4. § langsung
     *
     * Mixed format (misal: "&#FFD700<bold>TEKS") ditangani dengan
     * mengkonversi HEX legacy ke MiniMessage color tag sebelum di-parse.
     */
    public static Component format(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // Normalisasi § → &
        text = text.replace("§", "&");

        boolean hasMiniMessage = text.contains("<") && text.contains(">");
        boolean hasHexLegacy = HEX_PATTERN.matcher(text).find();

        // Jika ada campuran &#HEX dan MiniMessage tag, konversi &#HEX → <#RRGGBB>
        // agar semuanya bisa diproses MiniMessage sekaligus
        if (hasMiniMessage && hasHexLegacy) {
            text = convertHexLegacyToMiniMessage(text);
            // Konversi legacy &x ke MiniMessage sebelum parse
            text = convertLegacyToMiniMessage(text);
            try {
                return MiniMessage.miniMessage().deserialize(text);
            } catch (Exception ignored) {}
        }

        // Hanya MiniMessage (tidak ada &#HEX)
        if (hasMiniMessage) {
            try {
                return MiniMessage.miniMessage().deserialize(text);
            } catch (Exception ignored) {}
        }

        // Hanya HEX legacy atau & biasa — gunakan LegacyComponentSerializer
        return LEGACY_SERIALIZER.deserialize(text);
    }

    /**
     * Konversi &#RRGGBB → <#RRGGBB> (format MiniMessage)
     */
    private static String convertHexLegacyToMiniMessage(String text) {
        Matcher m = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "<#" + m.group(1) + ">");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Konversi format &x (legacy color/format codes) ke MiniMessage tags
     * Dipanggil hanya ketika ada mixed content dengan MiniMessage
     */
    private static String convertLegacyToMiniMessage(String text) {
        return text
            .replace("&0", "<black>").replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>").replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
            .replace("&6", "<gold>").replace("&7", "<gray>")
            .replace("&8", "<dark_gray>").replace("&9", "<blue>")
            .replace("&a", "<green>").replace("&b", "<aqua>")
            .replace("&c", "<red>").replace("&d", "<light_purple>")
            .replace("&e", "<yellow>").replace("&f", "<white>")
            .replace("&A", "<green>").replace("&B", "<aqua>")
            .replace("&C", "<red>").replace("&D", "<light_purple>")
            .replace("&E", "<yellow>").replace("&F", "<white>")
            .replace("&l", "<bold>").replace("&L", "<bold>")
            .replace("&o", "<italic>").replace("&O", "<italic>")
            .replace("&n", "<underlined>").replace("&N", "<underlined>")
            .replace("&m", "<strikethrough>").replace("&M", "<strikethrough>")
            .replace("&k", "<obfuscated>").replace("&K", "<obfuscated>")
            .replace("&r", "<reset>").replace("&R", "<reset>");
    }

    public static List<Component> formatList(List<String> texts) {
        return texts.stream().map(TextUtils::format).collect(Collectors.toList());
    }
}
