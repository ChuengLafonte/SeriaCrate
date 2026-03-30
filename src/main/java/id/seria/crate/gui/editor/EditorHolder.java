package id.seria.crate.gui.editor;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class EditorHolder implements InventoryHolder {
    public enum MenuType {
        MAIN_MENU, CRATE_SETTINGS, TIER_SELECTION, REWARD_LIST,
        REWARD_EDIT, REWARD_WIN_ITEMS, REWARD_COMMANDS, BLOCK_EDITOR, HOLOGRAM_EDITOR
    }

    private final MenuType type;
    private final String crateId;
    private final String tierId;
    private final int rewardIndex;
    private Inventory inventory;

    public EditorHolder(MenuType type, String crateId, String tierId, int rewardIndex) {
        this.type = type; this.crateId = crateId; this.tierId = tierId; this.rewardIndex = rewardIndex;
    }

    public MenuType getType() { return type; }
    public String getCrateId() { return crateId; }
    public String getTierId() { return tierId; }
    public int getRewardIndex() { return rewardIndex; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}