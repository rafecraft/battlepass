package me.kev.battlepass.command;

import me.kev.battlepass.Battlepass;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Handles /battlepass command and its subcommands.
 */
public class BattlepassCommand implements CommandExecutor {
    private final Battlepass plugin;

    public BattlepassCommand(Battlepass plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            // Only allow /battlepass reload from console
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                sender.sendMessage(plugin.getMessage("config_reloaded"));
                return true;
            }
            sender.sendMessage(plugin.getMessage("players_only"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            // /battlepass (open GUI page 1)
            if (!player.hasPermission("battlepass.use")) {
                player.sendMessage(plugin.getMessage("no_permission"));
                return true;
            }
            plugin.getGUIManager().openGUI(player, 1);
            return true;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("battlepass.admin")) {
                    player.sendMessage(plugin.getMessage("no_permission_reload"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getConfigManager().loadConfig(); // Reload config manager
                plugin.getBattlepassManager().reloadTiers(); // Reload tiers
                plugin.getGUIManager().reload(); // Reload GUI
                player.sendMessage(plugin.getMessage("config_gui_reloaded"));
                return true;
            }
            // Check for reset command BEFORE trying to parse as page number
            if (args[0].equalsIgnoreCase("reset")) {
                if (!player.hasPermission("battlepass.admin")) {
                    player.sendMessage(plugin.getMessage("no_permission_admin"));
                    return true;
                }
                // Handle reset without arguments - ask for confirmation
                plugin.resetConfirmations.put("ALL", System.currentTimeMillis());
                player.sendMessage(plugin.getMessage("reset_confirmation"));
                return true;
            }
            // /battlepass <page>
            try {
                int page = Integer.parseInt(args[0]);
                if (!player.hasPermission("battlepass.use")) {
                    player.sendMessage(plugin.getMessage("no_permission"));
                    return true;
                }
                if (!plugin.getGUIManager().openGUI(player, page)) {
                    player.sendMessage(plugin.getMessage("page_not_exist"));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("page_not_exist"));
            }
            return true;
        }
        if (!player.hasPermission("battlepass.admin")) {
            player.sendMessage(plugin.getMessage("no_permission_admin"));
            return true;
        }
        // /battlepass setTier <tier> <progressRequired>
        if (args[0].equalsIgnoreCase("setTier") && args.length == 3) {
            try {
                int tier = Integer.parseInt(args[1]);
                int xpRequired = Integer.parseInt(args[2]);

                // Input validation
                int minTier = plugin.getConfig().getInt("validation.tier.min", 1);
                int maxTier = plugin.getConfig().getInt("validation.tier.max", 1000);
                if (tier < minTier || tier > maxTier) {
                    player.sendMessage(plugin.getMessage("tier_range", "min", String.valueOf(minTier), "max", String.valueOf(maxTier)));
                    return true;
                }

                int minXP = plugin.getConfig().getInt("validation.xp.min", 0);
                int maxXP = plugin.getConfig().getInt("validation.xp.max", 1000000);
                if (xpRequired < minXP || xpRequired > maxXP) {
                    player.sendMessage(plugin.getMessage("xp_range", "min", String.valueOf(minXP), "max", String.valueOf(maxXP)));
                    return true;
                }

                boolean existed = plugin.getConfigManager().getConfig().contains("tiers." + tier + ".xp_required");

                // Always update the tier XP regardless of whether it existed or not
                plugin.getConfigManager().setTierXP(tier, xpRequired);
                plugin.getBattlepassManager().reloadTiers();

                if (existed) {
                    player.sendMessage(plugin.getMessage("updated_xp_tier", "tier", String.valueOf(tier), "xp", String.valueOf(xpRequired)));
                } else {
                    player.sendMessage(plugin.getMessage("set_xp_tier", "tier", String.valueOf(tier), "xp", String.valueOf(xpRequired)));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("usage_setTier"));
            }
            return true;
        }
        // /battlepass setReward <tier>
        if (args[0].equalsIgnoreCase("setReward") && args.length == 2) {
            try {
                int tier = Integer.parseInt(args[1]);

                // Input validation
                int minTier = plugin.getConfig().getInt("validation.tier.min", 1);
                int maxTier = plugin.getConfig().getInt("validation.tier.max", 1000);
                if (tier < minTier || tier > maxTier) {
                    player.sendMessage(plugin.getMessage("tier_range", "min", String.valueOf(minTier), "max", String.valueOf(maxTier)));
                    return true;
                }

                if (player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType().isAir()) {
                    player.sendMessage(plugin.getMessage("hold_item_main_hand"));
                    return true;
                }
                ItemStack item = player.getInventory().getItemInMainHand().clone();
                boolean existed = plugin.getConfigManager().getConfig().contains("tiers." + tier + ".reward");
                plugin.getConfigManager().setTierReward(tier, item);
                plugin.getBattlepassManager().reloadTiers();

                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName()
                        : item.getType().name().toLowerCase().replace("_", " ");

                if (existed) {
                    player.sendMessage(plugin.getMessage("set_reward_existing", "item", itemName, "tier", String.valueOf(tier)));
                } else {
                    player.sendMessage(plugin.getMessage("set_reward", "item", itemName, "tier", String.valueOf(tier)));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("usage_setReward"));
            }
            return true;
        }
        // /battlepass reset [<player>] [confirm]
        if (args[0].equalsIgnoreCase("reset")) {
            // Handle reset without arguments first
            if (args.length == 1) {
                plugin.resetConfirmations.put("ALL", System.currentTimeMillis());
                player.sendMessage(plugin.getMessage("reset_confirmation"));
                return true;
            }

            // Confirmation cache: Map<String, Long> (playerName or "ALL" -> timestamp)
            long now = System.currentTimeMillis();
            long timeout = 30_000L; // 30 seconds
            String target = "ALL";
            boolean isPlayerReset = false;
            if (args.length >= 2 && !args[1].equalsIgnoreCase("confirm")) {
                target = args[1].toLowerCase();
                isPlayerReset = true;
            }
            boolean isConfirm = args[args.length - 1].equalsIgnoreCase("confirm");
            String key = target;
            if (!isConfirm) {
                plugin.resetConfirmations.put(key, now);
                if (isPlayerReset) {
                    player.sendMessage(plugin.getMessage("reset_confirmation_player", "player", target));
                } else {
                    player.sendMessage(plugin.getMessage("reset_confirmation_all"));
                }
                return true;
            }
            // Confirmed: check timeout
            Long lastRequest = plugin.resetConfirmations.get(key);
            if (lastRequest == null || now - lastRequest > timeout) {
                player.sendMessage(plugin.getMessage("confirmation_expired"));
                plugin.resetConfirmations.remove(key);
                return true;
            }
            plugin.resetConfirmations.remove(key);
            if (isPlayerReset) {
                // Reset single player
                Player targetPlayer = plugin.getServer().getPlayerExact(target);
                if (targetPlayer == null) {
                    player.sendMessage(plugin.getMessage("player_not_found", "player", target));
                    return true;
                }
                plugin.getBattlepassManager().resetPlayerData(targetPlayer.getUniqueId());
                // Update the target player's XP bar
                plugin.getBattlepassManager().updatePlayerXPBar(targetPlayer);
                player.sendMessage(plugin.getMessage("battlepass_data_reset", "target", target));
            } else {
                // Reset all
                plugin.getBattlepassManager().resetAllPlayerData();
                // Update XP bar for all online players
                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    plugin.getBattlepassManager().updatePlayerXPBar(onlinePlayer);
                }
                player.sendMessage(plugin.getMessage("battlepass_data_reset_all"));
            }
            return true;
        }
        // New admin commands: setPlayerTier, removeTier, removeReward
        if (args[0].equalsIgnoreCase("setPlayerTier") && args.length == 3) {

            String targetName = args[1];
            int tier;
            try {
                tier = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("invalid_tier"));
                return true;
            }
            Player target = plugin.getServer().getPlayerExact(targetName);
            if (target == null) {
                player.sendMessage(plugin.getMessage("player_not_found", "player", targetName));
                return true;
            }

            // Input validation using configurable ranges
            int minTier = plugin.getConfig().getInt("validation.tier.min", 1);
            int maxTier = plugin.getConfig().getInt("validation.tier.max", 1000);
            if (tier < minTier || tier > maxTier) {
                player.sendMessage(plugin.getMessage("tier_range", "min", String.valueOf(minTier), "max", String.valueOf(maxTier)));
                return true;
            }

            plugin.getBattlepassManager().getPlayerData(target).setCurrentTier(tier);
            plugin.getBattlepassManager().updatePlayerXPBar(target);
            player.sendMessage(plugin.getMessage("set_player_tier_success", "player", targetName, "tier", String.valueOf(tier)));
            return true;
        }
        if (args[0].equalsIgnoreCase("removeTier") && args.length == 2) {
            if (!player.hasPermission("battlepass.removetier")) {
                player.sendMessage(plugin.getMessage("admin_no_permission"));
                return true;
            }
            int tier;
            try {
                tier = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("invalid_tier"));
                return true;
            }
            if (!plugin.getConfigManager().getConfig().contains("tiers." + tier)) {
                player.sendMessage(plugin.getMessage("tier_not_exist", "tier", String.valueOf(tier)));
                return true;
            }
            plugin.getConfigManager().getConfig().set("tiers." + tier, null);
            plugin.getConfigManager().saveConfig();
            plugin.getBattlepassManager().reloadTiers();
            player.sendMessage(plugin.getMessage("remove_tier_success", "tier", String.valueOf(tier)));
            return true;
        }

        if (args[0].equalsIgnoreCase("removeReward") && args.length == 2) {
            if (!player.hasPermission("battlepass.removereward")) {
                player.sendMessage(plugin.getMessage("admin_no_permission"));
                return true;
            }
            int tier;
            try {
                tier = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("invalid_tier"));
                return true;
            }
            if (!plugin.getConfigManager().getConfig().contains("tiers." + tier + ".reward")) {
                player.sendMessage(plugin.getMessage("tier_not_exist", "tier", String.valueOf(tier)));
                return true;
            }
            plugin.getConfigManager().getConfig().set("tiers." + tier + ".reward", null);
            plugin.getConfigManager().saveConfig();
            plugin.getBattlepassManager().reloadTiers();
            player.sendMessage(plugin.getMessage("remove_reward_success", "tier", String.valueOf(tier)));
            return true;
        }

        player.sendMessage(plugin.getMessage("unknown_command"));
        return true;
    }
}
