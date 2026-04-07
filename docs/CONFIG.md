# Configuration Guide ⚙️

**SeriaCrate** is highly customizable through its YAML configuration files. This guide explains the purpose of each file and how to modify them.

## 🛠️ Main Config (`config.yml`)
The `config.yml` file contains the core settings for the plugin.

- **`resin-system`**: 
  - `type`: 'VIRTUAL' (Eco/Vault) or 'PHYSICAL' (Item-based).
  - `item`: Defines the material and metadata for physical Resin.
- **`animations`**: 
  - `default`: Set the default animation type for all crates.
- **`database`**: 
  - `type`: 'SQLITE' or 'MYSQL'.
  - Configuration for MySQL connectivity if enabled.

## 🖼️ GUI Layouts (`gui.yml`)
Modify the appearance of all in-game menus.

- **`preview-menu`**: Title and item slots for the reward preview menu.
- **`editor-menu`**: Layout for the administrative crate editor.
- **`crate-menu`**: Appearance of the menu when a player interacts with a crate.

## 💬 Messages & Translations (`messages.yml`)
Every string shown to players is fully translatable in the `messages.yml` file. Supports **MiniMessage** and **Legacy Colors**.

Example:
```yaml
prefix: "<gray>[<blue>SeriaCrate</blue>]</gray> "
no-permission: "<red>You do not have permission to do this!</red>"
resin-balance: "<gray>Your current balance: <white>%balance%</white></gray>"
```

## 🎁 Rewards Structure (`rewards/`)
Each file in this folder represents a collection of rewards (a Reward Class/Tier).

Example Reward File (`rare.yml`):
```yaml
tier: "RARE"
weight: 10
rewards:
  sword:
    display: "<yellow>Legendary Sword</yellow>"
    item: "DIAMOND_SWORD"
    commands:
      - "give %player% diamond_sword 1"
    chance: 20
```

---
*Return to [README](../README.md)*
