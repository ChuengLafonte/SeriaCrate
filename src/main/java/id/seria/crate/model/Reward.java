package id.seria.crate.model;

import java.util.List;

import org.bukkit.inventory.ItemStack;

public class Reward {
    private String type, materialPath, displayName, enchant, command;
    private int weight, amount;
    private List<String> lore;
    private boolean hideEnchant;
    private ItemStack guiItem; // VARIABEL BARU

    public Reward(String type, String materialPath, int weight, int amount, String displayName, 
                  List<String> lore, String enchant, boolean hideEnchant, String command, ItemStack guiItem) {
        this.type = type;
        this.materialPath = materialPath;
        this.weight = weight;
        this.amount = amount;
        this.displayName = displayName;
        this.lore = lore;
        this.enchant = enchant;
        this.hideEnchant = hideEnchant;
        this.command = command;
        this.guiItem = guiItem;
    }

    public String getType() { return type; }
    public String getMaterialPath() { return materialPath; }
    public int getWeight() { return weight; }
    public int getAmount() { return amount; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public String getEnchant() { return enchant; }
    public boolean isHideEnchant() { return hideEnchant; }
    public String getCommand() { return command; }
    public ItemStack getGuiItem() { return guiItem; } // GETTER BARU
}