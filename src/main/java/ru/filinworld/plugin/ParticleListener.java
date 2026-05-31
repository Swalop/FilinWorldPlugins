package ru.filinworld.plugin;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class ParticleListener implements Listener {

    private final HashMap<UUID, Long> lastShown = new HashMap<>();

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        String region = ClaimCommand.getRegionAt(p.getLocation());
        if (region == null) return;

        // Показываем частицы раз в 3 секунды
        long now = System.currentTimeMillis();
        if (lastShown.containsKey(p.getUniqueId()) && now - lastShown.get(p.getUniqueId()) < 3000) return;
        lastShown.put(p.getUniqueId(), now);

        Object[] b = ClaimCommand.regionBounds.get(region);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 40) { cancel(); return; }
                showBorder(p, (int)b[0], (int)b[1], (int)b[2], (int)b[3], (int)b[4], (int)b[5], (String)b[6]);
                tick++;
            }
        }.runTaskTimer(FilinPrivates.getInstance(), 0, 5);
    }

    private void showBorder(Player p, int x1, int z1, int x2, int z2, int yMin, int yMax, String worldName) {
        if (!p.getWorld().getName().equals(worldName)) return;
        Location loc = p.getLocation();
        int py = loc.getBlockY();

        // Вертикальные линии по углам
        for (int y = yMin; y <= yMax; y += 2) {
            if (Math.abs(y - py) > 15) continue;
            spawnParticle(p, x1, y, z1);
            spawnParticle(p, x2, y, z1);
            spawnParticle(p, x1, y, z2);
            spawnParticle(p, x2, y, z2);
        }

        // Горизонтальные линии
        int showY = Math.max(yMin, Math.min(yMax, py));
        for (int x = x1; x <= x2; x += 2) {
            if (Math.abs(x - loc.getBlockX()) > 20) continue;
            spawnParticle(p, x, showY, z1);
            spawnParticle(p, x, showY, z2);
        }
        for (int z = z1; z <= z2; z += 2) {
            if (Math.abs(z - loc.getBlockZ()) > 20) continue;
            spawnParticle(p, x1, showY, z);
            spawnParticle(p, x2, showY, z);
        }
    }

    private void spawnParticle(Player p, int x, int y, int z) {
        p.spawnParticle(Particle.FLAME, x + 0.5, y + 0.5, z + 0.5, 1, 0, 0, 0, 0);
    }
}