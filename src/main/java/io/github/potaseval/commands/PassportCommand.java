package io.github.potaseval.commands;

import io.github.potaseval.DocsBase;
import io.github.potaseval.passport.ForeignPassportBook;
import io.github.potaseval.passport.PassportBook;
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
    private final Map<UUID, UUID> viewRequests = new HashMap<>();
    private final Map<UUID, Long> lastViewRequest = new HashMap<>();

    private static final Set<String> PASSPORT_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "гражданство","возраст","государство","номер","пол","рост",
            "месторождения","семейноеположение","супруг",
            "страна","район","улица","дом","координаты",
            "министерство","должность"
    ));
    private static final Set<String> FOREIGN_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "гражданство","возраст","пол","номер"
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
            player.sendMessage("Используйте: /gd passport, /gd view <игрок>, /gd setpassport [игрок] <поле> <значение>, /gd foreignpassport, /gd setforeignpassport ...");
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

    private static class ParsedField {
        final String field;
        final String value;
        ParsedField(String field, String value) { this.field = field; this.value = value; }
    }

    private void handleViewRequest(Player requester, String[] args) {
        if (args.length < 2) {
            requester.sendMessage("Использование: /gd view <ник>");
            return;
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
        viewRequests.put(target.getUniqueId(), requester.getUniqueId());
        lastViewRequest.put(requester.getUniqueId(), now);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        Component message = Component.text(requester.getName() + " показывает вам паспорт. ", NamedTextColor.YELLOW)
                .append(Component.text("[Посмотреть]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/gd acceptview " + requester.getName())))
                .append(Component.text(" "))
                .append(Component.text("[Отказаться]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/gd denyview " + requester.getName())));
        target.sendMessage(message);
        requester.sendMessage("Запрос отправлен игроку " + target.getName() + ".");
    }

    private void handleAcceptView(Player target, String[] args) {
        if (args.length < 2) {
            target.sendMessage("Использование: /gd acceptview <игрок>");
            return;
        }
        UUID requesterUuid = viewRequests.get(target.getUniqueId());
        if (requesterUuid == null) {
            target.sendMessage("Нет активного запроса.");
            return;
        }
        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester == null) {
            target.sendMessage("Игрок вышел из игры.");
            viewRequests.remove(target.getUniqueId());
            return;
        }
        if (!requester.getName().equalsIgnoreCase(args[1])) {
            target.sendMessage("Запрос не от указанного игрока.");
            return;
        }
        new PassportBook(requester, plugin.getConfig()).open(target);
        viewRequests.remove(target.getUniqueId());
        requester.sendMessage(target.getName() + " просматривает ваш паспорт.");
    }

    private void handleDenyView(Player target, String[] args) {
        if (args.length < 2) {
            target.sendMessage("Использование: /gd denyview <игрок>");
            return;
        }
        UUID requesterUuid = viewRequests.remove(target.getUniqueId());
        if (requesterUuid == null) {
            target.sendMessage("Нет активного запроса.");
            return;
        }
        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null) {
            requester.sendMessage(target.getName() + " отклонил(а) запрос.");
        }
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
        fieldPaths.put("министерство", "ministry");
        fieldPaths.put("должность", "issuerPosition");

        String configKey = fieldPaths.get(field);
        if (configKey == null) {
            editor.sendMessage("Поле не найдено в конфигурации.");
            return;
        }

        config.set(path + configKey, value);
        plugin.saveConfig();
        DocumentUtils.updateDocumentDates(plugin, target, "", 1, 0, editor.getName());
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