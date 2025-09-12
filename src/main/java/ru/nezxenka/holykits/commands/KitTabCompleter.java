package ru.nezxenka.holykits.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.nezxenka.holykits.HolyKits;
import ru.nezxenka.holykits.managers.Kit;
import ru.nezxenka.holykits.managers.KitManager;

public class KitTabCompleter implements TabCompleter {
    private final HolyKits plugin;

    public KitTabCompleter(HolyKits plugin) {
        this.plugin = plugin;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList();
        if (!(sender instanceof Player)) {
            return (List)completions;
        } else {
            Player player = (Player)sender;
            KitManager kitManager = this.plugin.getKitManager();
            if (args.length == 1) {
                if (player.hasPermission("holykits.prioritybypass")) {
                    completions = (List)kitManager.getAvailableKits(player).stream().map(Kit::getName).filter((name) -> {
                        return name.toLowerCase().startsWith(args[0].toLowerCase());
                    }).collect(Collectors.toList());
                } else {
                    int maxPriority = kitManager.getAvailableKits(player).stream().filter((kit) -> {
                        return kit.getPriority() != -1;
                    }).mapToInt(Kit::getPriority).max().orElse(Integer.MIN_VALUE);
                    completions = (List)kitManager.getAvailableKits(player).stream().filter((kit) -> {
                        return kit.getPriority() == -1 || kit.getPriority() == maxPriority;
                    }).map(Kit::getName).filter((name) -> {
                        return name.toLowerCase().startsWith(args[0].toLowerCase());
                    }).collect(Collectors.toList());
                }
            } else if (args.length == 2) {
                Iterator var10 = Bukkit.getOnlinePlayers().iterator();

                while(var10.hasNext()) {
                    Player player1 = (Player)var10.next();
                    if (player1.getName().toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                        ((List)completions).add(player1.getName());
                    }
                }
            }

            return (List)completions;
        }
    }
}