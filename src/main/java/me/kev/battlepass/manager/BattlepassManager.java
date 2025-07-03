package me.kev.battlepass.manager;

import me.kev.battlepass.Battlepass;
import me.kev.battlepass.model.PlayerData;
import me.kev.battlepass.model.Tier;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import org.bukkit.ChatColor;

/**
 * Handles battlepass logic, player progress, tiers, and rewards.
 */
public class BattlepassManager {
    private final Battlepass plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private List<Tier> tiers;

    public BattlepassManager(Battlepass plugin) {
        this.plugin = plugin;
        loadTiersFromConfig();
    }

    private void loadTiersFromConfig() {
        List<Tier> loadedTiers = new ArrayList<>();
        ConfigurationSection section = plugin.getConfigManager().getConfig().getConfigurationSection("tiers");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int tierNumber = Integer.parseInt(key);

                    // Validate tier number bounds
                    int minTier = plugin.getConfig().getInt("validation.tier.min", 1);
                    int maxTier = plugin.getConfig().getInt("validation.tier.max", 1000);
                    if (tierNumber < minTier || tierNumber > maxTier) {
                        plugin.getLogger().warning("Invalid tier number: " + tierNumber + ". Must be between " + minTier + "-" + maxTier + ". Skipping.");
                        continue;
                    }

                    int xpRequired = section.getInt(key + ".xp_required", 0);

                    // Validate XP requirement
                    int minXP = plugin.getConfig().getInt("validation.xp.min", 0);
                    int maxXP = plugin.getConfig().getInt("validation.xp.max", 1000000);
                    int warningThreshold = plugin.getConfig().getInt("validation.xp.warning_threshold", 1000000);

                    if (xpRequired < minXP) {
                        plugin.getLogger().warning("Invalid XP requirement for tier " + tierNumber + ": " + xpRequired + ". Must be >= " + minXP + ". Setting to " + minXP + ".");
                        xpRequired = minXP;
                    }

                    if (xpRequired > maxXP) {
                        plugin.getLogger().warning("XP requirement for tier " + tierNumber + " exceeds maximum: " + xpRequired + ". Must be <= " + maxXP + ". Setting to " + maxXP + ".");
                        xpRequired = maxXP;
                    }

                    if (xpRequired >= warningThreshold) {
                        plugin.getLogger().warning("XP requirement for tier " + tierNumber + " is very high: " + xpRequired + ". Consider lowering it.");
                    }

                    List<ItemStack> rewards = new ArrayList<>();

                    // Check if reward exists and load it properly
                    if (section.contains(key + ".reward")) {
                        // Try to load as direct ItemStack first (from setReward command)
                        ItemStack directReward = section.getItemStack(key + ".reward");
                        if (directReward != null) {
                            rewards.add(directReward);
                        } else {
                            // Fall back to old nested structure format
                            ConfigurationSection rewardSection = section.getConfigurationSection(key + ".reward");
                            if (rewardSection != null) {
                                String type = rewardSection.getString("type", "CHEST");
                                int amount = Math.max(1, rewardSection.getInt("amount", 1)); // Ensure positive amount

                                try {
                                    ItemStack item = new ItemStack(Material.valueOf(type.toUpperCase()), amount);
                                    if (rewardSection.isConfigurationSection("meta")) {
                                        ConfigurationSection metaSection = rewardSection.getConfigurationSection("meta");
                                        ItemMeta meta = item.getItemMeta();
                                        if (metaSection != null && meta != null) {
                                            if (metaSection.contains("display_name")) {
                                                meta.setDisplayName(metaSection.getString("display_name"));
                                            }
                                            if (metaSection.contains("lore")) {
                                                List<String> lore = metaSection.getStringList("lore");
                                                meta.setLore(lore);
                                            }
                                            item.setItemMeta(meta);
                                        }
                                    }
                                    rewards.add(item);
                                } catch (IllegalArgumentException e) {
                                    plugin.getLogger().warning("Invalid material type for tier " + tierNumber + ": " + type + ". Using CHEST instead.");
                                    rewards.add(new ItemStack(Material.CHEST, amount));
                                }
                            }
                        }
                    }
                    loadedTiers.add(new Tier(tierNumber, xpRequired, rewards));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid tier number format: " + key + ". Must be a valid integer. Skipping.");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load tier: " + key + ", error: " + e.getMessage());
                }
            }
        }

        // Validate tier progression (each tier should require more XP than the previous)
        loadedTiers.sort((a, b) -> Integer.compare(a.getTierNumber(), b.getTierNumber()));
        for (int i = 1; i < loadedTiers.size(); i++) {
            Tier current = loadedTiers.get(i);
            Tier previous = loadedTiers.get(i - 1);
            if (current.getXpRequired() < previous.getXpRequired()) {
                plugin.getLogger().warning("Tier " + current.getTierNumber() + " requires less XP (" + current.getXpRequired() +
                    ") than tier " + previous.getTierNumber() + " (" + previous.getXpRequired() + "). This may cause issues.");
            }
        }

        this.tiers = loadedTiers;
        plugin.getLogger().info("Loaded " + loadedTiers.size() + " battlepass tiers successfully.");
    }

    public PlayerData getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        return playerDataMap.computeIfAbsent(uuid, id -> plugin.getDatabaseManager().loadPlayerData(id));
    }

    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            plugin.getDatabaseManager().savePlayerData(uuid, data);
        }
    }

    public List<Tier> getTiers() {
        return tiers;
    }

    public void saveAll() {
        for (UUID uuid : playerDataMap.keySet()) {
            PlayerData data = playerDataMap.get(uuid);
            if (data != null) {
                plugin.getDatabaseManager().savePlayerData(uuid, data);
            }
        }
    }

    /**
     * Reset a single player's battlepass data (progress, claimed rewards, etc.)
     */
    public void resetPlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
        // Save empty PlayerData to database to properly reset
        plugin.getDatabaseManager().savePlayerData(uuid, new PlayerData());
    }

    /**
     * Reset all players' battlepass data (progress, claimed rewards, etc.)
     */
    public void resetAllPlayerData() {
        playerDataMap.clear();
        // Add proper database reset for all players
        plugin.getDatabaseManager().resetAllPlayerData();
    }

    /**
     * Add battlepass XP to a player, handle tier progression.
     */
    public void addBattlepassXP(Player player, int xp) {
         // Add null checks and validation
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Attempted to add XP to null or offline player");
            return;
        }

        if (xp == 0) return; // Don't process zero XP changes

        PlayerData data = getPlayerData(player);
        if (data == null) {
            plugin.getLogger().severe("Failed to load player data for " + player.getName());
            return;
        }

        int oldTier = data.getCurrentTier();
        int newXP = Math.max(0, data.getCurrentXP() + xp); // Prevent negative XP
        data.setCurrentXP(newXP);

        // Show XP gain/loss message
        if (xp > 0) {
            player.sendMessage(plugin.getMessage("xp_gained", "xp", String.valueOf(xp)));
        } else if (xp < 0) {
            player.sendMessage(plugin.getMessage("xp_lost", "xp", String.valueOf(-xp)));
        }

        // Handle tier progression (both up and down)
        int tier = calculateTierFromXP(newXP);

        // Check if player has reached max tier
        boolean hasReachedMax = isMaxTier(tier);

        if (tier > oldTier) {
            // Tier up
            data.setCurrentTier(tier);
            if (hasReachedMax) {
                player.sendMessage(plugin.getMessage("tier_max_reached", "tier", String.valueOf(tier)));
            } else {
                player.sendMessage(plugin.getMessage("tier_reached", "tier", String.valueOf(tier)));
            }
        } else if (tier < oldTier) {
            // Tier down (but don't allow re-claiming rewards)
            data.setCurrentTier(tier);
            player.sendMessage(plugin.getMessage("tier_demoted", "tier", String.valueOf(tier)));
        }

        // Update XP bar
        updatePlayerXPBar(player);
    }

    /**
     * Check if a tier is the maximum available tier
     */
    public boolean isMaxTier(int tier) {
        if (tiers.isEmpty()) return true;
        return tier >= tiers.get(tiers.size() - 1).getTierNumber();
    }

    /**
     * Get the maximum tier number available
     */
    public int getMaxTier() {
        if (tiers.isEmpty()) return 0;
        return tiers.get(tiers.size() - 1).getTierNumber();
    }

    /**
     * Get XP required for the next tier, or -1 if max tier reached
     */
    public int getXPToNextTier(Player player) {
        PlayerData data = getPlayerData(player);
        int currentTier = data.getCurrentTier();
        int currentXP = data.getCurrentXP();

        if (isMaxTier(currentTier)) {
            return -1; // Max tier reached
        }

        int nextTier = currentTier + 1;
        int nextTierXP = plugin.getConfigManager().getConfig().getInt("tiers." + nextTier + ".xp_required", -1);

        if (nextTierXP == -1) {
            return -1; // Next tier doesn't exist
        }

        return Math.max(0, nextTierXP - currentXP);
    }

    /**
     * Calculate what tier a player should be at based on their XP
     */
    private int calculateTierFromXP(int xp) {
        int tier = 0;
        for (Tier t : tiers) {
            if (xp >= plugin.getConfigManager().getConfig().getInt("tiers." + t.getTierNumber() + ".xp_required", 0)) {
                tier = t.getTierNumber();
            } else {
                break;
            }
        }
        return tier;
    }

    /**
     * Update the player's XP bar and level to reflect battlepass progress.
     */
    public void updatePlayerXPBar(Player player) {
        PlayerData data = getPlayerData(player);
        int tier = data.getCurrentTier();
        int xp = data.getCurrentXP();
        int nextTier = tier + 1;
        int required = plugin.getConfigManager().getConfig().getInt("tiers." + nextTier + ".xp_required", -1);
        int currentTierRequired = plugin.getConfigManager().getConfig().getInt("tiers." + tier + ".xp_required", 0);
        float progress = 0f;
        if (required > 0) {
            int needed = required - currentTierRequired;
            int have = xp - currentTierRequired;
            progress = needed > 0 ? Math.min(1f, Math.max(0f, have / (float) needed)) : 1f;
        } else {
            progress = 1f; // Maxed out
        }
        player.setLevel(tier);
        player.setExp(progress);
        player.setTotalExperience(0); // Always zero vanilla XP
    }

    public void reloadTiers() {
        // Close all open battlepass GUIs before reloading
        closeAllBattlepassGUIs();
        loadTiersFromConfig();
    }

    /**
     * Close all open battlepass GUIs to force refresh after config reload
     */
    private void closeAllBattlepassGUIs() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory() != null &&
                player.getOpenInventory().getTopInventory().getHolder() instanceof me.kev.battlepass.gui.BattlepassGUI.BattlepassGUIHolder) {
                player.closeInventory();
                player.sendMessage(plugin.getMessage("gui_closed_reload"));
            }
        }
    }

    /**
     * Clean up player data from memory for offline players to prevent memory leaks
     * Note: This only removes from memory cache, database data remains intact
     */
    public void cleanupOfflinePlayers() {
        if (playerDataMap.isEmpty()) return;

        int removedCount = 0;
        List<UUID> toRemove = new ArrayList<>();

        for (UUID uuid : playerDataMap.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                // Save data before removing from memory
                PlayerData data = playerDataMap.get(uuid);
                if (data != null) {
                    plugin.getDatabaseManager().savePlayerData(uuid, data);
                }
                toRemove.add(uuid);
                removedCount++;
            }
        }

        // Remove offline players from memory
        for (UUID uuid : toRemove) {
            playerDataMap.remove(uuid);
        }

        if (removedCount > 0) {
            plugin.getLogger().info("Cleaned up " + removedCount + " offline players from memory cache");
        }
    }
}
