package me.kev.battlepass.gui;

import me.kev.battlepass.Battlepass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import java.util.Collections;
import java.util.List;
import me.kev.battlepass.manager.BattlepassManager;
import me.kev.battlepass.model.Tier;
import me.kev.battlepass.model.PlayerData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles creation and management of the Battlepass GUI.
 */
public class BattlepassGUI {
    private final Battlepass plugin;

    // Constants to replace magic numbers
    private static final int GUI_SIZE = 54;
    private static final int TIERS_PER_PAGE = 9;
    private static final int STATS_SLOT = 4;
    private static final int CLOSE_SLOT = 49;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int REWARDS_ROW_START = 9;
    private static final int STATUS_ROW_START = 18;
    private static final int TIER_ROW_START = 27;

    // Cache for static GUI elements to reduce recreation overhead
    private static final Map<String, ItemStack> STATIC_ITEM_CACHE = new ConcurrentHashMap<>();

    // Cache keys
    private static final String CLOSE_ITEM = "close_item";
    private static final String PREV_ITEM = "prev_item";
    private static final String NEXT_ITEM = "next_item";

    public BattlepassGUI(Battlepass plugin) {
        this.plugin = plugin;
        initializeStaticItems();
    }

    /**
     * Initialize static GUI items that don't change
     */
    private void initializeStaticItems() {
        if (STATIC_ITEM_CACHE.isEmpty()) {
            // Close button
            ItemStack close = new ItemStack(Material.BARRIER);
            ItemMeta closeMeta = close.getItemMeta();
            closeMeta.setDisplayName(ChatColor.RED + "Close");
            close.setItemMeta(closeMeta);
            STATIC_ITEM_CACHE.put(CLOSE_ITEM, close);

            // Previous page arrow
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");
            prev.setItemMeta(prevMeta);
            STATIC_ITEM_CACHE.put(PREV_ITEM, prev);

            // Next page arrow
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(ChatColor.YELLOW + "Next Page");
            next.setItemMeta(nextMeta);
            STATIC_ITEM_CACHE.put(NEXT_ITEM, next);
        }
    }

    public static class BattlepassGUIHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /**
     * Opens the battlepass GUI for the given player and page.
     * @param player The player to open the GUI for.
     * @param page The page number to open.
     * @return true if the page exists and was opened, false otherwise
     */
    public boolean open(Player player, int page) {
        BattlepassManager manager = plugin.getBattlepassManager();
        List<Tier> tiers = manager.getTiers();
        if (tiers == null || tiers.isEmpty()) {
            player.sendMessage(plugin.getMessage("no_tiers_configured"));
            return false;
        }
        int maxPage = (int) Math.ceil(tiers.size() / (double) TIERS_PER_PAGE);
        if (page < 1 || page > maxPage) {
            return false;
        }
        int start = (page - 1) * TIERS_PER_PAGE;
        int end = Math.min(start + TIERS_PER_PAGE, tiers.size());
        List<Tier> pageTiers = tiers.subList(start, end);
        Inventory inv = org.bukkit.Bukkit.createInventory(new BattlepassGUIHolder(), 54, ChatColor.DARK_GREEN + "Battlepass (Page " + page + ")");
        PlayerData data = manager.getPlayerData(player);
        // First row: Center slot (slot 4) shows player stats
        ItemStack stats = new ItemStack(Material.PAPER);
        ItemMeta statsMeta = stats.getItemMeta();
        statsMeta.setDisplayName(ChatColor.GOLD + "Battlepass Stats");

        // Create enhanced stats with max tier detection
        String statsText;
        int xpToNext = manager.getXPToNextTier(player);
        if (xpToNext == -1) {
            statsText = ChatColor.YELLOW + "Tier: " + data.getCurrentTier() + " | XP: " + data.getCurrentXP() + "\n" +
                       ChatColor.GOLD + "§lMAX TIER REACHED!";
        } else {
            statsText = ChatColor.YELLOW + "Tier: " + data.getCurrentTier() + " | XP: " + data.getCurrentXP() + "\n" +
                       ChatColor.GREEN + "XP to next tier: " + xpToNext;
        }

        statsMeta.setLore(java.util.Arrays.asList(statsText.split("\n")));
        stats.setItemMeta(statsMeta);
        inv.setItem(STATS_SLOT, stats);
        // Second row: Rewards (slots 9-17)
        for (int i = 0; i < pageTiers.size(); i++) {
            Tier tier = pageTiers.get(i);
            List<ItemStack> rewards = tier.getRewards();
            ItemStack rewardItem = rewards != null && !rewards.isEmpty() ? rewards.get(0) : new ItemStack(Material.CHEST);
            ItemMeta meta = rewardItem.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + "Reward for Tier " + tier.getTierNumber());
            rewardItem.setItemMeta(meta);
            inv.setItem(REWARDS_ROW_START + i, rewardItem);
        }
        // Third row: Tier status (slots 18-26)
        for (int i = 0; i < pageTiers.size(); i++) {
            Tier tier = pageTiers.get(i);
            boolean achieved = data.getCurrentTier() >= tier.getTierNumber();
            boolean claimed = data.hasClaimed(tier.getTierNumber());

            Material mat;
            String displayName;

            if (!achieved) {
                // Not reached yet - Use config color or default to GRAY
                String lockedColor = plugin.getConfig().getString("gui.colors.locked", "GRAY").toUpperCase();
                mat = getMaterialFromColor(lockedColor + "_STAINED_GLASS_PANE", Material.GRAY_STAINED_GLASS_PANE);
                displayName = ChatColor.DARK_GRAY + "Not Achieved";
            } else if (achieved && !claimed) {
                // Reached but not claimed - Use config color or default to YELLOW
                String claimableColor = plugin.getConfig().getString("gui.colors.claimable", "YELLOW").toUpperCase();
                mat = getMaterialFromColor(claimableColor + "_STAINED_GLASS_PANE", Material.YELLOW_STAINED_GLASS_PANE);
                displayName = ChatColor.YELLOW + "Claimable!";
            } else {
                // Reached and claimed - Use config color or default to GREEN
                String achievedColor = plugin.getConfig().getString("gui.colors.achieved", "GREEN").toUpperCase();
                mat = getMaterialFromColor(achievedColor + "_STAINED_GLASS_PANE", Material.GREEN_STAINED_GLASS_PANE);
                displayName = ChatColor.GREEN + "Claimed";
            }

            ItemStack status = new ItemStack(mat);
            ItemMeta meta = status.getItemMeta();
            meta.setDisplayName(displayName);
            status.setItemMeta(meta);
            inv.setItem(STATUS_ROW_START + i, status);
        }
        // Fourth row: Minecart for tier number (slots 27-35)
        for (int i = 0; i < pageTiers.size(); i++) {
            Tier tier = pageTiers.get(i);
            ItemStack cart = new ItemStack(Material.MINECART);
            ItemMeta meta = cart.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Tier " + tier.getTierNumber());
            cart.setItemMeta(meta);
            inv.setItem(TIER_ROW_START + i, cart);
        }
        // Fifth row: Empty (slots 36-44)
        // Sixth row: Navigation (slots 45-53)
        if (page > 1) {
            inv.setItem(PREV_SLOT, STATIC_ITEM_CACHE.get(PREV_ITEM));
        }
        if (page < maxPage) {
            inv.setItem(NEXT_SLOT, STATIC_ITEM_CACHE.get(NEXT_ITEM));
        }
        inv.setItem(CLOSE_SLOT, STATIC_ITEM_CACHE.get(CLOSE_ITEM));
        player.openInventory(inv);
        return true;
    }

    /**
     * Helper method to get Material from color string with fallback
     */
    private Material getMaterialFromColor(String materialName, Material fallback) {
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + materialName + ", using fallback: " + fallback.name());
            return fallback;
        }
    }

    /**
     * Reload the GUI by clearing caches and reinitializing static items
     */
    public void reload() {
        // Clear the static item cache to force recreation with fresh config values
        STATIC_ITEM_CACHE.clear();

        // Reinitialize static items with potentially updated config values
        initializeStaticItems();

        plugin.getLogger().info("BattlepassGUI reloaded successfully.");
    }
}
