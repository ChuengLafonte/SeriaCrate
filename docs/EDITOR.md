# In-Game Editor Guide 🛠️

**SeriaCrate** features a powerful, intuitive in-game GUI editor to help you manage your crates and rewards without ever touching a config file.

## 🚀 Opening the Editor
To open the main editor menu, run the following command (requires `seriacrate.editor` permission):
```bash
/seriacrate editor
```

## 🏗️ Managing Crates
From the main menu, you can:
- **Create New Crates**: Set a name and a physical skin (block type) for your new crate.
- **Edit Existing Crates**: Change the cost (Resin), animation type, and attached rewards for any crate.
- **Set Locations**: Interactively place crates in the world by clicking on blocks.

## 🎁 Managing Rewards
Inside the Reward Editor, you can:
- **Add Items**: Simply drag and drop items from your inventory into the editor to add them as rewards.
- **Set Probabilities**: Click on a reward to adjust its percentage chance or weight.
- **Configure Commands**: Add console commands to be executed when a player wins a particular reward.

## 💾 Saving Your Changes
Changes made in the editor are saved **instantly** to the configuration files. You don't need to run `/seriacrate reload` after using the editor.

> [!TIP]
> Always verify your reward chances! If the total percentage in a tier is less than 100%, some openings might result in "No Reward" unless configured otherwise.

---
*Return to [README](../README.md)*
