package plugin.cubox.autosort.task;

import org.bukkit.Server;
import plugin.cubox.autosort.AutoSort;

public class CleanupTask implements Runnable {

    private final AutoSort plugin;
    private final Server server;

    public CleanupTask(AutoSort autoSort) {
        plugin = autoSort;
        server = plugin.getServer();
    }

    public void run() {
        if (server.getOnlinePlayers().size() > 0) {
            plugin.saveVersion6Network();
        }
    }
}
