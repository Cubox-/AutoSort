package plugin.cubox.autosort;

import org.bukkit.inventory.ItemStack;

class InventoryItem {

    public final ItemStack item;
    public int amount;

    public InventoryItem(ItemStack item, int amount) {
        this.item = item;
        this.amount = amount;
    }
}
