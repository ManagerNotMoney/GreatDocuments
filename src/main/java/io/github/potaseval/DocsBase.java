package io.github.potaseval;

import io.github.potaseval.commands.PassportCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DocsBase extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PassportCommand passportCommand = new PassportCommand(this);
        getCommand("gd").setExecutor(passportCommand);
        getCommand("gd").setTabCompleter(passportCommand);

        getLogger().info("GreatDocuments загружен!");
    }
}