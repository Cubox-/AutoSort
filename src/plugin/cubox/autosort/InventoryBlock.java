package plugin.cubox.autosort;

@SuppressWarnings("CanBeFinal")
public class InventoryBlock {

    private int itemId, dataValue;
    private boolean usedataValue;

    public InventoryBlock(int itemId) {
        this.itemId = itemId;
        dataValue = 0;
        setUsedataValue(false);
    }

    public InventoryBlock(int itemId, int dataValue) {
        this.itemId = itemId;
        this.dataValue = dataValue;
        setUsedataValue(true);
    }

    public void setUsedataValue(boolean usedataValue) {
        this.usedataValue = usedataValue;
    }

    @Override
    public String toString() {
        return "InventoryBlock [itemId=" + itemId + ", dataValue=" + dataValue + ", usedataValue=" + usedataValue + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + dataValue;
        result = prime * result + itemId;
        result = prime * result + (usedataValue ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        InventoryBlock other = (InventoryBlock) obj;
        if (dataValue != other.dataValue) return false;
        if (itemId != other.itemId) return false;
        return usedataValue == other.usedataValue;
    }
}
