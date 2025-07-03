package me.kev.battlepass.manager;

import me.kev.battlepass.Battlepass;
import me.kev.battlepass.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles MySQL/SQLite database operations for player data.
 */
public class DatabaseManager {
    private final Battlepass plugin;
    private Connection connection;
    private boolean useMySQL;

    // Prepared statement cache to improve performance
    private PreparedStatement savePlayerStatement;
    private PreparedStatement loadPlayerStatement;
    private PreparedStatement deleteClaimedStatement;
    private PreparedStatement insertClaimedStatement;
    private PreparedStatement loadClaimedStatement;

    public DatabaseManager(Battlepass plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        String type = plugin.getConfig().getString("database.type", "sqlite");
        useMySQL = type.equalsIgnoreCase("mysql");
        if (useMySQL) {
            connection = DriverManager.getConnection(
                plugin.getConfigManager().getMySqlUrl(),
                plugin.getConfigManager().getMySqlUser(),
                plugin.getConfigManager().getMySqlPassword()
            );
        } else {
            connection = DriverManager.getConnection(plugin.getConfigManager().getSqliteUrl());
        }
        createTables();
        prepareStatements();
    }

    /**
     * Prepare commonly used statements for better performance
     */
    private void prepareStatements() throws SQLException {
        savePlayerStatement = connection.prepareStatement(
            "REPLACE INTO battlepass_players (uuid, xp, tier) VALUES (?, ?, ?)"
        );
        loadPlayerStatement = connection.prepareStatement(
            "SELECT xp, tier FROM battlepass_players WHERE uuid = ?"
        );
        deleteClaimedStatement = connection.prepareStatement(
            "DELETE FROM battlepass_claimed WHERE uuid = ?"
        );
        insertClaimedStatement = connection.prepareStatement(
            "INSERT INTO battlepass_claimed (uuid, tier) VALUES (?, ?)"
        );
        loadClaimedStatement = connection.prepareStatement(
            "SELECT tier FROM battlepass_claimed WHERE uuid = ?"
        );
    }

    private void createTables() throws SQLException {
        String playerTable = "CREATE TABLE IF NOT EXISTS battlepass_players ("
                + "uuid VARCHAR(36) PRIMARY KEY,"
                + "xp INT NOT NULL,"
                + "tier INT NOT NULL"
                + ")";
        String claimedTable = "CREATE TABLE IF NOT EXISTS battlepass_claimed ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "tier INT NOT NULL,"
                + "PRIMARY KEY (uuid, tier)"
                + ")";
        try (Statement st = connection.createStatement()) {
            st.execute(playerTable);
            st.execute(claimedTable);
        }
    }

    public void savePlayerData(UUID uuid, PlayerData data) {
        try {
            synchronized (this) { // Ensure thread safety for DB operations
                savePlayerStatement.setString(1, uuid.toString());
                savePlayerStatement.setInt(2, data.getCurrentXP());
                savePlayerStatement.setInt(3, data.getCurrentTier());
                savePlayerStatement.executeUpdate();

                // Save claimed tiers
                deleteClaimedStatement.setString(1, uuid.toString());
                deleteClaimedStatement.executeUpdate();
                for (int tier : data.getClaimedTiers()) {
                    insertClaimedStatement.setString(1, uuid.toString());
                    insertClaimedStatement.setInt(2, tier);
                    insertClaimedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public PlayerData loadPlayerData(UUID uuid) {
        PlayerData data = new PlayerData();
        try {
            synchronized (this) { // Ensure thread safety for DB operations
                loadPlayerStatement.setString(1, uuid.toString());
                ResultSet rs = loadPlayerStatement.executeQuery();
                if (rs.next()) {
                    data.setCurrentXP(rs.getInt("xp"));
                    data.setCurrentTier(rs.getInt("tier"));
                }
                rs.close();
                // Load claimed tiers using prepared statement
                loadClaimedStatement.setString(1, uuid.toString());
                ResultSet rs2 = loadClaimedStatement.executeQuery();
                Set<Integer> claimed = new HashSet<>();
                while (rs2.next()) {
                    claimed.add(rs2.getInt("tier"));
                }
                data.getClaimedTiers().addAll(claimed);
                rs2.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player data for " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    public void close() {
        try {
            // Close prepared statements
            if (savePlayerStatement != null) savePlayerStatement.close();
            if (loadPlayerStatement != null) loadPlayerStatement.close();
            if (deleteClaimedStatement != null) deleteClaimedStatement.close();
            if (insertClaimedStatement != null) insertClaimedStatement.close();
            if (loadClaimedStatement != null) loadClaimedStatement.close();

            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reset all player data in the database
     */
    public void resetAllPlayerData() {
        try {
            // Clear all player data
            PreparedStatement clearPlayers = connection.prepareStatement("DELETE FROM battlepass_players");
            clearPlayers.executeUpdate();
            clearPlayers.close();

            // Clear all claimed rewards
            PreparedStatement clearClaimed = connection.prepareStatement("DELETE FROM battlepass_claimed");
            clearClaimed.executeUpdate();
            clearClaimed.close();

            plugin.getLogger().info("All battlepass data has been reset in the database.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to reset all player data in database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create a backup of the current database
     */
    public void createBackup() {
        if (!useMySQL) {
            // SQLite backup - copy the file
            createSQLiteBackup();
        } else {
            // MySQL backup - export data to file
            createMySQLBackup();
        }
    }

    private void createSQLiteBackup() {
        try {
            java.io.File sourceFile = new java.io.File(plugin.getDataFolder(), "battlepass.db");
            if (!sourceFile.exists()) {
                plugin.getLogger().warning("SQLite database file not found, skipping backup.");
                return;
            }

            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            java.io.File backupFile = new java.io.File(plugin.getDataFolder(), "backups/battlepass_backup_" + timestamp + ".db");

            // Create backups directory if it doesn't exist
            if (!backupFile.getParentFile().exists()) {
                backupFile.getParentFile().mkdirs();
            }

            // Copy file
            java.nio.file.Files.copy(sourceFile.toPath(), backupFile.toPath());
            plugin.getLogger().info("SQLite backup created: " + backupFile.getName());

            // Clean up old backups (keep only last 10)
            cleanupOldBackups();

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create SQLite backup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createMySQLBackup() {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            java.io.File backupFile = new java.io.File(plugin.getDataFolder(), "backups/battlepass_backup_" + timestamp + ".sql");

            // Create backups directory if it doesn't exist
            if (!backupFile.getParentFile().exists()) {
                backupFile.getParentFile().mkdirs();
            }

            StringBuilder backup = new StringBuilder();
            backup.append("-- Battlepass MySQL Backup - ").append(timestamp).append("\n\n");

            // Backup players table
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM battlepass_players");
            ResultSet rs = ps.executeQuery();
            backup.append("-- Players data\n");
            backup.append("DELETE FROM battlepass_players;\n");
            while (rs.next()) {
                backup.append("INSERT INTO battlepass_players (uuid, xp, tier) VALUES ('")
                      .append(rs.getString("uuid")).append("', ")
                      .append(rs.getInt("xp")).append(", ")
                      .append(rs.getInt("tier")).append(");\n");
            }
            rs.close();
            ps.close();

            // Backup claimed table
            ps = connection.prepareStatement("SELECT * FROM battlepass_claimed");
            rs = ps.executeQuery();
            backup.append("\n-- Claimed rewards data\n");
            backup.append("DELETE FROM battlepass_claimed;\n");
            while (rs.next()) {
                backup.append("INSERT INTO battlepass_claimed (uuid, tier) VALUES ('")
                      .append(rs.getString("uuid")).append("', ")
                      .append(rs.getInt("tier")).append(");\n");
            }
            rs.close();
            ps.close();

            // Write to file
            java.nio.file.Files.write(backupFile.toPath(), backup.toString().getBytes());
            plugin.getLogger().info("MySQL backup created: " + backupFile.getName());

            // Clean up old backups
            cleanupOldBackups();

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create MySQL backup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cleanupOldBackups() {
        try {
            java.io.File backupsDir = new java.io.File(plugin.getDataFolder(), "backups");
            if (!backupsDir.exists()) return;

            java.io.File[] backupFiles = backupsDir.listFiles((dir, name) ->
                name.startsWith("battlepass_backup_") && (name.endsWith(".db") || name.endsWith(".sql")));

            if (backupFiles != null && backupFiles.length > 10) {
                // Sort by last modified date (oldest first)
                java.util.Arrays.sort(backupFiles, (a, b) ->
                    Long.compare(a.lastModified(), b.lastModified()));

                // Delete oldest backups, keep only last 10
                for (int i = 0; i < backupFiles.length - 10; i++) {
                    if (backupFiles[i].delete()) {
                        plugin.getLogger().info("Deleted old backup: " + backupFiles[i].getName());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to cleanup old backups: " + e.getMessage());
        }
    }
}
