package plugin.arcwolf.autosort.network;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import plugin.arcwolf.autosort.AutoSort;
import plugin.arcwolf.autosort.Util;

import java.util.*;

public class SortNetwork {

    /*
     * DepositChest
     *      Chest / Double
     *      TrapChest / Double
     *      DropSign
     *      Hopper
     *      Furnace / Burning Furnace
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

    public UUID owner = null;
    public List<UUID> members = new ArrayList<UUID>();
    public String netName = "";
    public String world = "";

    public List<SortChest> sortChests = new ArrayList<SortChest>();
    public Map<Block, NetworkItem> depositChests = new HashMap<Block, NetworkItem>();
    public Map<Block, NetworkItem> withdrawChests = new HashMap<Block, NetworkItem>();
    public Map<Block, NetworkItem> dropSigns = new HashMap<Block, NetworkItem>();

    /**
     * @param owner
     * @param netName
     * @param world
     */
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
            Collections.sort(sortChests, new amountComparator(item));
        return sortItem(item, 4);
    }

    public boolean quickSortItem(ItemStack item) { // Sort Chests without empties first sort
        return sortItem(item, 4);
    }

    public boolean sortItem(ItemStack item, int minPriority) {
        for (int priority = 1; priority <= minPriority; priority++) {
            for (SortChest chest : sortChests) {
                if (chest.priority == priority) {
                    for (ItemStack mat : chest.matList) {
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
                        boolean ignoreData = true;
                        if (AutoSort.customMatGroups.containsKey(chest.signText)) {
                            for (ItemStack i : AutoSort.customMatGroups.get(chest.signText)) {
                                if (i != null && i.getDurability() > 0) {
                                    ignoreData = false;
                                }
                            }
                        }
                        if (chest.disregardDamage && ignoreData) {
                            if (mat.getType().equals(item.getType())) {
                                if (moveItemToChest(item, chest)) return true;
                            }
                        } else {
                            if (mat.getType().equals(item.getType()) && mat.getData().equals(item.getData())) {
                                if (moveItemToChest(item, chest)) return true;
                            }
                        }
                    }
                }
            }
            for (SortChest chest : sortChests) { // Sorts MISC items into MISC group. References to Material AIR are used for MISC in mat group
                if (chest.priority == priority) {
                    if (chest.matList.get(0) == null) continue;
                    if (chest.matList.size() == 1 && chest.matList.get(0).getType().equals(Material.AIR)) {
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

    public class amountComparator implements Comparator<Object> {

        private ItemStack item;

        public amountComparator(ItemStack item) {
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
