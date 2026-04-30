package io.github.potaseval.passport;

import io.github.potaseval.utils.DocumentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class ForeignPassportBook {

    private final Player owner;
    private final FileConfiguration config;

    public ForeignPassportBook(Player owner, FileConfiguration config) {
        this.owner = owner;
        this.config = config;
    }

    public void open() {
        open(owner);
    }

    public void open(Player viewer) {
        String uuid = owner.getUniqueId().toString();
        String path = "players." + uuid + ".";
        String foreignPath = path + "foreignPassport.";

        String citizenship = config.getString(foreignPath + "citizenship", "Не указано");
        String age = config.getString(foreignPath + "age", "Не указано");
        String gender = config.getString(foreignPath + "gender", "Не указано");
        String foreignNumber = config.getString(foreignPath + "number", "Не указано");
        String foreignIssueDate = config.getString(foreignPath + "issueDate", "Не указано");
        String foreignValidUntil = config.getString(foreignPath + "validUntil", "Не указано");
        String foreignIssuedBy = config.getString(foreignPath + "issuedBy", "Не указано");

        String internalState = config.getString(path + "passportState", "Не указано");
        String internalNumber = config.getString(path + "passportNumber", "Не указано");
        String internalIssueDate = config.getString(path + "issueDate", "Не указано");
        String internalValidUntil = config.getString(path + "validUntil", "Не указано");
        String internalIssuedBy = config.getString(path + "issuedBy", "Не указано");

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Загранпаспорт");
        meta.setAuthor("Миграционная служба");

        // ===== СТРАНИЦА 1: Титульная (центрированная) =====
        String titleText = "ЗАГРАНИЧНЫЙ";
        String subtitleText = "ПАСПОРТ";
        String subsubtitleText = "ГРАЖДАНИНА";
        int maxLen = Math.max(titleText.length(),
                Math.max(subtitleText.length(),
                        Math.max(subsubtitleText.length(), internalState.length())));
        String centeredTitle = DocumentUtils.centerText(titleText, maxLen);
        String centeredSubtitle = DocumentUtils.centerText(subtitleText, maxLen);
        String centeredSubsubtitle = DocumentUtils.centerText(subsubtitleText, maxLen);
        String centeredState = DocumentUtils.centerText(internalState, maxLen);

        Component header1 = Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.BLUE);
        Component line1 = Component.text(centeredTitle + "\n", NamedTextColor.DARK_BLUE);
        Component line2 = Component.text(centeredSubtitle + "\n", NamedTextColor.DARK_BLUE);
        Component line3 = Component.text(centeredSubsubtitle + "\n", NamedTextColor.DARK_BLUE);
        Component line4 = Component.text(centeredState + "\n\n", NamedTextColor.GOLD);
        Component footer1 = Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.BLUE);
        Component page1 = header1.append(line1).append(line2).append(line3).append(line4).append(footer1);
        meta.addPages(page1);

        // ===== СТРАНИЦА 2: Личные данные =====
        Component header2 = Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.BLUE);
        Component title2 = Component.text("    ЛИЧНЫЕ ДАННЫЕ\n\n", NamedTextColor.DARK_BLUE);
        Component info2 = Component.text()
                .append(Component.text("  ФИО: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(owner.getName(), NamedTextColor.BLACK))
                .append(Component.text("\n"))
                .append(Component.text("  Возраст: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(age, NamedTextColor.BLACK))
                .append(Component.text("\n"))
                .append(Component.text("  Пол: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(gender, NamedTextColor.BLACK))
                .append(Component.text("\n"))
                .append(Component.text("  Гражданство: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(citizenship, NamedTextColor.BLACK))
                .append(Component.text("\n\n"))
                .build();
        Component footer2 = Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.BLUE);
        Component page2 = header2.append(title2).append(info2).append(footer2);
        meta.addPages(page2);

        // ===== СТРАНИЦА 3: Паспорт гражданина =====
        Component header3 = Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n", NamedTextColor.BLUE);
        Component title3 = Component.text(" ПАСПОРТ ГРАЖДАНИНА\n\n", NamedTextColor.DARK_BLUE);
        Component info3 = Component.text()
                .append(Component.text("  Государство: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(internalState, NamedTextColor.BLACK))
                .append(Component.text("\n"))
                .append(Component.text("  Номер паспорта: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(internalNumber, NamedTextColor.BLACK))
                .append(Component.text("\n"))
                .append(Component.text("  Дата выдачи: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(internalIssueDate, NamedTextColor.BLACK))
                .append(Component.text("\n"))
                .append(Component.text("  Кем выдан: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(internalIssuedBy, NamedTextColor.BLACK))
                .append(Component.text("\n"))
                .append(Component.text("  Годен до: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(internalValidUntil, NamedTextColor.BLACK))
                .append(Component.text("\n\n"))
                .build();
        Component footer3 = Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.BLUE);
        Component page3 = header3.append(title3).append(info3).append(footer3);
        meta.addPages(page3);

        // ===== СТРАНИЦА 4: Загранпаспорт =====
        Component header4 = Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n", NamedTextColor.BLUE);
        Component title4 = Component.text("   ЗАГРАНПАСПОРТ\n\n", NamedTextColor.DARK_BLUE);
        Component info4 = Component.text()
                .append(Component.text("  Номер: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(foreignNumber, NamedTextColor.BLACK))
                .append(Component.text("\n"))
                .append(Component.text("  Дата выдачи: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(foreignIssueDate, NamedTextColor.BLACK))
                .append(Component.text("\n"))
                .append(Component.text("  Кем выдан: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(foreignIssuedBy, NamedTextColor.BLACK))
                .append(Component.text("\n"))
                .append(Component.text("  Годен до: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(foreignValidUntil, NamedTextColor.BLACK))
                .append(Component.text("\n\n"))
                .build();
        Component footer4 = Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.BLUE);
        Component page4 = header4.append(title4).append(info4).append(footer4);
        meta.addPages(page4);

        book.setItemMeta(meta);
        viewer.openBook(book);
    }
}