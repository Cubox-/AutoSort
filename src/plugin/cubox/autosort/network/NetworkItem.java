package plugin.cubox.autosort.network;

import org.bukkit.block.Block;

public class NetworkItem {

    public SortNetwork network = null;
    public Block chest;
    public Block sign;

    @SuppressWarnings("unused")
    public NetworkItem(SortNetwork network, Block chest, Block sign) {
        this.network = network;
        this.chest = chest;
        this.sign = sign;
    }
}
