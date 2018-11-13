package plugin.cubox.autosort;

import java.util.Comparator;


class StringComparator implements Comparator<Object> {

    /*
     * Should Sort the items into alphabetical order
     */
    @Override
    public int compare(Object o1, Object o2) {
        InventoryItem value1 = (InventoryItem) o1;
        InventoryItem value2 = (InventoryItem) o2;
        String itemId1 = value1.item.getType().toString();
        String itemId2 = value2.item.getType().toString();
        return itemId1.compareTo(itemId2);
    }
}
