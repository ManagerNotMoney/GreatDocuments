package io.github.potaseval.passport;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class PassportBook {

    private final Player owner;
    private final FileConfiguration config;

    public PassportBook(Player owner, FileConfiguration config) {
        this.owner = owner;
        this.config = config;
    }

    public void open() {
        open(owner);
    }

    public void open(Player viewer) {
        String uuid = owner.getUniqueId().toString();
        String path = "players." + uuid + ".";

        String state = config.getString(path + "passportState", "Не указано");
        String citizenship = config.getString(path + "citizenship", "Не указано");
        String age = config.getString(path + "age", "Не указано");
        String gender = config.getString(path + "gender", "Не указано");
        String height = config.getString(path + "height", "Не указано");

        String district = config.getString(path + "residence.district", "Не указано");
        String street = config.getString(path + "residence.street", "Не указано");
        String house = config.getString(path + "residence.house", "Не указано");
        String coords = config.getString(path + "residence.coords", "Не указано");

        String birthplace = config.getString(path + "birthplace", "Не указано");
        String maritalStatus = config.getString(path + "maritalStatus", "Не указано");
        String spouse = config.getString(path + "spouse", "Не указано");

        String passportNumber = config.getString(path + "passportNumber", "Не указано");
        String issuedBy = config.getString(path + "issuedBy", "Не указано");
        String ministry = config.getString(path + "ministry", "Не указано"); // было "Министерство юстиции"
        String issuerPosition = config.getString(path + "issuerPosition", "Не указано");

        String issueDate = config.getString(path + "issueDate", "Не указано");
        String validUntil = config.getString(path + "validUntil", "Не указано");

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Паспорт");
        meta.setAuthor("Сервер");

        // Страница 1: Титульная
        Component page1 = Component.text()
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                .append(Component.text("  ПАСПОРТ\n", NamedTextColor.DARK_RED))
                .append(Component.text("  ГРАЖДАНИНА\n", NamedTextColor.DARK_RED))
                .append(Component.text("  " + state.replace("\n", "\n  ") + "\n\n", NamedTextColor.GOLD))
                .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                .build();
        meta.addPages(page1);

        // Страница 2: Личные данные
        boolean hasPersonalData = !age.equals("Не указано") || !gender.equals("Не указано")
                || !citizenship.equals("Не указано") || !height.equals("Не указано");

        Component page2;
        if (hasPersonalData) {
            page2 = Component.text()
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                    .append(Component.text("  ЛИЧНЫЕ ДАННЫЕ\n\n", NamedTextColor.DARK_RED))
                    .append(Component.text("  ФИО: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(owner.getName() + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Возраст: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(age + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Пол: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(gender + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Гражданство: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(citizenship + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Рост: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(height + "\n\n", NamedTextColor.BLACK))
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                    .build();
        } else {
            page2 = Component.text()
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                    .append(Component.text("  ЛИЧНЫЕ ДАННЫЕ\n", NamedTextColor.DARK_RED))
                    .append(Component.text("  ОТСУТСТВУЮТ\n\n", NamedTextColor.RED))
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                    .build();
        }
        meta.addPages(page2);

        // Страница 3: Биография
        boolean hasBiography = !birthplace.equals("Не указано") || !maritalStatus.equals("Не указано");

        Component page3;
        if (hasBiography) {
            TextComponent.Builder page3Builder = Component.text()
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                    .append(Component.text("  БИОГРАФИЯ\n\n", NamedTextColor.DARK_RED))
                    .append(Component.text("  Место рождения: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(birthplace + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Семейное положение: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(maritalStatus + "\n", NamedTextColor.BLACK));

            String maritalLower = maritalStatus.toLowerCase();
            if (maritalLower.equals("женат") || maritalLower.equals("замужем")) {
                page3Builder
                        .append(Component.text("  Супруг(а): ", NamedTextColor.DARK_GREEN))
                        .append(Component.text(spouse + "\n", NamedTextColor.BLACK));
            }

            page3 = page3Builder
                    .append(Component.text("\n ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                    .build();
        } else {
            page3 = Component.text()
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                    .append(Component.text("  БИОГРАФИЯ\n", NamedTextColor.DARK_RED))
                    .append(Component.text("  ОТСУТСТВУЕТ\n\n", NamedTextColor.RED))
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                    .build();
        }
        meta.addPages(page3);

        // Страница 4: Прописка
        boolean hasResidence = !district.equals("Не указано") || !street.equals("Не указано")
                || !house.equals("Не указано") || !coords.equals("Не указано");

        Component page4;
        if (hasResidence) {
            page4 = Component.text()
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                    .append(Component.text("  ПРОПИСКА\n\n", NamedTextColor.DARK_RED))
                    .append(Component.text("  Район: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(district + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Улица: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(street + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Дом: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(house + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Координаты: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(coords + "\n\n", NamedTextColor.BLACK))
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                    .build();
        } else {
            page4 = Component.text()
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                    .append(Component.text("  ПРОПИСКА\n", NamedTextColor.DARK_RED))
                    .append(Component.text("  ОТСУТСТВУЕТ\n\n", NamedTextColor.RED))
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                    .build();
        }
        meta.addPages(page4);

        // Страница 5: Выдача
        boolean hasIssueInfo = !passportNumber.equals("Не указано") || !ministry.equals("Не указано")
                || !issuedBy.equals("Не указано") || !issuerPosition.equals("Не указано");

        Component page5;
        if (hasIssueInfo) {
            page5 = Component.text()
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                    .append(Component.text("  ВЫДАЧА\n\n", NamedTextColor.DARK_RED))
                    .append(Component.text("  Номер паспорта: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(passportNumber + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Выдано в: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(ministry + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Кем выдан: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(issuedBy + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  Должность: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(issuerPosition + "\n\n", NamedTextColor.BLACK))
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                    .build();
        } else {
            page5 = Component.text()
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                    .append(Component.text("  ВЫДАЧА\n", NamedTextColor.DARK_RED))
                    .append(Component.text("  ОТСУТСТВУЕТ\n\n", NamedTextColor.RED))
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                    .build();
        }
        meta.addPages(page5);

        // Страница 6: Даты
        boolean hasDates = !issueDate.equals("Не указано") || !validUntil.equals("Не указано");

        Component page6;
        if (hasDates) {
            page6 = Component.text()
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                    .append(Component.text("  ДАТА ВЫДАЧИ: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(issueDate + "\n", NamedTextColor.BLACK))
                    .append(Component.text("  ГОДЕН ДО: ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(validUntil + "\n\n", NamedTextColor.BLACK))
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                    .build();
        } else {
            page6 = Component.text()
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓\n\n", NamedTextColor.GRAY))
                    .append(Component.text("  СРОКИ\n", NamedTextColor.DARK_RED))
                    .append(Component.text("  ОТСУТСТВУЮТ\n\n", NamedTextColor.RED))
                    .append(Component.text(" ▓▓▓▓▓▓▓▓▓▓▓▓", NamedTextColor.GRAY))
                    .build();
        }
        meta.addPages(page6);

        book.setItemMeta(meta);
        viewer.openBook(book);
    }
}