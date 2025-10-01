package ru.nezxenka.holykits;

import org.bukkit.plugin.java.JavaPlugin;
import ru.nezxenka.holykits.commands.KitAdminCommand;
import ru.nezxenka.holykits.commands.KitCommand;
import ru.nezxenka.holykits.commands.KitTabCompleter;
import ru.nezxenka.holykits.database.Database;
import ru.nezxenka.holykits.managers.KitManager;

public final class HolyKits extends JavaPlugin {
    private Database database;
    private KitManager kitManager;
    private static HolyKits instance;

    public void onEnable() {
        instance = this;
        if (!this.getDataFolder().exists() && !this.getDataFolder().mkdirs()) {
            this.getLogger().severe("Не удалось создать папку плагина!");
            this.getServer().getPluginManager().disablePlugin(this);
        } else {
            this.database = new Database(this);
            if (!this.database.connect()) {
                this.getLogger().severe("Не удалось подключиться к базе данных! Плагин будет отключен.");
                this.getServer().getPluginManager().disablePlugin(this);
            } else {
                this.kitManager = new KitManager(this);
                this.getCommand("kit").setExecutor(new KitCommand(this));
                this.getCommand("kit").setTabCompleter(new KitTabCompleter(this));
                this.getCommand("kitadmin").setExecutor(new KitAdminCommand(this));
            }
        }
    }

    public void onDisable() {
        if (this.database != null) {
            this.database.disconnect();
        }

    }

    public Database getDatabase() {
        return this.database;
    }

    public KitManager getKitManager() {
        return this.kitManager;
    }

    public static HolyKits getInstance() {
        return instance;
    }
}