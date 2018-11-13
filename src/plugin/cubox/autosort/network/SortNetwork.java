package plugin.cubox.autosort.network;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import plugin.cubox.autosort.AutoSort;
import plugin.cubox.autosort.Util;

import java.util.*;

public class SortNetwork {

    /*
     * DepositChest
     *      Chest / Double
     *      TrapChest / Double
     *      DropSign
     *      Hopper
     *      Furnace
     *      
     * WithdrawChest
     *      Chest
     *      
     * SortChest
     *      Chest / Double
     *      TrapChest / Double
     *      Dispenser
     *      Dropper
     *      Hopper
     */

    public final UUID owner;
    public List<UUID> members = new ArrayList<>();
    public final String netName;
    public String world;

    public final List<SortChest> sortChests = new ArrayList<>();
    public final Map<Block, NetworkItem> depositChests = new HashMap<>();
    public final Map<Block, NetworkItem> withdrawChests = new HashMap<>();
    public final Map<Block, NetworkItem> dropSigns = new HashMap<>();

    public SortNetwork(UUID owner, String netName, String world) {
        this.owner = owner;
        this.netName = netName;
        this.world = world;
    }

    public SortChest findSortChest(Block block) {
        for (SortChest sc : sortChests) {
            if (sc.block.equals(block)) return sc;
        }
        return null;
    }

    public boolean sortItem(ItemStack item) { // Sort Chests by emptiest first
        if (AutoSort.emptiesFirst)
            sortChests.sort(new amountComparator(item));
        return sortItem(item, 4);
    }

    public boolean quickSortItem(ItemStack item) { // Sort Chests without empties first sort
        return sortItem(item, 4);
    }

    public boolean sortItem(ItemStack item, int minPriority) {
        for (int priority = 1; priority <= minPriority; priority++) {
            for (SortChest chest : sortChests) {
                if (chest.priority == priority) {
                    for (Material mat : chest.matList) {
                        if (mat == null) {
                            AutoSort.LOGGER.warning("----------------------------");
                            AutoSort.LOGGER.warning("The material group for chest at:");
                            AutoSort.LOGGER.warning(chest.block.getLocation().toString());
                            AutoSort.LOGGER.warning("was null!");
                            AutoSort.LOGGER.warning("Sign text follows:");
                            AutoSort.LOGGER.warning(chest.signText);
                            AutoSort.LOGGER.warning("----------------------------");
                            continue;
                        }
                        if (mat.equals(item.getType())) {
                            if (moveItemToChest(item, chest)) return true;
                        }
                    }
                }
            }
            for (SortChest chest : sortChests) { // Sorts MISC items into MISC group. References to Material AIR are used for MISC in mat group
                if (chest.priority == priority) {
                    if (chest.matList.get(0) == null) continue;
                    if (chest.matList.size() == 1 && chest.matList.get(0).equals(Material.AIR)) {
                        if (moveItemToChest(item, chest)) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean moveItemToChest(ItemStack item, SortChest chest) {
        Inventory inv = Util.getInventory(chest.block);
        if (inv != null) {
            if (!chest.block.getChunk().isLoaded()) chest.block.getChunk().load();
            if (chest.block.getChunk().isLoaded()) {
                try {
                    if (item != null) {
                        Map<Integer, ItemStack> couldntFit = inv.addItem(item);
                        if (couldntFit.isEmpty()) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    AutoSort.LOGGER.warning("[AutoSort] Error occured moving item to chest. " + chest.block.getLocation());
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }

    class amountComparator implements Comparator<Object> {

        private final ItemStack item;

        amountComparator(ItemStack item) {
            this.item = item;
        }

        @Override
        public int compare(Object o1, Object o2) {
            SortChest TypeN1 = (SortChest) o1;
            SortChest TypeN2 = (SortChest) o2;
            Integer TypeN1Value = TypeN1.getItemAmount(item);
            Integer TypeN2Value = TypeN2.getItemAmount(item);
            return TypeN1Value.compareTo(TypeN2Value);
        }
    }
}
