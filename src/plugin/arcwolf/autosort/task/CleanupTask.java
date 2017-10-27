package plugin.arcwolf.autosort.task;

import org.bukkit.Server;
import plugin.arcwolf.autosort.AutoSort;

public class CleanupTask implements Runnable {

    private AutoSort plugin;
    private Server server;

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
