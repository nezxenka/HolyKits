package ru.nezxenka.holykits.managers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.nezxenka.holykits.HolyKits;

public class KitManager {
    private final HolyKits plugin;
    private final Map<String, Kit> kits = new HashMap();

    public KitManager(HolyKits plugin) {
        this.plugin = plugin;
        this.loadKits();
    }

    private void loadKits() {
        FileConfiguration config = this.plugin.getConfig();
        Iterator var2 = config.getKeys(false).iterator();

        while(true) {
            String kitName;
            do {
                if (!var2.hasNext()) {
                    return;
                }

                kitName = (String)var2.next();
            } while(!config.isConfigurationSection(kitName));

            ConfigurationSection kitSection = config.getConfigurationSection(kitName);
            Kit kit = new Kit(kitName, kitSection.getInt("priority", 0), kitSection.getLong("cooldown", 0L));
            if (kitSection.isConfigurationSection("items")) {
                ConfigurationSection itemsSection = kitSection.getConfigurationSection("items");
                Iterator var7 = itemsSection.getKeys(false).iterator();

                while(var7.hasNext()) {
                    String slotStr = (String)var7.next();

                    try {
                        int slot = Integer.parseInt(slotStr);
                        ItemStack item = itemsSection.getItemStack(slotStr);
                        if (item != null) {
                            kit.addItem(slot, item);
                        }
                    } catch (NumberFormatException var11) {
                        this.plugin.getLogger().warning("Некорректный слот " + slotStr + " в наборе " + kitName);
                    }
                }
            }

            this.kits.put(kitName, kit);
        }
    }

    public boolean createKit(String kitName, Player player) {
        if (this.kits.containsKey(kitName)) {
            this.getKit(kitName).saveFromInventory(player.getInventory());
            this.saveKit(this.getKit(kitName));
            return false;
        } else {
            Kit kit = new Kit(kitName, 0, 0L);
            kit.saveFromInventory(player.getInventory());
            this.kits.put(kitName, kit);
            this.saveKit(kit);
            return true;
        }
    }

    public boolean deleteKit(String kitName) {
        if (this.kits.containsKey(kitName)) {
            this.kits.remove(kitName);
            FileConfiguration config = this.plugin.getConfig();
            config.set(kitName, (Object)null);
            this.plugin.saveConfig();
            return true;
        } else {
            return false;
        }
    }

    public void setKitCooldown(String kitName, long cooldown) {
        Kit kit = (Kit)this.kits.get(kitName);
        if (kit != null) {
            kit.setCooldown(cooldown);
            this.saveKit(kit);
        }

    }

    public void setKitPriority(String kitName, int priority) {
        Kit kit = (Kit)this.kits.get(kitName);
        if (kit != null) {
            kit.setPriority(priority);
            this.saveKit(kit);
        }

    }

    public void clearCooldown(Player player, String kitName) {
        this.plugin.getDatabase().clearCooldown(player.getUniqueId(), kitName);
    }

    private void saveKit(Kit kit) {
        FileConfiguration config = this.plugin.getConfig();
        ConfigurationSection kitSection = config.createSection(kit.getName());
        kitSection.set("priority", kit.getPriority());
        kitSection.set("cooldown", kit.getCooldown());
        ConfigurationSection itemsSection = kitSection.createSection("items");
        kit.getItems().forEach((slot, item) -> {
            itemsSection.set(String.valueOf(slot), item);
        });
        ConfigurationSection armorSection = kitSection.createSection("armor");
        this.plugin.saveConfig();
    }

    public List<Kit> getAvailableKits(Player player) {
        return (List)this.kits.values().stream().filter((kit) -> {
            return player.hasPermission("holykits.kit." + kit.getName());
        }).filter(Kit::isValid).collect(Collectors.toList());
    }

    public Optional<Kit> getBestPriorityKit(Player player, String kitName) {
        if (!this.hasKit(kitName)) {
            return Optional.empty();
        } else {
            Kit requestedKit = this.getKit(kitName);
            if (!player.hasPermission("holykits.kit." + kitName)) {
                return Optional.empty();
            } else if (!requestedKit.isValid()) {
                return Optional.empty();
            } else if (requestedKit.getPriority() == -1) {
                return Optional.of(requestedKit);
            } else {
                int maxPriority = this.getAvailableKits(player).stream().filter((kit) -> {
                    return kit.getPriority() != -1;
                }).mapToInt(Kit::getPriority).max().orElse(Integer.MIN_VALUE);
                return requestedKit.getPriority() == maxPriority ? Optional.of(requestedKit) : Optional.empty();
            }
        }
    }

    public Kit getKit(String kitName) {
        return (Kit)this.kits.get(kitName);
    }

    public boolean hasKit(String kitName) {
        return this.kits.containsKey(kitName);
    }
}