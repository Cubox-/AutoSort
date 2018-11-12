package plugin.cubox.autosort;

import java.util.UUID;

public class ProxExcep {

    String network;
    private UUID owner;
    private int distance;

    /**
     * @param owner
     * @param network
     * @param distance
     */
    public ProxExcep(UUID owner, String network, int distance) {
        this.owner = owner;
        this.network = network;
        this.distance = distance;
    }

    /**
     * @return the owner
     */
    @SuppressWarnings("unused")
    public UUID getOwner() {
        return owner;
    }

    /**
     * @return the network
     */
    @SuppressWarnings("unused")
    public String getNetwork() {
        return network;
    }

    /**
     * @return the distance
     */
    public int getDistance() {
        return distance;
    }

    public String toString() {
        return "Proximity: [Owner] " + owner + " [Network] " + network + " [Dist] " + distance;
    }
}
