package ru.filinworld.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.*;

public class RegionEnterListener implements Listener {

    private final HashMap<UUID, String> lastRegion = new HashMap<>();

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        String current = ClaimCommand.getRegionAt(p.getLocation());
        String last = lastRegion.getOrDefault(p.getUniqueId(), null);

        if (Objects.equals(current, last)) return;
        lastRegion.put(p.getUniqueId(), current);

        // ВЫШЕЛ из привата
        if (last != null && ClaimCommand.regionOwners.containsKey(last)) {
            UUID owner = ClaimCommand.regionOwners.get(last);
            String ownerName = Bukkit.getOfflinePlayer(owner).getName();

            if (!owner.equals(p.getUniqueId())) {
                p.sendMessage("§7Вы вышли с территории игрока §e" + ownerName);
            } else {
                p.sendMessage("§7Вы вышли из своего привата §e" + last);
            }
        }

        // ВОШЁЛ в приват
        if (current != null && ClaimCommand.regionOwners.containsKey(current)) {
            UUID owner = ClaimCommand.regionOwners.get(current);
            String ownerName = Bukkit.getOfflinePlayer(owner).getName();
            boolean isOwner = owner.equals(p.getUniqueId());
            boolean isCo = ClaimCommand.isCoOwner(current, p.getName());
            boolean isMember = ClaimCommand.regionMembers.getOrDefault(current, new ArrayList<>())
                    .contains(p.getName().toLowerCase());

            // Проверка флага entry
            if (!isOwner && !isCo && !isMember) {
                HashMap<String, Boolean> flags = ClaimCommand.regionFlags.get(current);
                if (flags != null && !flags.getOrDefault("entry", true)) {
                    Location loc = p.getLocation();
                    Object[] b = ClaimCommand.regionBounds.get(current);
                    int x = loc.getBlockX(), z = loc.getBlockZ();
                    if (x - (int)b[0] < (int)b[2] - x) x = (int)b[0] - 2; else x = (int)b[2] + 2;
                    if (z - (int)b[1] < (int)b[3] - z) z = (int)b[1] - 2; else z = (int)b[3] + 2;
                    p.teleport(new Location(loc.getWorld(), x + 0.5, loc.getY(), z + 0.5));
                    p.sendMessage("§cВход в этот приват запрещён!");
                    lastRegion.put(p.getUniqueId(), null);
                    return;
                }
            }

            // Сообщение вошедшему
            if (!isOwner) {
                p.sendMessage("§7Вы вошли на территорию игрока §e" + ownerName);
            } else {
                p.sendMessage("§7Вы вошли в свой приват §e" + current);
            }
        }
    }
}