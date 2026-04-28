package io.github.potaseval.passport;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class WorkPassBook {

    private final Player owner;
    private final FileConfiguration config;

    public WorkPassBook(Player owner, FileConfiguration config) {
        this.owner = owner;
        this.config = config;
    }

    public void open() {
        open(owner);
    }

    public void open(Player viewer) {
        String uuid = owner.getUniqueId().toString();
        String path = "players." + uuid + ".workPass.";

        String workPlace = config.getString(path + "workPlace", "Не указано");
        String position = config.getString(path + "position", "Не указано");
        String age = config.getString(path + "age", "Не указано");
        String passportNumber = config.getString(path + "passportNumber", "Не указано");
        String ministry = config.getString(path + "ministry", "Не указано");
        String issuedBy = config.getString(path + "issuedBy", "Не указано");
        String issueDate = config.getString(path + "issueDate", "Не указано");
        String validUntil = config.getString(path + "validUntil", "Не указано");

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Удостоверение");
        meta.setAuthor("Отдел кадров");

        // Страница 1: Титульная
        Component page1 = Component.text()
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("  УДОСТОВЕРЕНИЕ\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("  О РАБОТЕ В\n", NamedTextColor.BLUE))
                .append(Component.text("  " + workPlace.toUpperCase() + "\n\n", NamedTextColor.GOLD))
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.DARK_AQUA))
                .build();
        meta.addPages(page1);

        // Страница 2: Личные данные
        Component page2 = Component.text()
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("  ЛИЧНЫЕ ДАННЫЕ\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("  ФИО: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(owner.getName() + "\n", NamedTextColor.BLACK))
                .append(Component.text("  Возраст: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(age + "\n", NamedTextColor.BLACK))
                .append(Component.text("  Должность: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(position + "\n", NamedTextColor.BLACK))
                .append(Component.text("  Номер паспорта: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(passportNumber + "\n\n", NamedTextColor.BLACK))
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.DARK_AQUA))
                .build();
        meta.addPages(page2);

        // Страница 3: Подтверждение подлинности
        Component page3 = Component.text()
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("  УДОСТОВЕРЕНИЕ\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("  Настоящий документ\n", NamedTextColor.BLACK))
                .append(Component.text("  является официальным\n", NamedTextColor.BLACK))
                .append(Component.text("  подтверждением\n", NamedTextColor.BLACK))
                .append(Component.text("  трудоустройства\n", NamedTextColor.BLACK))
                .append(Component.text("  в указанном\n", NamedTextColor.BLACK))
                .append(Component.text("  министерстве.\n", NamedTextColor.BLACK))
                .append(Component.text("  Любые исправления\n", NamedTextColor.BLACK))
                .append(Component.text("  недействительны.\n\n", NamedTextColor.BLACK))
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.DARK_AQUA))
                .build();
        meta.addPages(page3);

        // Страница 4: Выдача
        Component page4 = Component.text()
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("  ВЫДАЧА\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("  Выдан: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(ministry + "\n", NamedTextColor.BLACK))
                .append(Component.text("  Кем выдан: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(issuedBy + "\n\n", NamedTextColor.BLACK))
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.DARK_AQUA))
                .build();
        meta.addPages(page4);

        // Страница 5: Сроки
        Component page5 = Component.text()
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("  СРОКИ\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("  Дата выдачи: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(issueDate + "\n", NamedTextColor.BLACK))
                .append(Component.text("  Годен до: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(validUntil + "\n\n", NamedTextColor.BLACK))
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.DARK_AQUA))
                .build();
        meta.addPages(page5);

        book.setItemMeta(meta);
        viewer.openBook(book);
    }
}