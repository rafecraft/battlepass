package me.kev.battlepass.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides tab completion for the /battlepass command and its subcommands.
 */
public class BattlepassTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("setTier", "setReward", "reset", "reload"));
            // Remove the page numbers - they shouldn't be in tab completion
            completions.removeIf(s -> !s.startsWith(args[0]));
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setTier")) {
            // Show placeholder instead of actual numbers
            completions.add("<tier>");
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setReward")) {
            // Show placeholder instead of actual numbers
            completions.add("<tier>");
            return completions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setTier")) {
            // Show placeholder for XP required
            completions.add("<xpRequired>");
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            // Show reset options
            completions.addAll(Arrays.asList("<player>", "confirm"));
            completions.removeIf(s -> !s.startsWith(args[1]));
            return completions;
        }
        return Collections.emptyList();
    }
}
