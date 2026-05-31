package ru.filinworld.plugin;

import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.UUID;

public class ProtectionListener implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!canBuild(p, e.getBlock().getLocation())) {
            e.setCancelled(true);
            p.sendMessage("§cЧужой приват!");
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!canBuild(p, e.getBlock().getLocation())) {
            e.setCancelled(true);
            p.sendMessage("§cЧужой приват!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();

        if (item != null && item.getType() == Material.WOODEN_HOE) {
            e.setCancelled(true);
            Location loc = e.getClickedBlock() != null ? e.getClickedBlock().getLocation() : p.getLocation();
            UUID id = p.getUniqueId();
            switch (e.getAction()) {
                case RIGHT_CLICK_BLOCK:
                    ClaimCommand.pos1.put(id, loc);
                    p.sendMessage("§aТочка 1: " + loc.getBlockX() + " " + loc.getBlockZ());
                    break;
                case LEFT_CLICK_BLOCK:
                    ClaimCommand.pos2.put(id, loc);
                    p.sendMessage("§aТочка 2: " + loc.getBlockX() + " " + loc.getBlockZ());
                    break;
            }
        }
    }

    private boolean canBuild(Player p, Location loc) {
        String region = ClaimCommand.getRegionAt(loc);
        if (region == null) return true;
        UUID owner = ClaimCommand.regionOwners.get(region);
        if (owner.equals(p.getUniqueId())) return true;
        if (ClaimCommand.isCoOwner(region, p.getName())) return true;
        List<String> members = ClaimCommand.regionMembers.getOrDefault(region, new ArrayList<>());
        return members.contains(p.getName().toLowerCase());
    }
}