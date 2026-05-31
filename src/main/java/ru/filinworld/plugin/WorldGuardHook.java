package ru.filinworld.plugin;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class WorldGuardHook {

    public static void createRegion(String name, Object[] bounds) {
        World world = Bukkit.getWorld((String) bounds[6]);
        if (world == null) return;

        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        if (rm == null) return;

        ProtectedRegion region = new ProtectedCuboidRegion(name,
                BukkitAdapter.asBlockVector(new org.bukkit.Location(world, (int)bounds[0], (int)bounds[4], (int)bounds[1])),
                BukkitAdapter.asBlockVector(new org.bukkit.Location(world, (int)bounds[2], (int)bounds[5], (int)bounds[3]))
        );

        rm.addRegion(region);
    }

    public static void removeRegion(String name) {
        for (World world : Bukkit.getWorlds()) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
            if (rm != null && rm.hasRegion(name)) {
                rm.removeRegion(name);
                return;
            }
        }
    }

    public static void setFlag(String name, String flagName, boolean value) {
        for (World world : Bukkit.getWorlds()) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
            if (rm != null && rm.hasRegion(name)) {
                ProtectedRegion region = rm.getRegion(name);
                if (region == null) return;
                Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                if (flag instanceof StateFlag) {
                    region.setFlag((StateFlag) flag, value ? StateFlag.State.ALLOW : StateFlag.State.DENY);
                }
            }
        }
    }
}