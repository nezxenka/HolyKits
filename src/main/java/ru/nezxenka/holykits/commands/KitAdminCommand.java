package ru.nezxenka.holykits.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.nezxenka.holykits.HolyKits;
import ru.nezxenka.holykits.managers.KitManager;

public class KitAdminCommand implements CommandExecutor {
    private final HolyKits plugin;

    public KitAdminCommand(HolyKits plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("holykits.admin")) {
            sender.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        } else if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        } else {
            KitManager kitManager = this.plugin.getKitManager();
            String subCommand = args[0].toLowerCase();
            byte var8 = -1;
            switch(subCommand.hashCode()) {
                case -1348984648:
                    if (subCommand.equals("clearcooldown")) {
                        var8 = 4;
                    }
                    break;
                case -934610812:
                    if (subCommand.equals("remove")) {
                        var8 = 1;
                    }
                    break;
                case -905779121:
                    if (subCommand.equals("setinv")) {
                        var8 = 0;
                    }
                    break;
                case -300581729:
                    if (subCommand.equals("setenabled")) {
                        var8 = 5;
                    }
                    break;
                case 759215110:
                    if (subCommand.equals("setpriority")) {
                        var8 = 3;
                    }
                    break;
                case 1985941039:
                    if (subCommand.equals("settime")) {
                        var8 = 2;
                    }
            }

            switch(var8) {
                case 0:
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§cЭта команда только для игроков!");
                        return true;
                    }

                    if (args.length < 2) {
                        sender.sendMessage("§cИспользование: /kitadmin setinv <название набора>");
                        return true;
                    }

                    String kitName = args[1].toLowerCase();
                    if (kitManager.createKit(kitName, (Player)sender)) {
                        sender.sendMessage("§aНабор " + kitName + " успешно создан из вашего инвентаря!");
                    } else {
                        sender.sendMessage("§aНабор с таким именем уже существует, инвентарь обновлен!");
                    }
                    break;
                case 1:
                    if (args.length < 2) {
                        sender.sendMessage("§cИспользование: /kitadmin remove <название набора>");
                        return true;
                    }

                    String kitName1 = args[1].toLowerCase();
                    if (kitManager.deleteKit(kitName1)) {
                        sender.sendMessage("§aНабор " + kitName1 + " успешно удален!");
                    } else {
                        sender.sendMessage("§cНабор с таким именем не существует!");
                    }
                    break;
                case 2:
                    if (args.length < 3) {
                        sender.sendMessage("§cИспользование: /kitadmin settime <название набора> <время в секундах>");
                        return true;
                    }

                    try {
                        long cooldown = Long.parseLong(args[2]);
                        kitManager.setKitCooldown(args[1].toLowerCase(), cooldown);
                        sender.sendMessage("§aКулдаун для набора " + args[1] + " установлен на " + cooldown + " секунд.");
                    } catch (NumberFormatException var17) {
                        sender.sendMessage("§cВремя должно быть числом!");
                    }
                    break;
                case 3:
                    if (args.length < 3) {
                        sender.sendMessage("§cИспользование: /kitadmin setpriority <название набора> <приоритет>");
                        return true;
                    }

                    try {
                        int priority = Integer.parseInt(args[2]);
                        kitManager.setKitPriority(args[1].toLowerCase(), priority);
                        sender.sendMessage("§aПриоритет для набора " + args[1] + " установлен на " + priority + ".");
                    } catch (NumberFormatException var16) {
                        sender.sendMessage("§cПриоритет должен быть числом!");
                    }
                    break;
                case 4:
                    if (args.length < 3) {
                        sender.sendMessage("§cИспользование: /kitadmin clearcooldown <игрок> <название набора>");
                        return true;
                    }

                    Player target = this.plugin.getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§cИгрок не найден или не в сети!");
                        return true;
                    }

                    kitManager.clearCooldown(target, args[2].toLowerCase());
                    sender.sendMessage("§aКулдаун для игрока " + target.getName() + " на набор " + args[2] + " был сброшен.");
                    break;
                case 5:
                    if (args.length < 2) {
                        sender.sendMessage("§cИспользование: /kitadmin setenabled <true/false> [время в секундах]");
                        return true;
                    }

                    boolean enabled;
                    try {
                        enabled = Boolean.parseBoolean(args[1]);
                    } catch (Exception var15) {
                        sender.sendMessage("§cУкажите true или false для включения/выключения наборов");
                        return true;
                    }

                    if (args.length >= 3 && args[1].equalsIgnoreCase("false")) {
                        try {
                            if (this.plugin.getDatabase().setKitToggleTime(args[2])) {
                                sender.sendMessage("§aУспешно");
                            } else {
                                sender.sendMessage("§cОшибка");
                            }
                        } catch (NumberFormatException var14) {
                            sender.sendMessage("§cВремя должно быть числом!");
                            return true;
                        }
                    } else {
                        this.plugin.getDatabase().toggleKits(!enabled);
                        sender.sendMessage("§aНаборы теперь " + (enabled ? "включены" : "выключены"));
                    }
                    break;
                default:
                    this.sendHelp(sender);
            }

            return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== HolyKits Admin Help =====");
        sender.sendMessage("§a/kitadmin setinv <название> - создать набор из инвентаря");
        sender.sendMessage("§a/kitadmin settime <название> <секунды> - установить кулдаун");
        sender.sendMessage("§a/kitadmin setpriority <название> <приоритет> - установить приоритет");
        sender.sendMessage("§a/kitadmin clearcooldown <игрок> <название> - сбросить кулдаун");
        sender.sendMessage("§a/kitadmin setenabled <true/false> [время] - вкл/выкл наборы");
    }
}