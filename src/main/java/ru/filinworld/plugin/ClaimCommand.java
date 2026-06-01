package ru.filinworld.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.*;

public class ClaimCommand implements CommandExecutor {

    public static HashMap<String, UUID> regionOwners = new HashMap<>();
    public static HashMap<UUID, List<String>> playerRegions = new HashMap<>();
    public static HashMap<String, Object[]> regionBounds = new HashMap<>();
    public static HashMap<String, List<String>> regionMembers = new HashMap<>();
    public static HashMap<String, List<String>> regionCoOwners = new HashMap<>();
    public static HashMap<String, HashMap<String, Boolean>> regionFlags = new HashMap<>();

    public static HashMap<UUID, Location> pos1 = new HashMap<>();
    public static HashMap<UUID, Location> pos2 = new HashMap<>();

    public static int maxClaims = 3;
    public static int maxSize = 50000;
    public static int defaultYMin = 0;
    public static int defaultYMax = 255;

    public static final List<String> AVAILABLE_FLAGS = Arrays.asList(
            "pvp", "chest-access", "mob-spawning", "use", "tnt", "liquid-flow", "entry"
    );

    private static final HashMap<String, List<String>> chunkIndex = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Только игрок!"); return true; }
        Player player = (Player) sender;
        UUID pid = player.getUniqueId();

        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "help": return sendHelpDetailed(player);
            case "claim": return claim(player, pid, args);
            case "add": return add(player, pid, args);
            case "addowner": return addOwner(player, pid, args);
            case "remove": return remove(player, pid, args);
            case "delete": return delete(player, pid, args);
            case "list": return list(player, pid);
            case "info": return info(player);
            case "flags": return listFlags(player);
            case "flag": return flag(player, args);
            case "up": return expandUp(player, pid, args);
            case "down": return expandDown(player, pid, args);
            case "menu": return menu(player);
            case "kick": return kick(player, pid, args);
            case "config": return configReload(player, args);
            default: sendHelp(player); return true;
        }
    }

    // ==================== HELP ====================
    private void sendHelp(Player p) {
        p.sendMessage("§6===== FilinPrivates =====");
        p.sendMessage("§e/rg claim <название> §7- Создать приват");
        p.sendMessage("§e/rg up|down <число> §7- Изменить высоту выделения");
        p.sendMessage("§e/rg add <ник> §7- Добавить участника");
        p.sendMessage("§e/rg addowner <ник> §7- Со-владелец");
        p.sendMessage("§e/rg remove <ник> §7- Убрать игрока");
        p.sendMessage("§e/rg delete <название> §7- Удалить приват");
        p.sendMessage("§e/rg info §7- Инфо");
        p.sendMessage("§e/rg list §7- Список приватов");
        p.sendMessage("§e/rg menu §7- GUI-меню");
        p.sendMessage("§e/rg flags §7- Список флагов");
        p.sendMessage("§e/rg flag <флаг> <on/off> §7- Флаги (ADMIN+)");
        p.sendMessage("§e/rg kick <ник> §7- Выгнать из привата");
        p.sendMessage("§e/rg config reload §7- Перезагрузка (ADMIN+)");
        p.sendMessage("§e/rg help §7- Подробная справка");
        p.sendMessage("");
        p.sendMessage("§7Инструмент: §fдеревянная мотыга §7(ПКМ + ЛКМ)");
    }

    private boolean sendHelpDetailed(Player p) {
        p.sendMessage("§6=============================================");
        p.sendMessage("§6        FilinPrivates v" + FilinPrivates.getInstance().getDescription().getVersion());
        p.sendMessage("§6        Система приватных территорий");
        p.sendMessage("§6=============================================");
        p.sendMessage("");
        p.sendMessage("§eОсновные команды:");
        p.sendMessage(" §7/rg claim <название> §f- Создать приват");
        p.sendMessage("  §7Выделите территорию деревянной мотыгой (ПКМ + ЛКМ).");
        p.sendMessage(" §7/rg up|down <число> §f- Изменить высоту выделения ДО создания.");
        p.sendMessage(" §7/rg add <ник> §f- Добавить участника");
        p.sendMessage(" §7/rg addowner <ник> §f- Назначить со-владельца");
        p.sendMessage(" §7/rg remove <ник> §f- Убрать игрока");
        p.sendMessage(" §7/rg delete <название> §f- Удалить приват (только владелец)");
        p.sendMessage(" §7/rg kick <ник> §f- Выгнать игрока за границу привата");
        p.sendMessage("");
        p.sendMessage("§eИнформация:");
        p.sendMessage(" §7/rg info §f- Информация о привате под ногами");
        p.sendMessage(" §7/rg list §f- Список ваших приватов");
        p.sendMessage(" §7/rg menu §f- GUI-меню (ЛКМ-ТП, ПКМ-удалить, Shift-управление)");
        p.sendMessage(" §7/rg flags §f- Список флагов");
        p.sendMessage("");
        p.sendMessage("§eФлаги (ADMIN+): §f" + String.join(", ", AVAILABLE_FLAGS));
        p.sendMessage("§eЛимиты: §f" + maxClaims + " приватов, " + maxSize + " блоков (зависят от группы LuckPerms)");
        p.sendMessage("§eИнструмент: §fДеревянная мотыга (ПКМ + ЛКМ)");
        p.sendMessage("§6=============================================");
        return true;
    }

    // ==================== UP / DOWN ====================
    private boolean expandUp(Player p, UUID pid, String[] args) {
        if (args.length < 2) { p.sendMessage("§c/rg up <число>"); return true; }
        if (!pos1.containsKey(pid) || !pos2.containsKey(pid)) {
            p.sendMessage("§cСначала выделите территорию мотыгой!"); return true;
        }
        int amount;
        try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
            p.sendMessage("§cВведите число!"); return true;
        }
        pos1.get(pid).setY(Math.min(255, pos1.get(pid).getY() + amount));
        pos2.get(pid).setY(Math.min(255, pos2.get(pid).getY() + amount));
        p.sendMessage("§aВысота выделения поднята на " + amount + " блоков.");
        return true;
    }

    private boolean expandDown(Player p, UUID pid, String[] args) {
        if (args.length < 2) { p.sendMessage("§c/rg down <число>"); return true; }
        if (!pos1.containsKey(pid) || !pos2.containsKey(pid)) {
            p.sendMessage("§cСначала выделите территорию мотыгой!"); return true;
        }
        int amount;
        try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
            p.sendMessage("§cВведите число!"); return true;
        }
        pos1.get(pid).setY(Math.max(0, pos1.get(pid).getY() - amount));
        pos2.get(pid).setY(Math.max(0, pos2.get(pid).getY() - amount));
        p.sendMessage("§aВысота выделения опущена на " + amount + " блоков.");
        return true;
    }

    // ==================== MENU ====================
    private boolean menu(Player p) {
        PrivatesGUI.openMenu(p);
        return true;
    }

    // ==================== KICK ====================
    private boolean kick(Player p, UUID pid, String[] args) {
        if (args.length < 2) { p.sendMessage("§c/rg kick <ник>"); return true; }
        String region = getRegionAt(p.getLocation());
        if (region == null) { p.sendMessage("§cСтойте в привате!"); return true; }
        if (!regionOwners.get(region).equals(pid) && !isCoOwner(region, p.getName())) {
            p.sendMessage("§cНет прав!"); return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { p.sendMessage("§cИгрок не онлайн!"); return true; }
        if (!Objects.equals(getRegionAt(target.getLocation()), region)) {
            p.sendMessage("§cИгрок не в этом привате!"); return true;
        }
        if (target.getUniqueId().equals(pid)) { p.sendMessage("§cНельзя выгнать себя!"); return true; }
        Location loc = target.getLocation();
        Object[] b = regionBounds.get(region);
        int x = loc.getBlockX(), z = loc.getBlockZ();
        if (x - (int)b[0] < (int)b[2] - x) x = (int)b[0] - 2; else x = (int)b[2] + 2;
        if (z - (int)b[1] < (int)b[3] - z) z = (int)b[1] - 2; else z = (int)b[3] + 2;
        target.teleport(new Location(loc.getWorld(), x + 0.5, loc.getY(), z + 0.5));
        p.sendMessage("§a" + target.getName() + " выгнан из привата!");
        target.sendMessage("§cВас выгнали из привата " + region + "!");
        return true;
    }

    // ==================== CLAIM ====================
    private boolean claim(Player p, UUID pid, String[] args) {
        if (args.length < 2) { p.sendMessage("§c/rg claim <название>"); return true; }
        if (!pos1.containsKey(pid) || !pos2.containsKey(pid)) {
            p.sendMessage("§cСначала выделите территорию!"); return true;
        }
        String name = args[1].toLowerCase();
        if (regionOwners.containsKey(name)) { p.sendMessage("§cПриват уже существует!"); return true; }

        List<String> my = playerRegions.getOrDefault(pid, new ArrayList<>());
        int playerMaxClaims = FilinPrivates.getInstance().getMaxClaims(p);
        if (my.size() >= playerMaxClaims) { p.sendMessage("§cЛимит: " + playerMaxClaims); return true; }

        Location p1 = pos1.get(pid), p2 = pos2.get(pid);
        int x1 = Math.min(p1.getBlockX(), p2.getBlockX());
        int z1 = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int x2 = Math.max(p1.getBlockX(), p2.getBlockX());
        int z2 = Math.max(p1.getBlockZ(), p2.getBlockZ());
        int y1 = Math.min(p1.getBlockY(), p2.getBlockY());
        int y2 = Math.max(p1.getBlockY(), p2.getBlockY());
        int size = (x2 - x1) * (z2 - z1);
        int playerMaxSize = FilinPrivates.getInstance().getMaxSize(p);
        if (size > playerMaxSize) { p.sendMessage("§cМакс. размер: " + playerMaxSize); return true; }

        for (String exist : regionBounds.keySet()) {
            Object[] eb = regionBounds.get(exist);
            if (regionsIntersect(x1, z1, x2, z2, (int)eb[0], (int)eb[1], (int)eb[2], (int)eb[3])) {
                p.sendMessage("§cПересекается с " + exist); return true;
            }
        }

        Object[] bounds = new Object[]{x1, z1, x2, z2, y1, y2, p.getWorld().getName()};
        regionOwners.put(name, pid);
        my.add(name);
        playerRegions.put(pid, my);
        regionBounds.put(name, bounds);
        indexRegion(name, bounds);
        regionMembers.put(name, new ArrayList<>());
        regionCoOwners.put(name, new ArrayList<>());

        HashMap<String, Boolean> flags = new HashMap<>();
        for (String f : AVAILABLE_FLAGS) flags.put(f, f.equals("mob-spawning"));
        regionFlags.put(name, flags);

        pos1.remove(pid); pos2.remove(pid);

        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard"))
            WorldGuardHook.createRegion(name, bounds);

        FilinPrivates.getInstance().saveAll();
        p.sendMessage("§aПриват §e" + name + " §aсоздан! (" + size + " бл, Y:" + y1 + "-" + y2 + ")");
        return true;
    }
    // ==================== ADD ====================
    private boolean add(Player p, UUID pid, String[] args) {
        if (args.length < 2) { p.sendMessage("§c/rg add <ник>"); return true; }
        String region = getRegionAt(p.getLocation());
        if (region == null) { p.sendMessage("§cСтойте в привате!"); return true; }
        if (!regionOwners.get(region).equals(pid) && !isCoOwner(region, p.getName())) {
            p.sendMessage("§cНет прав!"); return true;
        }
        String t = args[1].toLowerCase();
        if (Bukkit.getOfflinePlayer(regionOwners.get(region)).getName().equalsIgnoreCase(t)) {
            p.sendMessage("§cВладелец!"); return true;
        }
        if (p.getName().equalsIgnoreCase(t)) { p.sendMessage("§cНельзя себя!"); return true; }
        if (regionMembers.get(region).contains(t) || isCoOwner(region, t)) {
            p.sendMessage("§cУже в привате!"); return true;
        }
        regionMembers.get(region).add(t);
        p.sendMessage("§a" + args[1] + " добавлен в " + region);
        return true;
    }

    // ==================== ADDOWNER ====================
    private boolean addOwner(Player p, UUID pid, String[] args) {
        if (args.length < 2) { p.sendMessage("§c/rg addowner <ник>"); return true; }
        String region = getRegionAt(p.getLocation());
        if (region == null || !regionOwners.get(region).equals(pid)) {
            p.sendMessage("§cТолько владелец!"); return true;
        }
        String t = args[1].toLowerCase();
        if (Bukkit.getOfflinePlayer(pid).getName().equalsIgnoreCase(t)) {
            p.sendMessage("§cВы владелец!"); return true;
        }
        if (isCoOwner(region, t)) { p.sendMessage("§cУже со-владелец!"); return true; }
        regionMembers.get(region).remove(t);
        regionCoOwners.get(region).add(t);
        p.sendMessage("§a" + args[1] + " → со-владелец " + region);
        return true;
    }

    // ==================== REMOVE ====================
    private boolean remove(Player p, UUID pid, String[] args) {
        if (args.length < 2) { p.sendMessage("§c/rg remove <ник>"); return true; }
        String region = getRegionAt(p.getLocation());
        if (region == null) { p.sendMessage("§cСтойте в привате!"); return true; }
        boolean isOwner = regionOwners.get(region).equals(pid);
        if (!isOwner && !isCoOwner(region, p.getName())) { p.sendMessage("§cНет прав!"); return true; }
        String t = args[1].toLowerCase();
        if (isOwner && isCoOwner(region, t)) {
            regionCoOwners.get(region).remove(t);
            p.sendMessage("§a" + args[1] + " снят с со-владельца.");
            return true;
        }
        if (regionMembers.get(region).remove(t)) {
            p.sendMessage("§a" + args[1] + " удалён.");
            return true;
        }
        p.sendMessage("§cНе найден!");
        return true;
    }

    // ==================== DELETE ====================
    private boolean delete(Player p, UUID pid, String[] args) {
        if (args.length < 2) { p.sendMessage("§c/rg delete <название>"); return true; }
        String name = args[1].toLowerCase();
        if (!regionOwners.containsKey(name) || !regionOwners.get(name).equals(pid)) {
            p.sendMessage("§cТолько владелец!"); return true;
        }
        unindexRegion(name);
        regionOwners.remove(name); regionBounds.remove(name);
        regionMembers.remove(name); regionCoOwners.remove(name); regionFlags.remove(name);
        playerRegions.get(pid).remove(name);
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) WorldGuardHook.removeRegion(name);
        FilinPrivates.getInstance().saveAll();
        p.sendMessage("§aПриват " + name + " удалён!");
        return true;
    }

    // ==================== LIST ====================
    private boolean list(Player p, UUID pid) {
        List<String> my = playerRegions.getOrDefault(pid, new ArrayList<>());
        if (my.isEmpty()) { p.sendMessage("§7Нет приватов."); return true; }
        p.sendMessage("§6===== Ваши приваты (" + my.size() + ") =====");
        for (String r : my) {
            Object[] b = regionBounds.get(r);
            p.sendMessage("§e" + r + " §7(X:" + b[0] + "-" + b[2] + " Z:" + b[1] + "-" + b[3] + " Y:" + b[4] + "-" + b[5] + ")");
        }
        return true;
    }

    // ==================== INFO ====================
    private boolean info(Player p) {
        String region = getRegionAt(p.getLocation());
        if (region == null) { p.sendMessage("§7Нейтральная территория."); return true; }
        Object[] b = regionBounds.get(region);
        p.sendMessage("§6===== " + region + " =====");
        p.sendMessage("§eВладелец: §f" + Bukkit.getOfflinePlayer(regionOwners.get(region)).getName());
        p.sendMessage("§eСо-владельцы: §f" + String.join(", ", regionCoOwners.getOrDefault(region, new ArrayList<>())));
        p.sendMessage("§eУчастники: §f" + String.join(", ", regionMembers.getOrDefault(region, new ArrayList<>())));
        p.sendMessage("§eКоординаты: §fX:" + b[0] + "-" + b[2] + " Z:" + b[1] + "-" + b[3] + " Y:" + b[4] + "-" + b[5]);
        return true;
    }

    // ==================== FLAGS ====================
    private boolean listFlags(Player p) {
        p.sendMessage("§6Флаги: §f" + String.join(", ", AVAILABLE_FLAGS));
        return true;
    }

    private boolean flag(Player p, String[] args) {
        if (!p.hasPermission("filinprivates.admin")) { p.sendMessage("§cADMIN+!"); return true; }
        if (args.length < 3) { p.sendMessage("§c/rg flag <флаг> <on/off>"); return true; }
        String region = getRegionAt(p.getLocation());
        if (region == null) { p.sendMessage("§cСтойте в привате!"); return true; }
        if (!AVAILABLE_FLAGS.contains(args[1].toLowerCase())) {
            p.sendMessage("§cНет такого флага! Доступные: " + String.join(", ", AVAILABLE_FLAGS));
            return true;
        }
        boolean val = args[2].equalsIgnoreCase("on");
        regionFlags.get(region).put(args[1].toLowerCase(), val);
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard"))
            WorldGuardHook.setFlag(region, args[1].toLowerCase(), val);
        p.sendMessage("§aФлаг " + args[1] + " = " + (val ? "on" : "off"));
        return true;
    }

    // ==================== CONFIG RELOAD ====================
    private boolean configReload(Player p, String[] args) {
        if (!p.hasPermission("filinprivates.admin")) { p.sendMessage("§cADMIN+!"); return true; }
        if (args.length < 2 || !args[1].equalsIgnoreCase("reload")) { p.sendMessage("§c/rg config reload"); return true; }
        FilinPrivates.getInstance().reloadConfigValues();
        p.sendMessage("§aКонфиг перезагружен!");
        return true;
    }

    // ==================== SAFE TELEPORT ====================
    public static void safeTeleport(Player p, String region) {
        Object[] b = regionBounds.get(region);
        if (b == null) return;

        World world = Bukkit.getWorld((String) b[6]);
        if (world == null) return;

        int cx = ((int)b[0] + (int)b[2]) / 2;
        int cz = ((int)b[1] + (int)b[3]) / 2;

        for (int y = (int)b[5] + 1; y >= (int)b[4]; y--) {
            Location feet = new Location(world, cx + 0.5, y, cz + 0.5);
            Location ground = feet.clone().add(0, -1, 0);
            Location head = feet.clone().add(0, 1, 0);

            Material groundMat = ground.getBlock().getType();
            if (!groundMat.isSolid() || ground.getBlock().isLiquid() ||
                    groundMat == Material.CACTUS || groundMat == Material.MAGMA_BLOCK) continue;

            if (!feet.getBlock().isEmpty() && !feet.getBlock().isLiquid()) continue;
            if (!head.getBlock().isEmpty() || !head.clone().add(0, 1, 0).getBlock().isEmpty()) continue;
            if (ground.getBlock().getType() == Material.LAVA) continue;

            p.teleport(feet);
            p.sendMessage("§aТелепорт в приват §e" + region);
            return;
        }

        Location roof = new Location(world, cx + 0.5, (int)b[5] + 2, cz + 0.5);
        while (!roof.getBlock().isEmpty() && roof.getY() < 255) roof.add(0, 1, 0);
        p.teleport(roof);
        p.sendMessage("§aТелепорт в приват §e" + region + " §7(на крышу)");
    }

    // ==================== CHUNK INDEX ====================
    public static void indexRegion(String name, Object[] bounds) {
        int cx1 = (int)bounds[0] >> 4, cz1 = (int)bounds[1] >> 4;
        int cx2 = (int)bounds[2] >> 4, cz2 = (int)bounds[3] >> 4;
        for (int cx = cx1; cx <= cx2; cx++) {
            for (int cz = cz1; cz <= cz2; cz++) {
                String chunkKey = bounds[6] + ":" + cx + ":" + cz;
                chunkIndex.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(name);
            }
        }
    }

    public static void unindexRegion(String name) {
        for (List<String> list : chunkIndex.values()) list.remove(name);
    }

    public static String getRegionAt(Location loc) {
        if (loc.getWorld() == null) return null;
        String chunkKey = loc.getWorld().getName() + ":" + (loc.getBlockX() >> 4) + ":" + (loc.getBlockZ() >> 4);
        List<String> candidates = chunkIndex.getOrDefault(chunkKey, new ArrayList<>());
        int x = loc.getBlockX(), z = loc.getBlockZ(), y = loc.getBlockY();
        for (String name : candidates) {
            Object[] b = regionBounds.get(name);
            if (b == null) continue;
            if (x >= (int)b[0] && x <= (int)b[2] && z >= (int)b[1] && z <= (int)b[3] && y >= (int)b[4] && y <= (int)b[5])
                return name;
        }
        return null;
    }

    public static boolean isCoOwner(String region, String name) {
        return regionCoOwners.getOrDefault(region, new ArrayList<>()).contains(name.toLowerCase());
    }

    private boolean regionsIntersect(int x1, int z1, int x2, int z2, int ox1, int oz1, int ox2, int oz2) {
        return !(x2 < ox1 || x1 > ox2 || z2 < oz1 || z1 > oz2);
    }
}