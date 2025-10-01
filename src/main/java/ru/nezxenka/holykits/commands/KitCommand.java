package ru.nezxenka.holykits.commands;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.nezxenka.holykits.HolyKits;
import ru.nezxenka.holykits.managers.Kit;

public class KitCommand implements CommandExecutor {
    private final HolyKits plugin;

    public KitCommand(HolyKits plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length >= 2) {
                String kitName = args[0].toLowerCase();
                Optional<Kit> kitOptional = Optional.ofNullable(this.plugin.getKitManager().getKit(kitName));
                if (!kitOptional.isPresent()) {
                    sender.sendMessage("§c▶ §fТакого набора §cне существует");
                    return true;
                } else {
                    Kit kit = (Kit)kitOptional.get();
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§c▶ §fИгрок §cне найден");
                        return true;
                    } else {
                        kit.applyToInventory(target.getInventory());
                        target.updateInventory();
                        sender.sendMessage("§b▶ §fВы выдали набор §b" + kitName + " §fигроку §b" + target.getName());
                        return true;
                    }
                }
            } else {
                sender.sendMessage("§cЭта команда только для игроков!");
                return true;
            }
        } else {
            Player player = (Player)sender;
            if (args.length == 0) {
                player.sendMessage("§b▶ §fЧтобы получить набор, введите - §b/kit <название>");
                return true;
            } else {
                String kitName;
                if (this.plugin.getDatabase().isKitsDisabled()) {
                    kitName = this.plugin.getDatabase().getTimeKitToggled();
                    player.sendMessage("§x§F§D§1§A§0§B▶ §fИспользование §e/kit §x§F§D§1§A§0§Bотключено§f. Ограничение спадут через §x§1§D§C§2§F§F" + kitName);
                    return true;
                } else {
                    kitName = args[0].toLowerCase();
                    Optional kitOptional;
                    if (player.hasPermission("holykits.prioritybypass")) {
                        kitOptional = Optional.ofNullable(this.plugin.getKitManager().getKit(kitName));
                    } else {
                        kitOptional = this.plugin.getKitManager().getBestPriorityKit(player, kitName);
                    }

                    if (!kitOptional.isPresent()) {
                        player.sendMessage("§c▶ §fТакого набора §cне существует");
                        return true;
                    } else {
                        Kit kit = (Kit)kitOptional.get();
                        if (args.length >= 2 && player.hasPermission("holykits.admin.othergive")) {
                            Player target = Bukkit.getPlayer(args[1]);
                            if (target == null) {
                                player.sendMessage("§c▶ §fИгрок §cне найден");
                                return true;
                            } else {
                                if (!player.hasPermission("holykits.bypass")) {
                                    long currentTime = System.currentTimeMillis();
                                    long cooldownEnd = this.plugin.getDatabase().getCooldown(player.getUniqueId(), kitName);
                                    if (currentTime < cooldownEnd) {
                                        this.sendCooldownMessage(player, kitName, cooldownEnd - currentTime);
                                        return true;
                                    }

                                    if (kit.getCooldown() > 0L) {
                                        this.plugin.getDatabase().setCooldown(player.getUniqueId(), kitName, currentTime + kit.getCooldown() * 1000L);
                                    }
                                }

                                kit.applyToInventory(target.getInventory());
                                target.updateInventory();
                                player.sendMessage("§b▶ §fВы выдали набор §b" + kitName + " §fигроку §b" + target.getName());
                                return true;
                            }
                        } else {
                            if (!player.hasPermission("holykits.bypass")) {
                                long currentTime = System.currentTimeMillis();
                                long cooldownEnd = this.plugin.getDatabase().getCooldown(player.getUniqueId(), kitName);
                                if (currentTime < cooldownEnd) {
                                    this.sendCooldownMessage(player, kitName, cooldownEnd - currentTime);
                                    return true;
                                }

                                if (kit.getCooldown() > 0L) {
                                    this.plugin.getDatabase().setCooldown(player.getUniqueId(), kitName, currentTime + kit.getCooldown() * 1000L);
                                }
                            }

                            kit.applyToInventory(player.getInventory());
                            player.updateInventory();
                            player.sendMessage("§b▶ §fВы получили набор §b" + kitName);
                            return true;
                        }
                    }
                }
            }
        }
    }

    private void sendCooldownMessage(Player player, String kitName, long remainingMillis) {
        player.sendMessage("");
        player.sendMessage("       §b• §fДля повторного получения набора §6" + kitName);
        player.sendMessage("            §fОсталось §b" + this.formatCooldownMessage(remainingMillis));
        player.sendMessage("");
    }

    private String formatCooldownMessage(long millis) {
        long seconds = millis / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;
        if (days > 0L) {
            return String.format("%d дн. %02d ч. %02d мин. %02d сек.", days, hours % 24L, minutes % 60L, seconds % 60L);
        } else if (hours > 0L) {
            return String.format("%02d ч. %02d мин. %02d сек.", hours, minutes % 60L, seconds % 60L);
        } else {
            return minutes > 0L ? String.format("%02d мин. %02d сек.", minutes, seconds % 60L) : String.format("%02d сек.", seconds);
        }
    }
}