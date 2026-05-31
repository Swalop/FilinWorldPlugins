package ru.filinworld.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.*;

public class ClaimCommand implements CommandExecutor {

    public static HashMap<UUID, String> privates = new HashMap<>();
    public static HashMap<UUID, int[]> regionBounds = new HashMap<>();
    public static HashMap<UUID, List<String>> members = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может использовать только игрок!");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (args.length == 0) {
            player.sendMessage("§6===== FilinPrivates =====");
            player.sendMessage("§e/rg claim <название> §7- Создать приват");
            player.sendMessage("§e/rg add <ник> §7- Добавить друга");
            player.sendMessage("§e/rg remove <ник> §7- Убрать друга");
            player.sendMessage("§e/rg info §7- Информация о привате");
            return true;
        }

        if (args[0].equalsIgnoreCase("claim")) {
            if (args.length < 2) {
                player.sendMessage("§cИспользуй: /rg claim <название>");
                return true;
            }
            if (privates.containsKey(playerId)) {
                player.sendMessage("§cУ вас уже есть приват: §e" + privates.get(playerId));
                return true;
            }
            String name = args[1];
            privates.put(playerId, name);
            members.putIfAbsent(playerId, new ArrayList<>());
            Location loc = player.getLocation();
            regionBounds.put(playerId, new int[]{
                    loc.getBlockX() - 10, loc.getBlockZ() - 10,
                    loc.getBlockX() + 10, loc.getBlockZ() + 10
            });
            player.sendMessage("§aПриват §e" + name + " §aсоздан! 20x20 вокруг вас.");
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (!privates.containsKey(playerId)) {
                player.sendMessage("§cУ вас нет привата!");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§cИспользуй: /rg add <ник>");
                return true;
            }
            String friend = args[1].toLowerCase();
            List<String> list = members.get(playerId);
            if (list.contains(friend)) {
                player.sendMessage("§cЭтот игрок уже добавлен!");
                return true;
            }
            list.add(friend);
            player.sendMessage("§aИгрок §e" + args[1] + " §aдобавлен в ваш приват!");
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!privates.containsKey(playerId)) {
                player.sendMessage("§cУ вас нет привата!");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§cИспользуй: /rg remove <ник>");
                return true;
            }
            String friend = args[1].toLowerCase();
            List<String> list = members.get(playerId);
            if (!list.contains(friend)) {
                player.sendMessage("§cЭтого игрока нет в вашем привате!");
                return true;
            }
            list.remove(friend);
            player.sendMessage("§aИгрок §e" + args[1] + " §aудалён из привата!");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            Location loc = player.getLocation();
            for (UUID owner : regionBounds.keySet()) {
                int[] b = regionBounds.get(owner);
                int x = loc.getBlockX();
                int z = loc.getBlockZ();
                if (x >= b[0] && x <= b[2] && z >= b[1] && z <= b[3]) {
                    player.sendMessage("§6===== Информация о привате =====");
                    player.sendMessage("§eНазвание: §f" + privates.get(owner));
                    player.sendMessage("§eВладелец: §f" + Bukkit.getOfflinePlayer(owner).getName());
                    List<String> fl = members.getOrDefault(owner, new ArrayList<>());
                    player.sendMessage("§eДрузья: §f" + (fl.isEmpty() ? "нет" : String.join(", ", fl)));
                    return true;
                }
            }
            player.sendMessage("§7Вы находитесь на нейтральной территории.");
            return true;
        }

        return true;
    }
}