package me.kev.battlepass;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;
import me.kev.battlepass.command.BattlepassCommand;
import me.kev.battlepass.command.BattlepassTabCompleter;
import me.kev.battlepass.listener.PlayerXPListener;
import me.kev.battlepass.listener.PlayerDataListener;
import me.kev.battlepass.listener.BattlepassGUIListener;
import me.kev.battlepass.manager.BattlepassManager;
import me.kev.battlepass.manager.ConfigManager;
import me.kev.battlepass.manager.GUIManager;
import me.kev.battlepass.manager.DatabaseManager;

public final class Battlepass extends JavaPlugin {
    private static Battlepass instance;
    private BattlepassManager battlepassManager;
    private ConfigManager configManager;
    private GUIManager guiManager;
    private DatabaseManager databaseManager;

    // Constants to replace magic numbers
    private static final long CLEANUP_INTERVAL_TICKS = 6000L; // 5 minutes in ticks
    private static final long CLEANUP_OFFLINE_INTERVAL_TICKS = 36000L; // 30 minutes in ticks
    private static final long RESET_CONFIRMATION_TIMEOUT = 300000L; // 5 minutes in milliseconds

    // Add a public map for reset confirmations (player name or "ALL" -> timestamp)
    public java.util.Map<String, Long> resetConfirmations = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        // Initialize managers
        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.connect();
        } catch (Exception e) {
            getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        battlepassManager = new BattlepassManager(this);
        guiManager = new GUIManager(this);
        // Register command and tab completer
        PluginCommand battlepassCmd = getCommand("battlepass");
        if (battlepassCmd != null) {
            battlepassCmd.setExecutor(new BattlepassCommand(this));
            battlepassCmd.setTabCompleter(new BattlepassTabCompleter());
        }
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerXPListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);
        getServer().getPluginManager().registerEvents(new BattlepassGUIListener(this), this);

        // Start cleanup tasks
        startCleanupTasks();

        // Load config
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Save all data on shutdown
        battlepassManager.saveAll();
        if (databaseManager != null) databaseManager.close();
    }

    // Getter methods for managers
    public static Battlepass getInstance() {
        return instance;
    }

    public BattlepassManager getBattlepassManager() {
        return battlepassManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Start periodic cleanup tasks for memory management
     */
    private void startCleanupTasks() {
        // Clean up old reset confirmations every 5 minutes
        getServer().getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            resetConfirmations.entrySet().removeIf(entry ->
                now - entry.getValue() > RESET_CONFIRMATION_TIMEOUT);
        }, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);

        // Clean up offline player data every 30 minutes
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (battlepassManager != null) {
                battlepassManager.cleanupOfflinePlayers();
            }
        }, CLEANUP_OFFLINE_INTERVAL_TICKS, CLEANUP_OFFLINE_INTERVAL_TICKS);

        // Create automatic backups every 6 hours
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (databaseManager != null) {
                getLogger().info("Creating automatic backup...");
                databaseManager.createBackup();
            }
        }, 432000L, 432000L); // 432000 ticks = 6 hours
    }

    // Message helper for configurable messages with prefix support
    public String getMessage(String key, String... replacements) {
        String msg = getConfig().getString("messages." + key, "&cMessage not found: " + key);
        if (replacements != null && replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                msg = msg.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }

        // Apply prefix if enabled
        String finalMessage = org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
        if (getConfig().getBoolean("prefix.enabled", true)) {
            String prefix = getConfig().getString("prefix.text", "&8[&6BATTLEPASS&8]&r ");
            String translatedPrefix = org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix);
            finalMessage = translatedPrefix + finalMessage;
        }

        return finalMessage;
    }

    // Method to get just the prefix (useful for other plugins or integrations)
    public String getPrefix() {
        if (getConfig().getBoolean("prefix.enabled", true)) {
            String prefix = getConfig().getString("prefix.text", "&8[&6BATTLEPASS&8]&r ");
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix);
        }
        return "";
    }
}
