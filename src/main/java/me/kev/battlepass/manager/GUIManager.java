package me.kev.battlepass.manager;

import me.kev.battlepass.Battlepass;
import me.kev.battlepass.gui.BattlepassGUI;
import org.bukkit.entity.Player;

/**
 * Handles GUI state, navigation, and opening for players.
 */
public class GUIManager {
    private final Battlepass plugin;
    private final BattlepassGUI battlepassGUI;

    public GUIManager(Battlepass plugin) {
        this.plugin = plugin;
        this.battlepassGUI = new BattlepassGUI(plugin);
    }

    public BattlepassGUI getBattlepassGUI() {
        return battlepassGUI;
    }

    /**
     * Opens the battlepass GUI for the player at the specified page.
     * @return true if the page exists and was opened, false otherwise
     */
    public boolean openGUI(Player player, int page) {
        return battlepassGUI.open(player, page);
    }

    /**
     * Reload the GUI manager and its components
     */
    public void reload() {
        battlepassGUI.reload();
        plugin.getLogger().info("GUIManager reloaded successfully.");
    }
}
