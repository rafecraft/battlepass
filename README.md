# Note: This will be update soon! If you find any error please report!

# Battlepass Plugin for Minecraft

A feature-rich battlepass system for Minecraft servers, designed to enhance player engagement with XP-based progression, tiered rewards, and a modern GUI interface.

## Features

- **XP-Based Progression**: Players earn XP through gameplay to unlock tiers.
- **Tiered Rewards**: Configurable rewards for each battlepass tier.
- **Modern GUI**: Intuitive and user-friendly graphical interface for players and admins.
- **Admin Controls**: Manage player progress, rewards, and configurations with ease.
- **Database Integration**: Persistent player data storage for seamless experience.
- **PlaceholderAPI Support**: Advanced customization with PlaceholderAPI integration.
- **Highly Configurable**: Customize tiers, rewards, and settings via configuration files.

---

## Commands

| Command                  | Description                                                                 | Permission          |
|--------------------------|-----------------------------------------------------------------------------|---------------------|
| `/battlepass`            | Opens the battlepass GUI for players.                                      | `battlepass.use`    |
| `/battlepass setTier`    | Sets a player's battlepass tier.                                           | `battlepass.settier`|
| `/battlepass setReward`  | Sets a reward for a specific tier.                                         | `battlepass.setreward` |
| `/battlepass reset`      | Resets a player's battlepass progress.                                     | `battlepass.reset`  |
| `/battlepass reload`     | Reloads the battlepass configuration.                                      | `battlepass.reload` |

---

## Permissions

| Permission               | Description                                                                 | Default |
|--------------------------|-----------------------------------------------------------------------------|---------|
| `battlepass.use`         | Allows players to use the `/battlepass` command.                           | `true`  |
| `battlepass.admin`       | Grants access to all admin subcommands.                                    | `op`    |
| `battlepass.reload`      | Allows reloading the battlepass configuration.                             | `op`    |
| `battlepass.settier`     | Allows setting a player's battlepass tier.                                 | `op`    |
| `battlepass.setreward`   | Allows setting a reward for a tier.                                        | `op`    |
| `battlepass.reset`       | Allows resetting a player's battlepass progress.                           | `op`    |

---

## Configuration

The plugin is highly configurable via the `config.yml` file. You can define:
- XP requirements for each tier.
- Rewards for each tier.
- GUI customization options.

---

## Installation

1. Download the plugin JAR file.
2. Place it in your server's `plugins` folder.
3. Restart your server.
4. Configure the plugin via `config.yml` as needed.

---

## PlaceholderAPI Integration

This plugin supports PlaceholderAPI for advanced customization. Use placeholders to display player progress, tiers, and rewards in other plugins.

---

## Support

For issues or feature requests, please open an issue on the [GitHub repository](#).
