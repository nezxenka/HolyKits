package ru.nezxenka.holykits.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import ru.nezxenka.holykits.HolyKits;

public class Kit {
    private final String name;
    private int priority;
    private long cooldown;
    private final Map<Integer, ItemStack> items = new HashMap();

    public Kit(String name, int priority, long cooldown) {
        this.name = name;
        this.priority = priority;
        this.cooldown = cooldown;
    }

    public void saveFromInventory(PlayerInventory inventory) {
        for(int i = 0; i < 41; ++i) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                this.items.put(i, item.clone());
            }
        }

    }

    public void applyToInventory(PlayerInventory inventory) {
        List<ItemStack> leftItems = new ArrayList();
        Iterator var3 = this.items.entrySet().iterator();

        while(var3.hasNext()) {
            Entry<Integer, ItemStack> entry = (Entry)var3.next();
            if (inventory.getItem((Integer)entry.getKey()) == null) {
                inventory.setItem((Integer)entry.getKey(), ((ItemStack)entry.getValue()).clone());
            } else {
                leftItems.add(((ItemStack)entry.getValue()).clone());
            }
        }

        var3 = leftItems.iterator();

        while(var3.hasNext()) {
            ItemStack itemStack = (ItemStack)var3.next();
            this.addItemInventory(inventory, itemStack, inventory.getHolder().getLocation());
        }

    }

    public boolean isValid() {
        return !this.items.isEmpty();
    }

    public void addItem(int slot, ItemStack itemStack) {
        this.items.put(slot, itemStack);
    }

    public String getName() {
        return this.name;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public Map<Integer, ItemStack> getItems() {
        return new HashMap(this.items);
    }

    private void addItemInventory(Inventory inventory, ItemStack itemStack, Location location) {
        for(int id = 0; id < inventory.getStorageContents().length; ++id) {
            ItemStack item = inventory.getItem(id);
            if (item == null || item.getType().isAir()) {
                inventory.addItem(new ItemStack[]{itemStack});
                return;
            }

            if (item.isSimilar(itemStack)) {
                int count = item.getMaxStackSize() - item.getAmount();
                if (count > 0) {
                    if (itemStack.getAmount() <= count) {
                        inventory.addItem(new ItemStack[]{itemStack});
                        return;
                    }

                    ItemStack i = itemStack.clone();
                    i.setAmount(count);
                    inventory.addItem(new ItemStack[]{i});
                    itemStack.setAmount(itemStack.getAmount() - count);
                }
            }
        }

        Bukkit.getScheduler().runTask(HolyKits.getInstance(), () -> {
            location.getWorld().dropItemNaturally(location, itemStack);
        });
    }
}