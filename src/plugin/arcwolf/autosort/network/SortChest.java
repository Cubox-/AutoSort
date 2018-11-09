package plugin.arcwolf.autosort.network;

import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import plugin.arcwolf.autosort.AutoSort;
import plugin.arcwolf.autosort.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SortChest {

    public Block block;
    public Block sign;
    public String signText = "";
    public int priority;
    public List<ItemStack> matList;
    public boolean disregardDamage;

    public SortChest(Block block, Block sign, String signText, int priority, boolean disregardDamage) {
        this.block = block;
        this.sign = sign;
        this.priority = priority;
        this.signText = signText;
        this.matList = parseMaterialIDList(signText);
        this.disregardDamage = disregardDamage;
    }

    private List<ItemStack> parseMaterialIDList(String str) {
        String[] parts = str.split(",");
        List<ItemStack> result = new ArrayList<>();
        for (String id : parts) {
            if (AutoSort.customMatGroups.containsKey(id)) {
                result.addAll(AutoSort.customMatGroups.get(id));
            } else {
                result.add(Util.parseMaterialID(id));
            }
        }
        return result;
    }

    public int getItemAmount(ItemStack item) {
        Inventory inv = Util.getInventory(block);
        int amount = 0;
        try {
            if (inv != null) {
                Map<Integer, ? extends ItemStack> allItems = inv.all(item.getType());
                Collection<? extends ItemStack> values = allItems.values();
                for (ItemStack i : values) {
                    amount += i.getAmount();
                }
            }
        } catch (Exception e) {
            return amount;
        }
        return amount;
    }
}
