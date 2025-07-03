package me.kev.battlepass.listener;

import me.kev.battlepass.Battlepass;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerExpChangeEvent;

/**
 * Listens for player events related to XP gain and progression.
 */
public class PlayerXPListener implements Listener {
    private final Battlepass plugin;

    public PlayerXPListener(Battlepass plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        int xp = plugin.getConfig().getInt("xp_sources.PLAYER_DEATH", 0);
        if (xp != 0) {
            plugin.getBattlepassManager().addBattlepassXP(player, xp);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        event.setDroppedExp(0); // Prevent XP orbs from dropping
        if (event.getEntity().getKiller() == null) return;
        Player killer = event.getEntity().getKiller();
        EntityType type = event.getEntityType();
        String mobName = type.name();
        int xp = plugin.getConfig().getInt("xp_sources.ENTITY_KILL." + mobName, 0);
        if (xp != 0) {
            plugin.getBattlepassManager().addBattlepassXP(killer, xp);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        event.setExpToDrop(0); // Prevent XP orbs from dropping
        Player player = event.getPlayer();
        Material mat = event.getBlock().getType();
        String blockName = mat.name();
        int xp = plugin.getConfig().getInt("xp_sources.BLOCK_BREAK." + blockName, 0);
        if (xp != 0) {
            plugin.getBattlepassManager().addBattlepassXP(player, xp);
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        event.setAmount(0); // Prevent vanilla XP gain
    }
}
