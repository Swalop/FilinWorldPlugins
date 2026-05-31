package ru.filinworld.plugin;

import org.bukkit.Bukkit;
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

        if (last != null && ClaimCommand.regionOwners.containsKey(last)) {
            UUID owner = ClaimCommand.regionOwners.get(last);
            Player ownerP = Bukkit.getPlayer(owner);
            if (ownerP != null && ownerP.isOnline() && !owner.equals(p.getUniqueId())) {
                ownerP.sendMessage("§7Игрок §e" + p.getName() + " §7вышел из привата §e" + last);
            }
        }

        if (current != null && ClaimCommand.regionOwners.containsKey(current)) {
            UUID owner = ClaimCommand.regionOwners.get(current);
            Player ownerP = Bukkit.getPlayer(owner);
            if (ownerP != null && ownerP.isOnline() && !owner.equals(p.getUniqueId())) {
                ownerP.sendMessage("§7Игрок §e" + p.getName() + " §7вошёл в ваш приват §e" + current);
            }
        }
    }
}