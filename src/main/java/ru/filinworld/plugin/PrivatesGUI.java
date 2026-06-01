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

            List<String> coOwners = ClaimCommand.regionCoOwners.getOrDefault(r, new ArrayList<>());
            List<String> members = ClaimCommand.regionMembers.getOrDefault(r, new ArrayList<>());

            List<String> lore = new ArrayList<>();
            lore.add("§7Размер: §f" + ((int)b[2]-(int)b[0])*((int)b[3]-(int)b[1]) + " бл");
            lore.add("§7X:" + b[0] + "-" + b[2] + " Z:" + b[1] + "-" + b[3] + " Y:" + b[4] + "-" + b[5]);
            lore.add("");

            if (!coOwners.isEmpty()) {
                lore.add("§6Со-владельцы:");
                for (String co : coOwners) lore.add(" §7- §e" + co);
                lore.add("");
            }

            if (!members.isEmpty()) {
                lore.add("§aУчастники:");
                for (String m : members) lore.add(" §7- §f" + m);
                lore.add("");
            }

            if (coOwners.isEmpty() && members.isEmpty()) {
                lore.add("§7Нет доверенных игроков");
                lore.add("");
            }

            lore.add("§aЛКМ — Телепорт");
            lore.add("§cПКМ — Удалить");
            lore.add("§eShift+ЛКМ — Добавить игрока");
            lore.add("§eShift+ПКМ — Убрать игрока");

            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + r);
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§eИнфо");
        im.setLore(Arrays.asList("§7Приватов: §f" + my.size() + "/" + ClaimCommand.maxClaims,
                "", "§7Shift+ЛКМ — добавить игрока в приват",
                "§7Shift+ПКМ — убрать игрока из привата"));
        info.setItemMeta(im);
        inv.setItem(22, info);

        p.openInventory(inv);
    }
}