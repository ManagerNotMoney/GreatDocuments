package io.github.potaseval.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class DocumentUtils {

    private DocumentUtils() {
    }

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Центрирует текст в строке указанной ширины (моноширинный шрифт Minecraft)
     */
    public static String centerText(String text, int width) {
        if (text.length() >= width) return text;
        int left = (width - text.length()) / 2;
        int right = width - text.length() - left;
        return " ".repeat(left) + text + " ".repeat(right);
    }

    public static String getString(FileConfiguration config, String path) {
        return config.getString(path, "Не указано");
    }

    public static void updateDocumentDates(JavaPlugin plugin, Player player,
                                           String subPath, int months, int weeks,
                                           String issuerName) {
        LocalDate issue = LocalDate.now();
        LocalDate valid = issue.plusMonths(months).plusWeeks(weeks);
        FileConfiguration config = plugin.getConfig();
        String path = "players." + player.getUniqueId() + "." + subPath;
        config.set(path + "issueDate", issue.format(DATE_FORMAT));
        config.set(path + "validUntil", valid.format(DATE_FORMAT));
        config.set(path + "issuedBy", issuerName);
    }
}