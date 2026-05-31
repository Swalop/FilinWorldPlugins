package ru.filinworld.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
            "pvp", "chest-access", "mob-spawning", "use", "tnt", "liquid-flow"
    );

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
            case "config": return configReload(player, args);
            default: sendHelp(player); return true;
        }
    }

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
        p.sendMessage("§e/rg config reload §7- Перезагрузка (ADMIN+)");
        p.sendMessage("§e/rg help §7- Подробная справка");
        p.sendMessage("");
        p.sendMessage("§7Инструмент: §fдеревянная мотыга §7(ПКМ + ЛКМ)");
    }

    private boolean sendHelpDetailed(Player p) {
        p.sendMessage("§6=============================================");
        p.sendMessage("§6  FilinPrivates v" + FilinPrivates.getInstance().getDescription().getVersion());
        p.sendMessage("§6=============================================");
        p.sendMessage("§e/rg up|down <число> §f- Изменяет высоту выделения ДО создания привата.");
        p.sendMessage("§e/rg menu §f- GUI со списком ваших приватов.");
        p.sendMessage("  §7ЛКМ — телепорт, ПКМ — удалить.");
        p.sendMessage("");
        p.sendMessage("§7Лимит: " + maxClaims + " | Макс. размер: " + maxSize + " блоков");
        p.sendMessage("§7Высота по умолчанию: Y:" + defaultYMin + "-" + defaultYMax);
        p.sendMessage("§6=============================================");
        return true;
    }

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

    private boolean menu(Player p) {
        PrivatesGUI.openMenu(p);
        return true;
    }

    private boolean claim(Player p, UUID pid, String[] args) {
        if (args.length < 2) { p.sendMessage("§c/rg claim <название>"); return true; }
        if (!pos1.containsKey(pid) || !pos2.containsKey(pid)) {
            p.sendMessage("§cСначала выделите территорию!"); return true;
        }
        String name = args[1].toLowerCase();
        if (regionOwners.containsKey(name)) { p.sendMessage("§cПриват уже существует!"); return true; }

        List<String> my = playerRegions.getOrDefault(pid, new ArrayList<>());
        if (my.size() >= maxClaims) { p.sendMessage("§cЛимит: " + maxClaims); return true; }

        Location p1 = pos1.get(pid), p2 = pos2.get(pid);
        int x1 = Math.min(p1.getBlockX(), p2.getBlockX());
        int z1 = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int x2 = Math.max(p1.getBlockX(), p2.getBlockX());
        int z2 = Math.max(p1.getBlockZ(), p2.getBlockZ());
        int y1 = Math.min(p1.getBlockY(), p2.getBlockY());
        int y2 = Math.max(p1.getBlockY(), p2.getBlockY());
        int size = (x2 - x1) * (z2 - z1);
        if (size > maxSize) { p.sendMessage("§cМакс. размер: " + maxSize); return true; }

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
        regionMembers.put(name, new ArrayList<>());
        regionCoOwners.put(name, new ArrayList<>());

        HashMap<String, Boolean> flags = new HashMap<>();
        for (String f : AVAILABLE_FLAGS) flags.put(f, f.equals("mob-spawning"));
        regionFlags.put(name, flags);

        pos1.remove(pid); pos2.remove(pid);

        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard"))
            WorldGuardHook.createRegion(name, bounds);

        p.sendMessage("§aПриват §e" + name + " §aсоздан! (" + size + " бл, Y:" + y1 + "-" + y2 + ")");
        return true;
    }

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

    private boolean delete(Player p, UUID pid, String[] args) {
        if (args.length < 2) { p.sendMessage("§c/rg delete <название>"); return true; }
        String name = args[1].toLowerCase();
        if (!regionOwners.containsKey(name) || !regionOwners.get(name).equals(pid)) {
            p.sendMessage("§cТолько владелец!"); return true;
        }
        regionOwners.remove(name); regionBounds.remove(name);
        regionMembers.remove(name); regionCoOwners.remove(name); regionFlags.remove(name);
        playerRegions.get(pid).remove(name);
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) WorldGuardHook.removeRegion(name);
        p.sendMessage("§aПриват " + name + " удалён!");
        return true;
    }

    private boolean list(Player p, UUID pid) {
        List<String> my = playerRegions.getOrDefault(pid, new ArrayList<>());
        if (my.isEmpty()) { p.sendMessage("§7Нет приватов."); return true; }
        p.sendMessage("§6===== Ваши приваты (" + my.size() + "/" + maxClaims + ") =====");
        for (String r : my) {
            Object[] b = regionBounds.get(r);
            p.sendMessage("§e" + r + " §7(X:" + b[0] + "-" + b[2] + " Z:" + b[1] + "-" + b[3] + " Y:" + b[4] + "-" + b[5] + ")");
        }
        return true;
    }

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

    private boolean listFlags(Player p) {
        p.sendMessage("§6Флаги: §f" + String.join(", ", AVAILABLE_FLAGS));
        return true;
    }

    private boolean flag(Player p, String[] args) {
        if (!p.hasPermission("filinprivates.admin")) { p.sendMessage("§cADMIN+!"); return true; }
        if (args.length < 3) { p.sendMessage("§c/rg flag <флаг> <on/off>"); return true; }
        String region = getRegionAt(p.getLocation());
        if (region == null) { p.sendMessage("§cСтойте в привате!"); return true; }
        if (!AVAILABLE_FLAGS.contains(args[1].toLowerCase())) { p.sendMessage("§cНет такого флага!"); return true; }
        boolean val = args[2].equalsIgnoreCase("on");
        regionFlags.get(region).put(args[1].toLowerCase(), val);
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard"))
            WorldGuardHook.setFlag(region, args[1].toLowerCase(), val);
        p.sendMessage("§aФлаг " + args[1] + " = " + (val ? "on" : "off"));
        return true;
    }

    private boolean configReload(Player p, String[] args) {
        if (!p.hasPermission("filinprivates.admin")) { p.sendMessage("§cADMIN+!"); return true; }
        if (args.length < 2 || !args[1].equalsIgnoreCase("reload")) { p.sendMessage("§c/rg config reload"); return true; }
        FilinPrivates.getInstance().reloadConfigValues();
        p.sendMessage("§aКонфиг перезагружен!");
        return true;
    }

    public static String getRegionAt(Location loc) {
        int x = loc.getBlockX(), z = loc.getBlockZ(), y = loc.getBlockY();
        String world = loc.getWorld().getName();
        for (String name : regionBounds.keySet()) {
            Object[] b = regionBounds.get(name);
            if (!b[6].equals(world)) continue;
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