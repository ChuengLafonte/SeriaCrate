package id.seria.crate.model;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

public class Reward {
    private final int weight;
    private final int amount;
    private final ItemStack displayItem;
    private final List<String> commands;
    private final List<String> winItemsClean;

    public Reward(int weight, int amount, ItemStack displayItem, List<String> commands, List<String> winItemsClean) {
        this.weight = weight;
        this.amount = amount;
        this.displayItem = displayItem;
        this.commands = commands != null ? commands : new ArrayList<>();
        this.winItemsClean = winItemsClean != null ? winItemsClean : new ArrayList<>();
    }

    public int getWeight() { return weight; }
    public int getAmount() { return amount; }
    public ItemStack getDisplayItem() { return displayItem; }
    public List<String> getCommands() { return commands; }
    public List<String> getWinItemsClean() { return winItemsClean; }
}