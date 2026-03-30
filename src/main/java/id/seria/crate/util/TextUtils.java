package id.seria.crate.util;

import java.util.List;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class TextUtils {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    public static Component format(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        
        // Ubah § menjadi & agar konsisten
        text = text.replace("§", "&");

        // Jika mengandung format MiniMessage
        if (text.contains("<") && text.contains(">")) {
            try {
                return MiniMessage.miniMessage().deserialize(text);
            } catch (Exception ignored) {}
        }

        return LEGACY_SERIALIZER.deserialize(text);
    }

    public static List<Component> formatList(List<String> texts) {
        return texts.stream().map(TextUtils::format).collect(Collectors.toList());
    }
}