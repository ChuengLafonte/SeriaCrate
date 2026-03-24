package id.seria.crate.gui.editor;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class EditorHolder implements InventoryHolder {

    public enum MenuType {
        MAIN_MENU,
        CRATE_SETTINGS,
        TIER_SELECTION,
        ITEM_EDITOR,
        BLOCK_EDITOR,
        HOLOGRAM_EDITOR // [BARU]
    }

    private final MenuType type;
    private final String crateId;
    private final String tierId;
    private Inventory inventory;

    public EditorHolder(MenuType type, String crateId, String tierId) {
        this.type = type;
        this.crateId = crateId;
        this.tierId = tierId;
    }

    public MenuType getType() { return type; }
    public String getCrateId() { return crateId; }
    public String getTierId() { return tierId; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}