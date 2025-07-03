package me.kev.battlepass.listener;

import me.kev.battlepass.Battlepass;
import me.kev.battlepass.gui.BattlepassGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles clicks and navigation in the Battlepass GUI.
 */
public class BattlepassGUIListener implements Listener {
    private final Battlepass plugin;

    public BattlepassGUIListener(Battlepass plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        // Use custom holder to identify the GUI
        if (!(inv.getHolder() instanceof BattlepassGUI.BattlepassGUIHolder)) return;
        event.setCancelled(true);
        // Prevent placing or taking items
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();
        if (name == null) return;
        // Navigation
        if (name.contains("Previous Page")) {
            int page = getPageFromTitle(inv.getViewers().get(0).getOpenInventory().getTitle());
            plugin.getGUIManager().openGUI(player, page - 1);
            return;
        } else if (name.contains("Next Page")) {
            int page = getPageFromTitle(inv.getViewers().get(0).getOpenInventory().getTitle());
            plugin.getGUIManager().openGUI(player, page + 1);
            return;
        } else if (name.contains("Close")) {
            player.closeInventory();
            return;
        }
        // Reward claiming logic
        // Only allow claiming if the item is a reward and the player is eligible
        if (name.contains("Reward for Tier ")) {
            final int tierNumber;
            try {
                tierNumber = Integer.parseInt(name.replaceAll("[^0-9]", ""));
            } catch (Exception ignored) { return; }
            var manager = plugin.getBattlepassManager();
            var playerData = manager.getPlayerData(player);
            var tier = manager.getTiers().stream().filter(t -> t.getTierNumber() == tierNumber).findFirst().orElse(null);
            if (tier == null) return;
            if (playerData.getCurrentTier() < tierNumber) {
                player.sendMessage(plugin.getMessage("tier_not_reached"));
                return;
            }
            if (playerData.hasClaimed(tierNumber)) {
                player.sendMessage(plugin.getMessage("reward_already_claimed"));
                return;
            }
            // Give all rewards for this tier
            for (ItemStack reward : tier.getRewards()) {
                // Check if player has space in inventory
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage(plugin.getMessage("inventory_full"));
                    return;
                }
                player.getInventory().addItem(reward.clone());
            }
            playerData.claimTier(tierNumber);
            player.sendMessage(plugin.getMessage("reward_claimed", "tier", String.valueOf(tierNumber)));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // Refresh the GUI instead of closing it
            int currentPage = getPageFromTitle(player.getOpenInventory().getTitle());
            plugin.getGUIManager().openGUI(player, currentPage);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof BattlepassGUI.BattlepassGUIHolder)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Optionally handle inventory close if needed
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Optionally handle player quit to clean up if needed
    }

    private int getPageFromTitle(String title) {
        try {
            int start = title.indexOf("Page ") + 5;
            int end = title.indexOf(")", start);
            return Integer.parseInt(title.substring(start, end));
        } catch (Exception e) {
            return 1;
        }
    }
}
