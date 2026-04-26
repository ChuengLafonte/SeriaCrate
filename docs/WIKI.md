# SeriaCrate Wiki

Detailed documentation for the SeriaCrate and Resin system.

## 🛠 Commands
| Command | Alias | Permission | Description |
|---------|-------|------------|-------------|
| `/opencrate` | | None | Open the crate selection GUI |
| `/resin` | `/stamina` | None | Check current resin and regen timer |
| `/scrate reload` | | `seriacrate.admin` | Reload all configurations |
| `/resinadmin set <player> <amount>` | | `seriacrate.admin` | Set a player's resin amount |
| `/resinadmin add <player> <amount>` | | `seriacrate.admin` | Add resin to a player |

## 🔑 Permissions
- `seriacrate.admin`: Full access to admin commands and configuration.

## 📊 Placeholders
Integrated with PlaceholderAPI:
- `%seriacrate_resin%`: Current resin amount.
- `%seriacrate_resin_max%`: Maximum resin capacity.
- `%seriacrate_resin_time%`: Time remaining until next resin regeneration.
- `%seriacrate_resin_cost%`: Resin cost to open a standard crate.

## ⚙️ Configuration

### config.yml
```yaml
resin:
  max: 160
  regen-time: 480 # Seconds (8 minutes per 1 resin)
  initial-amount: 160

settings:
  crate-cost: 20
  open-animation-duration: 5 # Seconds
```

### crates.yml
```yaml
crates:
  PRIME_CRATE:
    display_name: "<gold><bold>Prime Crate"
    material: GOLD_BLOCK
    lore:
      - "<gray>Cost: <blue>20 Resin"
    rewards:
      - id: LEGENDARY_SWORD
        type: MMOITEM
        category: SWORD
        chance: 0.05
      - id: 1000
        type: MONEY
        chance: 0.50
```
