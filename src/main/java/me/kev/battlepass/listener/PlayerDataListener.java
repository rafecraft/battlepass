package me.kev.battlepass.listener;

import me.kev.battlepass.Battlepass;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.entity.Player;

public class PlayerDataListener implements Listener {
    private final Battlepass plugin;
    public PlayerDataListener(Battlepass plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player data from DB (handled by getPlayerData lazily)
        Player player = event.getPlayer();
        plugin.getBattlepassManager().getPlayerData(player);
        // Reset vanilla XP bar
        player.setExp(0f);
        player.setLevel(0);
        player.setTotalExperience(0);
        // Show battlepass progress in XP bar
        plugin.getBattlepassManager().updatePlayerXPBar(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getBattlepassManager().savePlayerData(player);
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        // Prevent vanilla XP gain
        event.setAmount(0);
    }
}
