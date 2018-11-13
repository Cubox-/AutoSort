package plugin.cubox.autosort.network;

import org.bukkit.block.Block;

public class NetworkItem {

    public final SortNetwork network;
    public final Block chest;
    public final Block sign;

    public NetworkItem(SortNetwork network, Block chest, Block sign) {
        this.network = network;
        this.chest = chest;
        this.sign = sign;
    }
}
