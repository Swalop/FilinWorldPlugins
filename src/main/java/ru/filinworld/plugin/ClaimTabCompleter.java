package ru.filinworld.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;

public class ClaimTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "claim", "add", "addowner", "remove", "delete",
            "info", "list", "flags", "flag", "up", "down",
            "menu", "config", "help"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(input)).collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "add": case "addowner": case "remove":
                    return sender.getServer().getOnlinePlayers().stream()
                            .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "delete":
                    if (sender instanceof Player) {
                        return ClaimCommand.playerRegions
                                .getOrDefault(((Player)sender).getUniqueId(), new ArrayList<>()).stream()
                                .filter(r -> r.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                    }
                    break;
                case "flag":
                    return ClaimCommand.AVAILABLE_FLAGS.stream()
                            .filter(f -> f.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "config": return Collections.singletonList("reload");
                case "up": case "down": return Arrays.asList("5", "10", "20");
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            return Arrays.asList("on", "off").stream()
                    .filter(v -> v.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }

        return completions;
    }
}