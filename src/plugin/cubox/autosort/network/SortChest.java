package plugin.cubox.autosort.network;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import plugin.cubox.autosort.AutoSort;
import plugin.cubox.autosort.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SortChest {

    public final Block block;
    public final Block sign;
    public final String signText;
    public final int priority;
    public final List<Material> matList;

    public SortChest(Block block, Block sign, String signText, int priority) {
        this.block = block;
        this.sign = sign;
        this.priority = priority;
        this.signText = signText;
        this.matList = parseMaterialList(signText);
    }

    private List<Material> parseMaterialList(String str) {
        String[] parts = str.split(",");
        List<Material> result = new ArrayList<>();
        for (String id : parts) {
            if (AutoSort.customMatGroups.containsKey(id)) {
                result.addAll(AutoSort.customMatGroups.get(id));
            } else {
                result.add(Util.parseMaterial(id));
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
