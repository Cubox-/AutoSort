package plugin.arcwolf.autosort.Network;

import org.bukkit.block.Block;

public class NetworkItem {

    public SortNetwork network = null;
    public Block chest;
    public Block sign;

    public NetworkItem(SortNetwork network, Block chest, Block sign) {
        this.network = network;
        this.chest = chest;
        this.sign = sign;
    }
}
