package plugin.cubox.autosort.task;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import plugin.cubox.autosort.AutoSort;
import plugin.cubox.autosort.Util;
import plugin.cubox.autosort.network.NetworkItem;
import plugin.cubox.autosort.network.SortChest;
import plugin.cubox.autosort.network.SortNetwork;

import java.util.List;
import java.util.Map.Entry;

@SuppressWarnings("ConstantConditions")
public class SortTask implements Runnable {

    private final AutoSort plugin;
    private long tick = 0;

    public SortTask(AutoSort autoSort) {
        plugin = autoSort;
    }

    public void run() {
        if (!plugin.UUIDLoaded) return;
        long timer = System.currentTimeMillis();
        try {
            for (List<SortNetwork> networks : plugin.networks.values())
                // Deposit Chest Sort
                for (SortNetwork net : networks)
                    for (Entry<Block, NetworkItem> depChest : net.depositChests.entrySet()) {
                        if (depChest.getKey().getChunk().isLoaded()) {
                            if (net != null && plugin.util.isValidDepositBlock(depChest.getKey())) {
                                Inventory chest = Util.getInventory(depChest.getKey());
                                if (chest == null) continue;
                                ItemStack[] contents = chest.getContents();
                                int i;
                                ItemStack is;
                                for (i = 0; i < contents.length; i++) {
                                    is = contents[i];
                                    if (is != null) {
                                        if (net.sortItem(is)) {
                                            contents[i] = null;
                                        }
                                    }
                                }
                                chest.setContents(contents);
                            }
                        }
                    }
        } catch (Exception e) {
            AutoSort.LOGGER.warning("[AutoSort] Error in DepositChests Sort Thread");
            e.printStackTrace();
        }

        try {
            if (AutoSort.keepPriority) { // Priority Resort
                for (int i = 4; i > 1; i--) {
                    for (List<SortNetwork> networks : plugin.networks.values()) {
                        for (SortNetwork net : networks) {
                            for (SortChest chest : net.sortChests) {
                                if (chest.block.getChunk().isLoaded()) {
                                    if (chest.priority == i && plugin.util.isValidInventoryBlock(chest.block)) {
                                        Inventory inv = Util.getInventory(chest.block);
                                        if (inv != null) {
                                            ItemStack[] items = inv.getContents();
                                            ItemStack is;
                                            for (int j = 0; j < items.length; j++) {
                                                is = items[j];
                                                if (is != null) {
                                                    if (net.sortItem(is, i - 1)) {
                                                        items[j] = null;
                                                    }
                                                }
                                            }
                                            inv.setContents(items);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            AutoSort.LOGGER.warning("[AutoSort] Error in Sort Chests Sort Thread");
            e.printStackTrace();
        }

        if (AutoSort.getDebug() == 10) {
            if (tick != (System.currentTimeMillis() - timer)) {
                tick = (System.currentTimeMillis() - timer);
                System.out.println("Sort Time = " + tick + "ms");
            }
        }
    }
}
