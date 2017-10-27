package plugin.arcwolf.autosort;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import plugin.arcwolf.autosort.network.SortNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CustomPlayer {

    public static ConcurrentHashMap<String, CustomPlayer> playerSettings = new ConcurrentHashMap<String, CustomPlayer>();
    public int startItemIdx = 0; // Current Start index for chest withdraw
    public int currentItemIdx = 0; // Current item to start a chest withdraw on.
    public int wantedAmount = 1; // Current amount for a withdraw
    public String netName = ""; // This networks name
    public UUID owner = null; // This networks owner
    public String playerName = ""; // The players name
    public Block block = null; // The withdraw chest block
    public Inventory withdrawInventory = null; // The inventory for withdrawing
    public List<InventoryItem> inventory = new ArrayList<InventoryItem>(400); // Full inventory of this network
    public SortNetwork sortNetwork = null;

    public static CustomPlayer getSettings(Player player) {
        playerSettings.putIfAbsent(player.getName(), new CustomPlayer());
        return playerSettings.get(player.getName());
    }

    public void clearPlayer() {
        currentItemIdx = 0;
        startItemIdx = 0;
        wantedAmount = 1;
        netName = "";
        owner = null;
        playerName = "";
        block = null;
        withdrawInventory = null;
        inventory.clear();
        sortNetwork = null;
    }

    public int findItem(ItemStack item) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).item.equals(item)) return i;
        }
        return -1;
    }
}
