package ru.filinworld.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§6Мои приваты")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String name = item.getItemMeta().getDisplayName().replace("§e", "");
        if (name.equals("Инфо")) return;
        if (ClaimCommand.regionOwners.containsKey(name)) {
            if (e.isLeftClick()) {
                Object[] b = ClaimCommand.regionBounds.get(name);
                p.teleport(new Location(Bukkit.getWorld((String)b[6]),
                        ((int)b[0]+(int)b[2])/2.0, (int)b[5]+1, ((int)b[1]+(int)b[3])/2.0));
                p.sendMessage("§aТП в " + name);
            }
            if (e.isRightClick()) {
                p.closeInventory();
                p.performCommand("rg delete " + name);
            }
        }
    }
}