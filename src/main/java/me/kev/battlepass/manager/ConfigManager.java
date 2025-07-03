package me.kev.battlepass.manager;

import me.kev.battlepass.Battlepass;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.io.File;

/**
 * Handles loading and saving of config and data files.
 */
public class ConfigManager {
    private final Battlepass plugin;
    private FileConfiguration config;
    private File playerDataFile;

    public ConfigManager(Battlepass plugin) {
        this.plugin = plugin;
        // TODO: load data files when the database is done
        loadConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public File getPlayerDataFile() {
        return playerDataFile;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void saveConfig() {
        plugin.saveConfig();
    }

    public void setTierXP(int tier, int xpRequired) {
        config.set("tiers." + tier + ".xp_required", xpRequired);
        saveConfig();
    }

    public void setTierReward(int tier, ItemStack reward) {
        config.set("tiers." + tier + ".reward", reward);
        saveConfig();
    }

    public int getTierXP(int tier) {
        return config.getInt("tiers." + tier + ".xp_required", 0);
    }

    public ItemStack getTierReward(int tier) {
        return config.getItemStack("tiers." + tier + ".reward");
    }

    // SQL config
    public String getSqliteUrl() {
        return "jdbc:sqlite:" + plugin.getDataFolder() + "/battlepass.db";
    }
    public String getMySqlUrl() {
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String db = config.getString("database.mysql.database", "battlepass");
        return "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false";
    }
    public String getMySqlUser() {
        return config.getString("database.mysql.user", "root");
    }
    public String getMySqlPassword() {
        return config.getString("database.mysql.password", "");
    }
}
