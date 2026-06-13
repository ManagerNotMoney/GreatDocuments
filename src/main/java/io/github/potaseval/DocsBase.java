package io.github.potaseval;

import io.github.potaseval.commands.PassportCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DocsBase extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PassportCommand passportCommand = new PassportCommand(this);
        var cmd = getCommand("gd");
        if (cmd == null) {
            getLogger().severe("Команда 'gd' не найдена в plugin.yml! Плагин отключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        cmd.setExecutor(passportCommand);
        cmd.setTabCompleter(passportCommand);

        getLogger().info("GreatDocuments загружен!");
    }
}
