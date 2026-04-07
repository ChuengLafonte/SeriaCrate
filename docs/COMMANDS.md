# Commands & Permissions 📜

All administrative and player commands for **SeriaCrate** are listed here.

## 🛠️ Administrative Commands

Admin commands require the `seriacrate.admin` permission.

### /seriacrate
The main command for managing crates.

- **`/seriacrate reload`**: Reloads all configuration files and rewards.
- **`/seriacrate editor`**: Opens the in-game GUI editor.
- **`/seriacrate list`**: Lists all active crates and their locations.
- **`/seriacrate give <player> <crate> [amount]`**: Gives a specific crate to a player.
- **`/seriacrate set <crate>`**: Sets the block you are looking at as a crate location.

### /resinadmin
Manage the Resin currency for any player.

- **`/resinadmin add <player> <amount>`**: Adds Resin to a player's balance.
- **`/resinadmin set <player> <amount>`**: Sets a player's Resin balance.
- **`/resinadmin take <player> <amount>`**: Removes Resin from a player's balance.
- **`/resinadmin view <player>`**: Views a player's Resin balance.

## 🎮 Player Commands

### /resin
Manage your own Resin currency.

- **`/resin balance`**: Shows your current Resin balance.
- **`/resin pay <player> <amount>`**: Sends Resin to another player.

## 🛡️ Permissions Table

| Permission | Description | Default |
|---|---|---|
| `seriacrate.admin` | Access to all administrative commands. | Op |
| `seriacrate.editor` | Access to the in-game crate editor. | Op |
| `seriacrate.resin.admin` | Access to `/resinadmin` commands. | Op |
| `seriacrate.preview` | Preview rewards before opening a crate. | All |
| `seriacrate.resin.use` | Permission to use/send Resin. | All |

---
*Return to [README](../README.md)*
