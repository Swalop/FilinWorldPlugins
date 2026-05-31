package ru.filinworld.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class PrivatesGUI {
    public static void openMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Мои приваты");
        List<String> my = ClaimCommand.playerRegions.getOrDefault(p.getUniqueId(), new ArrayList<>());
        int slot = 10;
        for (String r : my) {
            if (slot > 16) break;
            Object[] b = ClaimCommand.regionBounds.get(r);
            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + r);
            meta.setLore(Arrays.asList(
                    "§7Размер: §f" + ((int)b[2]-(int)b[0])*((int)b[3]-(int)b[1]) + " бл",
                    "§7X:" + b[0] + "-" + b[2] + " Z:" + b[1] + "-" + b[3],
                    "§7Y:" + b[4] + "-" + b[5],
                    "", "§aЛКМ — ТП", "§cПКМ — Удалить"
            ));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§eИнфо");
        im.setLore(Arrays.asList("§7Приватов: §f" + my.size() + "/" + ClaimCommand.maxClaims));
        info.setItemMeta(im);
        inv.setItem(22, info);
        p.openInventory(inv);
    }
}