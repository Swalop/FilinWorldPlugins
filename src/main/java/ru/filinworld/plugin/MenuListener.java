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
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        String name = item.getItemMeta().getDisplayName().replace("§e", "");

        if (name.equals("Инфо")) return;

        if (ClaimCommand.regionOwners.containsKey(name)) {
            Object[] b = ClaimCommand.regionBounds.get(name);

            // Shift+ЛКМ — добавить игрока
            if (e.isShiftClick() && e.isLeftClick()) {
                p.closeInventory();
                p.sendMessage("§aВведите в чат ник игрока для добавления в приват §e" + name);
                p.sendMessage("§7Или используйте: §e/rg add <ник>");
                return;
            }

            // Shift+ПКМ — убрать игрока
            if (e.isShiftClick() && e.isRightClick()) {
                p.closeInventory();
                p.sendMessage("§cВведите в чат ник игрока для удаления из привата §e" + name);
                p.sendMessage("§7Или используйте: §e/rg remove <ник>");
                return;
            }

            // ЛКМ — телепорт
            if (e.isLeftClick()) {
                ClaimCommand.safeTeleport(p, name);
            }

            // ПКМ — удалить
            if (e.isRightClick()) {
                p.closeInventory();
                p.performCommand("rg delete " + name);
            }
        }
    }
}