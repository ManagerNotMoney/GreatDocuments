package io.github.potaseval.commands;

import io.github.potaseval.DocsBase;
import io.github.potaseval.passport.ForeignPassportBook;
import io.github.potaseval.passport.PassportBook;
import io.github.potaseval.passport.WorkPassBook;
import io.github.potaseval.utils.DocumentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PassportCommand implements CommandExecutor, TabCompleter {

    private final DocsBase plugin;
    private final Map<UUID, ViewRequest> viewRequests = new HashMap<>();
    private final Map<UUID, Long> lastViewRequest = new HashMap<>();

    private static final Set<String> PASSPORT_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "гражданство","возраст","государство","номер","пол","рост",
            "месторождения","семейноеположение","супруг",
            "страна","район","улица","дом","координаты"
    ));
    private static final Set<String> FOREIGN_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "гражданство","возраст","пол","номер"
    ));
    private static final Set<String> WORK_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "местоработы","должность","возраст","номерпаспорта","министерство"
    ));

    public PassportCommand(DocsBase plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эта команда только для игроков.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(
                    "§6/gd passport §7- открыть паспорт\n" +
                            "§6/gd view <игрок> §7- запросить просмотр\n" +
                            "§6/gd foreignpassport §7- загранпаспорт\n" +
                            "§6/gd workpass §7- удостоверение о работе\n" +
                            (player.hasPermission("greatdocuments.setpassport") ?
                                    "§6/gd setpassport [игрок] <поле> <значение> §7- изменить паспорт\n" : "") +
                            (player.hasPermission("greatdocuments.setforeignpassport") ?
                                    "§6/gd setforeignpassport [игрок] <поле> <значение> §7- изменить загранпаспорт\n" : "") +
                            (player.hasPermission("greatdocuments.setworkpass") ?
                                    "§6/gd setworkpass [игрок] <поле> <значение> §7- изменить удостоверение" : "")
            );
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "passport":
                new PassportBook(player, plugin.getConfig()).open();
                break;
            case "setpassport":
                if (!player.hasPermission("greatdocuments.setpassport")) {
                    player.sendMessage("У вас недостаточно прав.");
                    return true;
                }
                handleSetPassport(player, args);
                break;
            case "foreignpassport":
                new ForeignPassportBook(player, plugin.getConfig()).open();
                break;
            case "setforeignpassport":
                if (!player.hasPermission("greatdocuments.setforeignpassport")) {
                    player.sendMessage("У вас недостаточно прав.");
                    return true;
                }
                handleSetForeignPassport(player, args);
                break;
            case "view":
                handleViewRequest(player, args);
                break;
            case "acceptview":
                handleAcceptView(player, args);
                break;
            case "denyview":
                handleDenyView(player, args);
                break;
            case "workpass":
                new WorkPassBook(player, plugin.getConfig()).open();
                break;
            case "setworkpass":
                if (!player.hasPermission("greatdocuments.setworkpass")) {
                    player.sendMessage("У вас недостаточно прав.");
                    return true;
                }
                handleSetWorkPass(player, args);
                break;
            default:
                player.sendMessage("Неизвестная подкоманда.");
        }
        return true;
    }

    private ParsedField parseFieldAndValue(Player sender, String[] args, int start, Set<String> validFields) {
        // Ищем наиболее длинное совпадение (до 2 слов, но сейчас все поля без пробелов)
        for (int len = Math.min(args.length - start, 1); len > 0; len--) { // только одно слово
            String candidate = args[start]; // все поля теперь однословные
            if (validFields.contains(candidate.toLowerCase())) {
                String value = start + 1 < args.length ? String.join(" ", Arrays.copyOfRange(args, start + 1, args.length)) : "";
                if (value.isEmpty()) {
                    sender.sendMessage("Укажите значение для поля '" + candidate + "'.");
                    return null;
                }
                return new ParsedField(candidate.toLowerCase(), value);
            }
        }
        sender.sendMessage("Неизвестное поле. Доступные: " + String.join(", ", validFields));
        return null;
    }
    private void handleSetWorkPass(Player editor, String[] args) {
        Player target = null;
        int fieldStart = 1;
        if (args.length >= 2) {
            Player potentialTarget = Bukkit.getPlayer(args[1]);
            if (potentialTarget != null) {
                target = potentialTarget;
                fieldStart = 2;
            }
        }
        if (target == null) target = editor;

        if (!target.equals(editor)) {
            if (editor.getLocation().distance(target.getLocation()) > 20) {
                editor.sendMessage("Игрок слишком далеко (максимум 20 блоков).");
                return;
            }
        }

        if (args.length <= fieldStart) {
            editor.sendMessage("Использование: /gd setworkpass [игрок] <поле> <значение>");
            return;
        }

        ParsedField parsed = parseFieldAndValue(editor, args, fieldStart, WORK_FIELDS);
        if (parsed == null) return;

        String field = parsed.field;
        String value = parsed.value;

        if (field.equals("возраст")) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                editor.sendMessage("Возраст должен быть целым числом.");
                return;
            }
        }

        String path = "players." + target.getUniqueId() + ".workPass.";
        var config = plugin.getConfig();

        Map<String, String> fieldPaths = new LinkedHashMap<>();
        fieldPaths.put("местоработы", "workPlace");
        fieldPaths.put("должность", "position");
        fieldPaths.put("возраст", "age");
        fieldPaths.put("номерпаспорта", "passportNumber");
        fieldPaths.put("министерство", "ministry");

        String configKey = fieldPaths.get(field);
        if (configKey == null) {
            editor.sendMessage("Поле не найдено.");
            return;
        }

        config.set(path + configKey, value);
        DocumentUtils.updateDocumentDates(plugin, target, "workPass.", 12, 0, editor.getName());
        plugin.saveConfig();

        String targetMsg = target.equals(editor) ? "Ваше удостоверение" : "Удостоверение игрока " + target.getName();
        editor.sendMessage("Поле '" + field + "' обновлено! (" + targetMsg + ")");
    }
    private static class ParsedField {
        final String field;
        final String value;
        ParsedField(String field, String value) { this.field = field; this.value = value; }
    }

    private void handleViewRequest(Player requester, String[] args) {
        if (args.length < 2) {
            requester.sendMessage("Использование: /gd view <ник> [passport|workpass]");
            return;
        }
        // Определяем тип документа
        String docType = "passport"; // по умолчанию
        if (args.length >= 3) {
            String typeArg = args[2].toLowerCase();
            if (typeArg.equals("workpass") || typeArg.equals("passport")) {
                docType = typeArg;
            } else {
                requester.sendMessage("Неизвестный тип документа. Используйте passport или workpass.");
                return;
            }
        }

        long now = System.currentTimeMillis();
        Long last = lastViewRequest.get(requester.getUniqueId());
        if (last != null && (now - last) < 30_000) {
            long remaining = 30 - (now - last) / 1000;
            requester.sendMessage("Подождите " + remaining + " сек.");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            requester.sendMessage("Игрок не найден.");
            return;
        }
        if (target.equals(requester)) {
            requester.sendMessage("Нельзя отправить запрос самому себе.");
            return;
        }
        if (requester.getLocation().distance(target.getLocation()) > 15) {
            requester.sendMessage("Игрок слишком далеко (максимум 15 блоков).");
            return;
        }

        viewRequests.put(target.getUniqueId(), new ViewRequest(requester.getUniqueId(), docType));
        lastViewRequest.put(requester.getUniqueId(), now);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        String docName = docType.equals("workpass") ? "удостоверение" : "паспорт";
        Component message = Component.text(requester.getName() + " показывает вам " + docName + ". ", NamedTextColor.YELLOW)
                .append(Component.text("[Посмотреть]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/gd acceptview " + requester.getName())))
                .append(Component.text(" "))
                .append(Component.text("[Отказаться]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/gd denyview " + requester.getName())));
        target.sendMessage(message);
        requester.sendMessage("Запрос отправлен игроку " + target.getName() + " (" + docName + ").");
    }
    private void handleAcceptView(Player target, String[] args) {
        if (args.length < 2) {
            target.sendMessage("Использование: /gd acceptview <игрок>");
            return;
        }
        ViewRequest request = viewRequests.get(target.getUniqueId());
        if (request == null) {
            target.sendMessage("Нет активного запроса.");
            return;
        }
        Player requester = Bukkit.getPlayer(request.requester);
        if (requester == null) {
            target.sendMessage("Игрок вышел из игры.");
            viewRequests.remove(target.getUniqueId());
            return;
        }
        if (!requester.getName().equalsIgnoreCase(args[1])) {
            target.sendMessage("Запрос не от указанного игрока.");
            return;
        }
        if (!target.getWorld().equals(requester.getWorld()) ||
                target.getLocation().distance(requester.getLocation()) > 15) {
            target.sendMessage("Игрок слишком далеко (максимум 15 блоков).");
            return;
        }

        // Открываем соответствующий документ
        if (request.docType.equals("workpass")) {
            new WorkPassBook(requester, plugin.getConfig()).open(target);
        } else {
            new PassportBook(requester, plugin.getConfig()).open(target);
        }

        viewRequests.remove(target.getUniqueId());
        requester.sendMessage(target.getName() + " просматривает ваш " +
                (request.docType.equals("workpass") ? "удостоверение." : "паспорт."));
    }

    private void handleDenyView(Player target, String[] args) {
        if (args.length < 2) {
            target.sendMessage("Использование: /gd denyview <игрок>");
            return;
        }
        ViewRequest request = viewRequests.get(target.getUniqueId());
        if (request == null) {
            target.sendMessage("Нет активного запроса.");
            return;
        }
        Player requester = Bukkit.getPlayer(request.requester);
        if (requester == null) {
            target.sendMessage("Игрок, запросивший просмотр, вышел из игры.");
            viewRequests.remove(target.getUniqueId());
            return;
        }
        if (!requester.getName().equalsIgnoreCase(args[1])) {
            target.sendMessage("Запрос не от указанного игрока.");
            return;
        }
        viewRequests.remove(target.getUniqueId());
        requester.sendMessage(target.getName() + " отклонил(а) запрос.");
        target.sendMessage("Вы отклонили запрос.");
    }

    private void handleSetPassport(Player editor, String[] args) {
        Player target = null;
        int fieldStart = 1;
        if (args.length >= 2) {
            Player potentialTarget = Bukkit.getPlayer(args[1]);
            if (potentialTarget != null) {
                target = potentialTarget;
                fieldStart = 2;
            }
        }
        if (target == null) target = editor;

        if (!target.equals(editor)) {
            if (editor.getLocation().distance(target.getLocation()) > 20) {
                editor.sendMessage("Игрок слишком далеко (максимум 20 блоков).");
                return;
            }
        }

        if (args.length <= fieldStart) {
            editor.sendMessage("Использование: /gd setpassport [игрок] <поле> <значение>");
            return;
        }

        ParsedField parsed = parseFieldAndValue(editor, args, fieldStart, PASSPORT_FIELDS);
        if (parsed == null) return;

        String field = parsed.field;
        String value = parsed.value;

        if (field.equals("пол")) {
            if (!value.equalsIgnoreCase("Мужской") && !value.equalsIgnoreCase("Женский")) {
                editor.sendMessage("Пол должен быть 'Мужской' или 'Женский'.");
                return;
            }
        } else if (field.equals("семейноеположение")) {
            Set<String> allowed = new HashSet<>(Arrays.asList("женат", "не женат", "замужем", "не замужем"));
            if (!allowed.contains(value.toLowerCase())) {
                editor.sendMessage("Допустимые значения: Женат, Не женат, Замужем, Не замужем.");
                return;
            }
        } else if (field.equals("возраст") || field.equals("рост")) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                editor.sendMessage("Возраст и рост должны быть целыми числами.");
                return;
            }
        }

        String uuid = target.getUniqueId().toString();
        String path = "players." + uuid + ".";
        var config = plugin.getConfig();

        if (!target.equals(editor)) {
            String editorWorkPath = "players." + editor.getUniqueId() + ".workPass.";
            String editorWorkPlace = config.getString(editorWorkPath + "workPlace");
            String editorPosition = config.getString(editorWorkPath + "position");
            if (editorWorkPlace == null || editorWorkPlace.isEmpty() ||
                    editorPosition == null || editorPosition.isEmpty()) {
                editor.sendMessage("У вас нет действующего удостоверения о работе. Вы не можете редактировать чужие паспорта.");
                return;
            }
            config.set(path + "ministry", editorWorkPlace);
            config.set(path + "issuerPosition", editorPosition);
        } else {
            String editorWorkPath = "players." + editor.getUniqueId() + ".workPass.";
            String wp = config.getString(editorWorkPath + "workPlace");
            if (wp != null && !wp.isEmpty()) {
                config.set(path + "ministry", wp);
            }
            String pos = config.getString(editorWorkPath + "position");
            if (pos != null && !pos.isEmpty()) {
                config.set(path + "issuerPosition", pos);
            }
        }
        Map<String, String> fieldPaths = new LinkedHashMap<>();
        fieldPaths.put("гражданство", "citizenship");
        fieldPaths.put("возраст", "age");
        fieldPaths.put("государство", "passportState");
        fieldPaths.put("номер", "passportNumber");
        fieldPaths.put("пол", "gender");
        fieldPaths.put("рост", "height");
        fieldPaths.put("месторождения", "birthplace");
        fieldPaths.put("семейноеположение", "maritalStatus");
        fieldPaths.put("супруг", "spouse");
        fieldPaths.put("страна", "residence.country");
        fieldPaths.put("район", "residence.district");
        fieldPaths.put("улица", "residence.street");
        fieldPaths.put("дом", "residence.house");
        fieldPaths.put("координаты", "residence.coords");

        String configKey = fieldPaths.get(field);
        if (configKey == null) {
            editor.sendMessage("Поле не найдено в конфигурации.");
            return;
        }

        config.set(path + configKey, value);
        DocumentUtils.updateDocumentDates(plugin, target, "", 1, 0, editor.getName());
        plugin.saveConfig();

        String targetMsg = target.equals(editor) ? "Ваш паспорт" : "Паспорт игрока " + target.getName();
        editor.sendMessage("Поле '" + field + "' обновлено! (" + targetMsg + ")");
    }

    private void handleSetForeignPassport(Player editor, String[] args) {
        Player target = null;
        int fieldStart = 1;
        if (args.length >= 2) {
            Player potentialTarget = Bukkit.getPlayer(args[1]);
            if (potentialTarget != null) {
                target = potentialTarget;
                fieldStart = 2;
            }
        }
        if (target == null) target = editor;

        if (!target.equals(editor)) {
            if (editor.getLocation().distance(target.getLocation()) > 20) {
                editor.sendMessage("Игрок слишком далеко (максимум 20 блоков).");
                return;
            }
        }

        if (args.length <= fieldStart) {
            editor.sendMessage("Использование: /gd setforeignpassport [игрок] <поле> <значение>");
            return;
        }

        ParsedField parsed = parseFieldAndValue(editor, args, fieldStart, FOREIGN_FIELDS);
        if (parsed == null) return;

        String field = parsed.field;
        String value = parsed.value;

        if (field.equals("пол")) {
            if (!value.equalsIgnoreCase("Мужской") && !value.equalsIgnoreCase("Женский")) {
                editor.sendMessage("Пол должен быть 'Мужской' или 'Женский'.");
                return;
            }
        } else if (field.equals("возраст")) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                editor.sendMessage("Возраст должен быть числом.");
                return;
            }
        }

        String path = "players." + target.getUniqueId() + ".foreignPassport.";
        var config = plugin.getConfig();

        Map<String, String> fieldPaths = new LinkedHashMap<>();
        fieldPaths.put("гражданство", "citizenship");
        fieldPaths.put("возраст", "age");
        fieldPaths.put("пол", "gender");
        fieldPaths.put("номер", "number");

        String configKey = fieldPaths.get(field);
        if (configKey == null) {
            editor.sendMessage("Поле не найдено.");
            return;
        }
        config.set(path + configKey, value);
        DocumentUtils.updateDocumentDates(plugin, target, "foreignPassport.", 0, 2, editor.getName());
        plugin.saveConfig();
        String targetMsg = target.equals(editor) ? "Ваш загранпаспорт" : "Загранпаспорт игрока " + target.getName();
        editor.sendMessage("Поле '" + field + "' обновлено! (" + targetMsg + ")");
    }
    private static class ViewRequest {
        final UUID requester;
        final String docType;
        ViewRequest(UUID requester, String docType) {
            this.requester = requester;
            this.docType = docType;
        }
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return null;
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("passport");
            if (sender.hasPermission("greatdocuments.setpassport")) completions.add("setpassport");
            completions.add("foreignpassport");
            if (sender.hasPermission("greatdocuments.setforeignpassport")) completions.add("setforeignpassport");
            completions.add("view");
            completions.add("acceptview");
            completions.add("denyview");
            completions.add("workpass");
            if (sender.hasPermission("greatdocuments.setworkpass")) completions.add("setworkpass");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setpassport") && sender.hasPermission("greatdocuments.setpassport")) {
                completions.addAll(PASSPORT_FIELDS);
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            } else if (sub.equals("setforeignpassport") && sender.hasPermission("greatdocuments.setforeignpassport")) {
                completions.addAll(FOREIGN_FIELDS);
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            } else if (sub.equals("view") || sub.equals("acceptview") || sub.equals("denyview")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            } else if (sub.equals("setworkpass") && sender.hasPermission("greatdocuments.setworkpass")) {
                completions.addAll(WORK_FIELDS);
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setpassport") && sender.hasPermission("greatdocuments.setpassport")) {
                String prev = args[1];
                if (Bukkit.getOnlinePlayers().stream().anyMatch(p -> p.getName().equalsIgnoreCase(prev))) {
                    completions.addAll(PASSPORT_FIELDS);
                } else if (prev.equalsIgnoreCase("пол")) {
                    completions.addAll(Arrays.asList("Мужской","Женский"));
                } else if (prev.equalsIgnoreCase("семейноеположение")) {
                    completions.addAll(Arrays.asList("Женат","Не женат","Замужем","Не замужем"));
                }
            } else if (sub.equals("setforeignpassport") && sender.hasPermission("greatdocuments.setforeignpassport")) {
                String prev = args[1];
                if (Bukkit.getOnlinePlayers().stream().anyMatch(p -> p.getName().equalsIgnoreCase(prev))) {
                    completions.addAll(FOREIGN_FIELDS);
                } else if (prev.equalsIgnoreCase("пол")) {
                    completions.addAll(Arrays.asList("Мужской","Женский"));
                }
            } else if (sub.equals("view")) {
            completions.addAll(Arrays.asList("passport", "workpass"));
        }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setpassport") && sender.hasPermission("greatdocuments.setpassport")
                    && Bukkit.getOnlinePlayers().stream().anyMatch(p -> p.getName().equalsIgnoreCase(args[1]))) {
                String field = args[2];
                if (field.equalsIgnoreCase("пол")) {
                    completions.addAll(Arrays.asList("Мужской","Женский"));
                } else if (field.equalsIgnoreCase("семейноеположение")) {
                    completions.addAll(Arrays.asList("Женат","Не женат","Замужем","Не замужем"));
                }
            } else if (sub.equals("setforeignpassport") && sender.hasPermission("greatdocuments.setforeignpassport")
                    && Bukkit.getOnlinePlayers().stream().anyMatch(p -> p.getName().equalsIgnoreCase(args[1]))) {
                String field = args[2];
                if (field.equalsIgnoreCase("пол")) {
                    completions.addAll(Arrays.asList("Мужской","Женский"));
                }
            }
        }
        String lastArg = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));
        return completions;
    }
}