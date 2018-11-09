package plugin.arcwolf.autosort;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import plugin.arcwolf.autosort.network.NetworkItem;
import plugin.arcwolf.autosort.network.SortChest;
import plugin.arcwolf.autosort.network.SortNetwork;
import plugin.arcwolf.autosort.task.CleanupTask;
import plugin.arcwolf.autosort.task.SortTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Debug codes:
 * 0 = No debugging
 * 1 = Permissions error traps
 * 2 = Custom inventory errors
 * 3 = UUID search errors
 */

@SuppressWarnings("ConstantConditions")
public class AutoSort extends JavaPlugin {

    public static final int SAVEVERSION = 6;
    public static final Logger LOGGER = Logger.getLogger("Minecraft.AutoSort");
    public static String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    public static boolean ONLINE_UUID_CHECK = true;
    public static int httpConnectTimeout = 15000;
    public static int httpReadTimeout = 15000;
    public static ConcurrentHashMap<String, List<ItemStack>> customMatGroups = new ConcurrentHashMap<>();
    public static Map<UUID, ProxExcep> proximities = new HashMap<>();
    public static int defaultProx = 0;
    public static boolean bkError = false;
    public static boolean emptiesFirst = true;
    public static boolean keepPriority = false;
    private static int debug = 0;
    public List<Item> items = new ArrayList<>();
    public List<Item> stillItems = new ArrayList<>();
    public ConcurrentHashMap<Block, SortNetwork> allNetworkBlocks = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, List<SortNetwork>> networks = new ConcurrentHashMap<>();
    public Map<InventoryBlock, InventoryBlock> sortBlocks = new HashMap<>();
    public Map<InventoryBlock, InventoryBlock> depositBlocks = new HashMap<>();
    public Map<InventoryBlock, InventoryBlock> withdrawBlocks = new HashMap<>();
    public boolean UUIDLoaded = false;
    public boolean worldRestrict = false;
    public AutoSortListener asListener;
    public Util util;
    private boolean uuidprecache = false;
    private Server server;
    private BukkitScheduler scheduler;
private String pluginName;
    private FileConfiguration customConfig = null;
    private File customConfigFile = null;
    private CommandHandler commandHandler;
    private boolean permissionsEr = false;
    private boolean permissionsSet = false;

    public static int getDebug() {
        return debug;
    }

    public void onEnable() {
        server = this.getServer();
        scheduler = server.getScheduler();
        PluginDescriptionFile pdfFile = getDescription();
        pluginName = pdfFile.getName();
        PluginManager pm = server.getPluginManager();
        util = new Util(this);

        asListener = new AutoSortListener(this);
        commandHandler = new CommandHandler(this);

        loadConfig();

        getPermissionsPlugin();
        loadCustomGroups();
        loadInventoryBlocks();
        scheduler.runTaskLater(this, () -> {
            loadDatabase();
            LOGGER.info(pluginName + ": Autosort Database Loaded...");
        }, 1);

        pm.registerEvents(asListener, this);
        scheduler.scheduleSyncRepeatingTask(this, new SortTask(this), 5L, 10L);
        scheduler.scheduleSyncRepeatingTask(this, new CleanupTask(this), 12000L,
                getConfig().getLong("save-network-rate", 1800) * 20 // Converts time to seconds, defaults to 30 minutes.
        );
    }

    public void onDisable() {
        scheduler.cancelTasks(this);
        saveVersion6Network();
    }

    public boolean onCommand(final CommandSender sender, final Command cmd, final String commandLabel, final String[] args) {
        if (sender instanceof Player) {
            scheduler.runTaskAsynchronously(this, () -> commandHandler.inGame(sender, cmd, commandLabel, args));
            return true;
        } else {
            scheduler.runTaskAsynchronously(this, () -> commandHandler.inConsole(sender, cmd, commandLabel, args));
            return true;
        }
    }

    public boolean canAccessProtection(Player player, Block block) {
        return true;
        // The 'com.griefcraft.lwc.LWC' package has been removed from
        // the project so it will always be null, ie. true.
    }

    public boolean hasPermission(Player player, String permission) {
        getPermissionsPlugin();
        if (debug == 1) {
            if (server.getPluginManager().getPlugin("PermissionsBukkit") != null) {
                LOGGER.info("Bukkit Permissions " + permission + " " + player.hasPermission(permission));
            } else if (permissionsEr && (player.isOp() || player.hasPermission(permission))) {
                LOGGER.info("Unknown permissions plugin " + permission + " " + player.hasPermission(permission));
            } else {
                LOGGER.info("Unknown permissions plugin " + permission + " " + player.hasPermission(permission));
            }
        }
        return player.isOp() || player.hasPermission(permission);
    }

    // permissions plugin debug information
    private void getPermissionsPlugin() {
        if (server.getPluginManager().getPlugin("PermissionsBukkit") != null) {
            if (!permissionsSet) {
                LOGGER.info(pluginName + ": Bukkit permissions detected, permissions enabled...");
                permissionsSet = true;
            }
        } else {
            if (!permissionsEr) {
                LOGGER.info(pluginName + ": Unknown permissions detected, Using Generic Permissions...");
                permissionsEr = true;
            }
        }
    }

    public void loadConfig() {
        File configFile = new File(this.getDataFolder(), "config.yml");
        File dataFolder = this.getDataFolder();
        if (!dataFolder.exists() || !configFile.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
        try {
            refreshInternalConfig();
            PROFILE_URL = getConfig().getString("Profile_URL", "https://api.mojang.com/profiles/minecraft");
            ONLINE_UUID_CHECK = getConfig().getBoolean("online_UUID_Check", true);
            httpConnectTimeout = getConfig().getInt("HTTPConnectTimeout", 15000);
            httpReadTimeout = getConfig().getInt("HTTPReadTimeout", 15000);
            debug = getConfig().getInt("debug", 0);
            worldRestrict = getConfig().getBoolean("worldRestrict", false);
            emptiesFirst = getConfig().getBoolean("fill-emptier-first", false);
            defaultProx = getConfig().getInt("proximity", 0);
            keepPriority = getConfig().getBoolean("keep-priority-sorted", false);
            ConfigurationSection proxSec = getConfig().getConfigurationSection("proximity-exceptions");
            for (String owner : proxSec.getKeys(false)) {
                ConfigurationSection username = proxSec.getConfigurationSection(owner);
                for (String network : username.getKeys(false)) {
                    UUID pId = UUID.fromString(owner);
                    proximities.put(pId, new ProxExcep(pId, network, username.getInt(network)));
                }
            }
        } catch (Exception e) {
            LOGGER.warning(pluginName + ": Error loading config. Try reloading the plugin.");
            LOGGER.warning(pluginName + ": If that does not work, delete the config and try again.");
        }
    }

    public void loadCustomGroups() {
        refreshInternalConfig();
        ConfigurationSection groupSec = getConfig().getConfigurationSection("customGroups");
        Map<String, Object> groups = groupSec.getValues(false);
        for (String key : groups.keySet()) {
            List<String> idList = groupSec.getStringList(key);
            List<ItemStack> matList = new ArrayList<>();
            for (String id : idList) {
                ItemStack is = Util.parseMaterialID(id);
                matList.add(is);
            }
            customMatGroups.put(key.toUpperCase(), matList);
        }
    }

    public void refreshInternalConfig() {
        try {
            getConfig().load(new File(this.getDataFolder(), "config.yml"));
        } catch (Exception e) {
            LOGGER.warning(pluginName + ": Error loading config. Try reloading the plugin.");
            LOGGER.warning(pluginName + ": If that does not work, delete the config and try again.");
        }
    }

    public void loadInventoryBlocks() {
        refreshInternalConfig();
        ConfigurationSection ibSec = getConfig().getConfigurationSection("inventoryBlocks");
        Map<String, Object> sortType = ibSec.getValues(false);
        for (String key : sortType.keySet()) {
            List<String> idList = ibSec.getStringList(key);
            Map<InventoryBlock, InventoryBlock> ivb;
            switch (key.toUpperCase()) {
                case "DEPOSIT":
                    ivb = depositBlocks;
                    break;
                case "WITHDRAW":
                    ivb = withdrawBlocks;
                    break;
                default:
                    ivb = sortBlocks;
                    break;
            }
            for (String id : idList) {
                String[] split = id.split(":");
                if (split.length > 1) {
                    try {
                        InventoryBlock ib = new InventoryBlock(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                        ivb.put(ib, ib);
                    } catch (Exception e) {
                        AutoSort.LOGGER.warning("Error Parsing Inventory Block in " + key + " group. ID found was: " + split[0] + " " + split[1]);
                    }
                } else {
                    try {
                        InventoryBlock ib = new InventoryBlock(Integer.parseInt(id));
                        ivb.put(ib, ib);
                    } catch (Exception e) {
                        AutoSort.LOGGER.warning("Error Parsing Inventory Block in " + key + " group. ID found was: " + id);
                    }
                }
            }
        }
    }

    private Map<String, UUID> getDatabaseUUIDs() {
        Map<String, UUID> namesToUUID = new HashMap<>();
        ConfigurationSection netsSec = getCustomConfig().getConfigurationSection("Owners");
        for (String owner : netsSec.getKeys(false)) {
            ConfigurationSection netSec = netsSec.getConfigurationSection(owner);
            ConfigurationSection newnet = netSec.getConfigurationSection("NetworkNames");
            UUID ownerId = namesToUUID.get(owner);
            if (ownerId == null) {
                ownerId = FindUUID.getUUIDFromPlayerName(owner);
                if (ownerId == null) {
                    LOGGER.warning(pluginName + ": could not resolve UUID for " + owner + " dropped from database");
                    continue;
                }
                namesToUUID.put(owner, ownerId);
            }
            for (String netNameSec : newnet.getKeys(false)) {
                ConfigurationSection network = newnet.getConfigurationSection(netNameSec);
                SortNetwork net = new SortNetwork(ownerId, netNameSec, "");
                List<String> memberNames = network.getStringList("Members");
                for (String name : memberNames) {
                    UUID memberId = namesToUUID.get(name);
                    if (memberId == null) {
                        memberId = FindUUID.getUUIDFromPlayerName(name);
                        if (memberId == null) {
                            LOGGER.warning(pluginName + ": could not resolve UUID for " + name + " dropped from members of " + net.netName);
                            continue;
                        }
                        namesToUUID.put(name, memberId);
                    }
                }
            }
        }
        return namesToUUID;
    }

    public void loadDatabase() {
        int version = getCustomConfig().getInt("version", 5);
        if (version == 5) {
            LOGGER.info(pluginName + ": One moment, converting Version 5 database to Version 6 UUID database.");
            final Map<String, UUID> namesToUUID = new HashMap<>();
            scheduler.runTaskAsynchronously(this, () -> {
                LOGGER.info(pluginName + ": Precaching UUIDs for database conversion.");
                namesToUUID.putAll(getDatabaseUUIDs());
                uuidprecache = true;
            });
            scheduler.runTaskTimer(this, () -> {
                if (uuidprecache) {
                    ConfigurationSection netsSec = getCustomConfig().getConfigurationSection("Owners");
                    for (String owner : netsSec.getKeys(false)) {
                        ConfigurationSection netSec = netsSec.getConfigurationSection(owner);
                        ConfigurationSection newnet = netSec.getConfigurationSection("NetworkNames");
                        UUID ownerId = namesToUUID.get(owner);
                        if (ownerId == null) {
                            continue;
                        }
                        for (String netNameSec : newnet.getKeys(false)) {
                            ConfigurationSection network = newnet.getConfigurationSection(netNameSec);
                            SortNetwork net = new SortNetwork(ownerId, netNameSec, "");
                            List<String> memberNames = network.getStringList("Members");
                            List<UUID> memberUUIDs = new ArrayList<>();
                            for (String name : memberNames) {
                                UUID memberId = namesToUUID.get(name);
                                if (memberId == null) {
                                    continue;
                                }
                                memberUUIDs.add(memberId);
                            }
                            net.members = memberUUIDs;
                            ConfigurationSection chests = network.getConfigurationSection("Chests");
                            for (String chestLocStr : chests.getKeys(false)) {
                                ConfigurationSection cSec = chests.getConfigurationSection(chestLocStr);
                                String[] chestData = chestLocStr.split(",");
                                World world = getServer().getWorld(chestData[0].replace("(dot)", "."));
                                if (world != null) {
                                    if (!net.world.equals(world.getName())) {
                                        net.world = world.getName();
                                    }
                                    Location chestLoc = new Location(world, Integer.parseInt(chestData[1]), Integer.parseInt(chestData[2]), Integer.parseInt(chestData[3]));
                                    Block chest = chestLoc.getBlock();

                                    String signLocStr = cSec.getString("Sign");
                                    String[] signData = signLocStr.split(",");
                                    Location signLoc = new Location(world, Integer.parseInt(signData[1]), Integer.parseInt(signData[2]), Integer.parseInt(signData[3]));

                                    if (signLoc.getBlock().getState() instanceof Sign) {
                                        Block sign = signLoc.getBlock();

                                        String signText = cSec.getString("SignText");

                                        int priority = cSec.getInt("Priority");

                                        boolean disregardDamage = cSec.getBoolean("DisregardDamage");

                                        net.sortChests.add(new SortChest(chest, sign, signText, priority, disregardDamage));
                                        allNetworkBlocks.put(chest, net);
                                        allNetworkBlocks.put(util.doubleChest(chest), net);
                                        allNetworkBlocks.put(sign, net);
                                    } else {
                                        LOGGER.warning(pluginName + ": SortChest Sign Didnt exist at " + signLoc.getBlockX() + "," + signLoc.getBlockY() + "," + signLoc.getBlockZ() + ":" + signLoc.getWorld().getName());
                                    }
                                } else {
                                    LOGGER.warning(pluginName + ": Null world: " + chestData[0] + " . Does this world still exist?");
                                }
                            }
                            if (networks.containsKey(ownerId)) {
                                networks.get(ownerId).add(net);
                            } else {
                                List<SortNetwork> nets = new ArrayList<>();
                                nets.add(net);
                                networks.put(ownerId, nets);
                            }
                            try {
                                ConfigurationSection depChest = network.getConfigurationSection("DepositChests");
                                for (String depChestLocStr : depChest.getKeys(false)) {
                                    ConfigurationSection dSec = depChest.getConfigurationSection(depChestLocStr);
                                    String[] depChestData = depChestLocStr.split(",");
                                    World world = getServer().getWorld(depChestData[0].replace("(dot)", "."));
                                    if (world != null) {
                                        if (!net.world.equals(world.getName())) {
                                            net.world = world.getName();
                                        }
                                        Location depChestLoc = new Location(world, Integer.parseInt(depChestData[1]), Integer.parseInt(depChestData[2]), Integer.parseInt(depChestData[3]));
                                        Block chest = depChestLoc.getBlock();

                                        String signLocStr = dSec.getString("Sign");
                                        String[] signData = signLocStr.split(",");
                                        Location signLoc = new Location(world, Integer.parseInt(signData[1]), Integer.parseInt(signData[2]), Integer.parseInt(signData[3]));

                                        if (signLoc.getBlock().getState() instanceof Sign) {
                                            Block sign = signLoc.getBlock();
                                            NetworkItem netItem = new NetworkItem(net, chest, sign);
                                            net.depositChests.put(chest, netItem);
                                            allNetworkBlocks.put(chest, net);
                                            allNetworkBlocks.put(util.doubleChest(chest), net);
                                            allNetworkBlocks.put(sign, net);
                                        } else {
                                            LOGGER.warning(pluginName + ": SortChest Sign Didnt exist at " + signLoc.getBlockX() + "," + signLoc.getBlockY() + "," + signLoc.getBlockZ() + ":" + signLoc.getWorld().getName());
                                        }
                                    } else {
                                        LOGGER.warning(pluginName + ": Null world: " + depChestData[0] + " . Does this world still exist?");
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.warning(pluginName + ": did not find dropchest section in database");
                                LOGGER.warning(pluginName + ": it will be added next save update");
                            }

                            try {
                                ConfigurationSection wdChest = network.getConfigurationSection("WithdrawChests");
                                for (String wdChestLocStr : wdChest.getKeys(false)) {
                                    ConfigurationSection dSec = wdChest.getConfigurationSection(wdChestLocStr);
                                    String[] wdChestData = wdChestLocStr.split(",");
                                    World world = getServer().getWorld(wdChestData[0].replace("(dot)", "."));
                                    if (world != null) {
                                        if (!net.world.equals(world.getName())) {
                                            net.world = world.getName();
                                        }
                                        Location depChestLoc = new Location(world, Integer.parseInt(wdChestData[1]), Integer.parseInt(wdChestData[2]), Integer.parseInt(wdChestData[3]));
                                        Block chest = depChestLoc.getBlock();

                                        String signLocStr = dSec.getString("Sign");
                                        String[] signData = signLocStr.split(",");
                                        Location signLoc = new Location(world, Integer.parseInt(signData[1]), Integer.parseInt(signData[2]), Integer.parseInt(signData[3]));
                                        if (signLoc.getBlock().getState() instanceof Sign) {
                                            Block sign = signLoc.getBlock();
                                            NetworkItem netItem = new NetworkItem(net, chest, sign);
                                            net.withdrawChests.put(chest, netItem);
                                            allNetworkBlocks.put(chest, net);
                                            allNetworkBlocks.put(util.doubleChest(chest), net);
                                            allNetworkBlocks.put(sign, net);
                                        } else {
                                            LOGGER.warning(pluginName + ": SortChest Sign Didnt exist at " + signLoc.getBlockX() + "," + signLoc.getBlockY() + "," + signLoc.getBlockZ() + ":" + signLoc.getWorld().getName());
                                        }
                                    } else {
                                        LOGGER.warning(pluginName + ": Null world: " + wdChestData[0] + " . Does this world still exist?");
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.warning(pluginName + ": did not find withdraw chest section in database");
                                LOGGER.warning(pluginName + ": it will be added next save update");
                            }

                            try {
                                ConfigurationSection dSign = network.getConfigurationSection("DropSigns");
                                for (String dSignLocStr : dSign.getKeys(false)) {
                                    String[] dSignData = dSignLocStr.split(",");
                                    World world = getServer().getWorld(dSignData[0].replace("(dot)", "."));
                                    if (world != null) {
                                        if (!net.world.equals(world.getName())) {
                                            net.world = world.getName();
                                        }
                                        Location dSignLoc = new Location(world, Integer.parseInt(dSignData[1]), Integer.parseInt(dSignData[2]), Integer.parseInt(dSignData[3]));
                                        Block sign = dSignLoc.getBlock();
                                        if (sign.getState() instanceof Sign) {
                                            NetworkItem netItem = new NetworkItem(net, null, sign);
                                            net.dropSigns.put(sign, netItem);
                                            allNetworkBlocks.put(sign, net);
                                        } else {
                                            LOGGER.warning(pluginName + ": Drop Sign Didnt exist at " + dSignLoc.getBlockX() + "," + dSignLoc.getBlockY() + "," + dSignLoc.getBlockZ() + ":" + dSignLoc.getWorld().getName());
                                        }
                                    } else {
                                        LOGGER.warning(pluginName + ": Null world: " + dSignData[0] + " . Does this world still exist?");
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.warning(pluginName + ": did not find dropsigns section in database");
                                LOGGER.warning(pluginName + ": it will be added next save update");
                            }
                        }
                    }
                    saveVersion6Network();
                    UUIDLoaded = true;
                    LOGGER.info(pluginName + ": Finished UUID conversion. Database updated to Version 6.");
                    uuidprecache = false;
                }
            }, 20, 20);
        } else if (version == 6) {
            ConfigurationSection netsSec = getCustomConfig().getConfigurationSection("Owners");
            for (String owner : netsSec.getKeys(false)) {
                ConfigurationSection netSec = netsSec.getConfigurationSection(owner);
                ConfigurationSection newnet = netSec.getConfigurationSection("NetworkNames");
                UUID ownerId = UUID.fromString(owner);
                for (String netNameSec : newnet.getKeys(false)) {
                    ConfigurationSection network = newnet.getConfigurationSection(netNameSec);
                    SortNetwork net = new SortNetwork(ownerId, netNameSec, "");
                    List<String> memberUUIDStrings = network.getStringList("Members");
                    List<UUID> memberUUIDs = new ArrayList<>();
                    for (String uuidString : memberUUIDStrings) {
                        UUID memberId = UUID.fromString(uuidString);
                        memberUUIDs.add(memberId);
                    }
                    net.members = memberUUIDs;
                    ConfigurationSection chests = network.getConfigurationSection("Chests");
                    for (String chestLocStr : chests.getKeys(false)) {
                        ConfigurationSection cSec = chests.getConfigurationSection(chestLocStr);
                        String[] chestData = chestLocStr.split(",");
                        World world = getServer().getWorld(chestData[0].replace("(dot)", "."));
                        if (world != null) {
                            if (!net.world.equals(world.getName())) {
                                net.world = world.getName();
                            }
                            Location chestLoc = new Location(world, Integer.parseInt(chestData[1]), Integer.parseInt(chestData[2]), Integer.parseInt(chestData[3]));
                            Block chest = chestLoc.getBlock();

                            String signLocStr = cSec.getString("Sign");
                            String[] signData = signLocStr.split(",");
                            Location signLoc = new Location(world, Integer.parseInt(signData[1]), Integer.parseInt(signData[2]), Integer.parseInt(signData[3]));

                            if (signLoc.getBlock().getState() instanceof Sign) {
                                Block sign = signLoc.getBlock();

                                String signText = cSec.getString("SignText");

                                int priority = cSec.getInt("Priority");

                                boolean disregardDamage = cSec.getBoolean("DisregardDamage");

                                net.sortChests.add(new SortChest(chest, sign, signText, priority, disregardDamage));
                                allNetworkBlocks.put(chest, net);
                                allNetworkBlocks.put(util.doubleChest(chest), net);
                                allNetworkBlocks.put(sign, net);
                            } else {
                                LOGGER.warning(pluginName + ": SortChest Sign Didnt exist at " + signLoc.getBlockX() + "," + signLoc.getBlockY() + "," + signLoc.getBlockZ() + ":" + signLoc.getWorld().getName());
                            }
                        } else {
                            LOGGER.warning(pluginName + ": Null world: " + chestData[0] + " . Does this world still exist?");
                        }
                    }
                    if (networks.containsKey(ownerId)) {
                        networks.get(ownerId).add(net);
                    } else {
                        List<SortNetwork> nets = new ArrayList<>();
                        nets.add(net);
                        networks.put(ownerId, nets);
                    }
                    try {
                        ConfigurationSection depChest = network.getConfigurationSection("DepositChests");
                        for (String depChestLocStr : depChest.getKeys(false)) {
                            ConfigurationSection dSec = depChest.getConfigurationSection(depChestLocStr);
                            String[] depChestData = depChestLocStr.split(",");
                            World world = getServer().getWorld(depChestData[0].replace("(dot)", "."));
                            if (world != null) {
                                if (!net.world.equals(world.getName())) {
                                    net.world = world.getName();
                                }
                                Location depChestLoc = new Location(world, Integer.parseInt(depChestData[1]), Integer.parseInt(depChestData[2]), Integer.parseInt(depChestData[3]));
                                Block chest = depChestLoc.getBlock();

                                String signLocStr = dSec.getString("Sign");
                                String[] signData = signLocStr.split(",");
                                Location signLoc = new Location(world, Integer.parseInt(signData[1]), Integer.parseInt(signData[2]), Integer.parseInt(signData[3]));

                                if (signLoc.getBlock().getState() instanceof Sign) {
                                    Block sign = signLoc.getBlock();
                                    NetworkItem netItem = new NetworkItem(net, chest, sign);
                                    net.depositChests.put(chest, netItem);
                                    allNetworkBlocks.put(chest, net);
                                    allNetworkBlocks.put(util.doubleChest(chest), net);
                                    allNetworkBlocks.put(sign, net);
                                } else {
                                    LOGGER.warning(pluginName + ": SortChest Sign Didnt exist at " + signLoc.getBlockX() + "," + signLoc.getBlockY() + "," + signLoc.getBlockZ() + ":" + signLoc.getWorld().getName());
                                }
                            } else {
                                LOGGER.warning(pluginName + ": Null world: " + depChestData[0] + " . Does this world still exist?");
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warning(pluginName + ": did not find dropchest section in database");
                        LOGGER.warning(pluginName + ": it will be added next save update");
                    }

                    try {
                        ConfigurationSection wdChest = network.getConfigurationSection("WithdrawChests");
                        for (String wdChestLocStr : wdChest.getKeys(false)) {
                            ConfigurationSection dSec = wdChest.getConfigurationSection(wdChestLocStr);
                            String[] wdChestData = wdChestLocStr.split(",");
                            World world = getServer().getWorld(wdChestData[0].replace("(dot)", "."));
                            if (world != null) {
                                if (!net.world.equals(world.getName())) {
                                    net.world = world.getName();
                                }
                                Location depChestLoc = new Location(world, Integer.parseInt(wdChestData[1]), Integer.parseInt(wdChestData[2]), Integer.parseInt(wdChestData[3]));
                                Block chest = depChestLoc.getBlock();

                                String signLocStr = dSec.getString("Sign");
                                String[] signData = signLocStr.split(",");
                                Location signLoc = new Location(world, Integer.parseInt(signData[1]), Integer.parseInt(signData[2]), Integer.parseInt(signData[3]));
                                if (signLoc.getBlock().getState() instanceof Sign) {
                                    Block sign = signLoc.getBlock();
                                    NetworkItem netItem = new NetworkItem(net, chest, sign);
                                    net.withdrawChests.put(chest, netItem);
                                    allNetworkBlocks.put(chest, net);
                                    allNetworkBlocks.put(util.doubleChest(chest), net);
                                    allNetworkBlocks.put(sign, net);
                                } else {
                                    LOGGER.warning(pluginName + ": SortChest Sign Didnt exist at " + signLoc.getBlockX() + "," + signLoc.getBlockY() + "," + signLoc.getBlockZ() + ":" + signLoc.getWorld().getName());
                                }
                            } else {
                                LOGGER.warning(pluginName + ": Null world: " + wdChestData[0] + " . Does this world still exist?");
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warning(pluginName + ": did not find withdraw chest section in database");
                        LOGGER.warning(pluginName + ": it will be added next save update");
                    }

                    try {
                        ConfigurationSection dSign = network.getConfigurationSection("DropSigns");
                        for (String dSignLocStr : dSign.getKeys(false)) {
                            String[] dSignData = dSignLocStr.split(",");
                            World world = getServer().getWorld(dSignData[0].replace("(dot)", "."));
                            if (world != null) {
                                if (!net.world.equals(world.getName())) {
                                    net.world = world.getName();
                                }
                                Location dSignLoc = new Location(world, Integer.parseInt(dSignData[1]), Integer.parseInt(dSignData[2]), Integer.parseInt(dSignData[3]));
                                Block sign = dSignLoc.getBlock();
                                if (sign.getState() instanceof Sign) {
                                    NetworkItem netItem = new NetworkItem(net, null, sign);
                                    net.dropSigns.put(sign, netItem);
                                    allNetworkBlocks.put(sign, net);
                                } else {
                                    LOGGER.warning(pluginName + ": Drop Sign Didnt exist at " + dSignLoc.getBlockX() + "," + dSignLoc.getBlockY() + "," + dSignLoc.getBlockZ() + ":" + dSignLoc.getWorld().getName());
                                }
                            } else {
                                LOGGER.warning(pluginName + ": Null world: " + dSignData[0] + " . Does this world still exist?");
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warning(pluginName + ": did not find dropsigns section in database");
                        LOGGER.warning(pluginName + ": it will be added next save update");
                    }
                }
            }
            UUIDLoaded = true;
        }
    }

    public boolean cleanupNetwork() {
        List<Block> removeNetMapBlock = new ArrayList<>();
        List<SortNetwork> removedNets = new ArrayList<>();
        List<UUID> players = new ArrayList<>();
        boolean somethingCleaned = false;
        for (Entry<Block, SortNetwork> networkObject : allNetworkBlocks.entrySet()) {
            SortNetwork network = networkObject.getValue();
            List<Block> removeDepositChests = new ArrayList<>();
            List<Block> removeDropSigns = new ArrayList<>();
            List<Block> removeWithdrawChests = new ArrayList<>();
            List<SortChest> removedChests = new ArrayList<>();

            for (NetworkItem netItem : network.depositChests.values()) {
                boolean chestChunkLoaded = false;
                boolean signChunkLoaded = false;
                Block chest = netItem.chest.getLocation().getBlock();
                Block sign = netItem.sign.getLocation().getBlock();
                if (!chest.getChunk().isLoaded()) chestChunkLoaded = chest.getChunk().load();
                if (!sign.getChunk().isLoaded()) signChunkLoaded = sign.getChunk().load();
                if (!util.isValidDepositBlock(chest)) {
                    removeDepositChests.add(chest);
                    removeNetMapBlock.add(chest);
                    removeNetMapBlock.add(sign);
                    somethingCleaned = true;
                    LOGGER.info(pluginName + ": Chest at " + chest.getWorld().getName() + "," + chest.getLocation().getX() + "," + chest.getLocation().getY() + "," + chest.getLocation().getZ() + " in network " + netItem.network.netName + " removed (Not a chest block).");
                } else if (sign.getChunk().isLoaded() && !sign.getType().equals(Material.WALL_SIGN)) {
                    removeDepositChests.add(chest);
                    removeNetMapBlock.add(chest);
                    removeNetMapBlock.add(sign);
                    somethingCleaned = true;
                    LOGGER.info(pluginName + ": Chest at " + chest.getWorld().getName() + "," + chest.getLocation().getX() + "," + chest.getLocation().getY() + "," + chest.getLocation().getZ() + " in network " + netItem.network.netName + " removed (No deposit sign).");
                }
                if (chestChunkLoaded) chest.getChunk().unload();
                if (signChunkLoaded) sign.getChunk().unload();
            }

            for (Block chest : removeDepositChests)
                network.depositChests.remove(chest);

            for (NetworkItem netItem : network.dropSigns.values()) {
                Block sign = netItem.sign.getLocation().getBlock();
                boolean signChunkLoaded = false;
                if (!sign.getChunk().isLoaded()) signChunkLoaded = sign.getChunk().load();
                if (!sign.getType().equals(Material.SIGN_POST)) {
                    removeDropSigns.add(sign);
                    removeNetMapBlock.add(sign);
                    somethingCleaned = true;
                    LOGGER.info(pluginName + ": Sign at " + sign.getWorld().getName() + "," + sign.getLocation().getX() + "," + sign.getLocation().getY() + "," + sign.getLocation().getZ() + " in network " + netItem.network.netName + " removed (No drop sign).");
                }
                if (signChunkLoaded) sign.getChunk().unload();
            }

            for (Block sign : removeDropSigns)
                network.dropSigns.remove(sign);

            for (NetworkItem netItem : network.withdrawChests.values()) {
                boolean chestChunkLoaded = false;
                boolean signChunkLoaded = false;
                Block chest = netItem.chest.getLocation().getBlock();
                Block sign = netItem.sign.getLocation().getBlock();
                if (!chest.getChunk().isLoaded()) chestChunkLoaded = chest.getChunk().load();
                if (!sign.getChunk().isLoaded()) signChunkLoaded = sign.getChunk().load();
                if (!util.isValidWithdrawBlock(chest)) {
                    removeWithdrawChests.add(chest);
                    removeNetMapBlock.add(chest);
                    removeNetMapBlock.add(sign);
                    somethingCleaned = true;
                    LOGGER.info(pluginName + ": Chest at " + chest.getWorld().getName() + "," + chest.getLocation().getX() + "," + chest.getLocation().getY() + "," + chest.getLocation().getZ() + " in network " + netItem.network.netName + " removed (Not a chest block).");
                } else if (sign.getChunk().isLoaded() && !sign.getType().equals(Material.WALL_SIGN)) {
                    removeWithdrawChests.add(chest);
                    removeNetMapBlock.add(chest);
                    removeNetMapBlock.add(sign);
                    somethingCleaned = true;
                    LOGGER.info(pluginName + ": Chest at " + chest.getWorld().getName() + "," + chest.getLocation().getX() + "," + chest.getLocation().getY() + "," + chest.getLocation().getZ() + " in network " + netItem.network.netName + " removed (No Withdraw sign).");
                }
                if (chestChunkLoaded) chest.getChunk().unload();
                if (signChunkLoaded) sign.getChunk().unload();
            }

            for (Block chest : removeWithdrawChests)
                network.withdrawChests.remove(chest);

            for (SortChest sortChest : network.sortChests) {
                boolean chestChunkLoaded = false;
                boolean signChunkLoaded = false;
                Block chest = sortChest.block.getLocation().getBlock();
                Block sign = sortChest.sign.getLocation().getBlock();
                if (!chest.getChunk().isLoaded()) chestChunkLoaded = chest.getChunk().load();
                if (!sign.getChunk().isLoaded()) signChunkLoaded = sign.getChunk().load();
                if (util.isValidInventoryBlock(chest)) {
                    if (sign.getType().equals(Material.WALL_SIGN)) {
                        if (!((Sign) sign.getState()).getLine(0).startsWith("*")) {
                            removedChests.add(sortChest);
                            somethingCleaned = true;
                            LOGGER.info(pluginName + ": Chest at " + chest.getWorld().getName() + "," + chest.getX() + "," + chest.getY() + "," + chest.getZ() + " in network " + network.netName + " removed (No sort sign).");
                        }
                    } else {
                        removedChests.add(sortChest);
                        removeNetMapBlock.add(chest);
                        removeNetMapBlock.add(sign);
                        somethingCleaned = true;
                        LOGGER.info(pluginName + ": Chest at " + chest.getWorld().getName() + "," + chest.getX() + "," + chest.getY() + "," + chest.getZ() + " in network " + network.netName + " removed (No sort sign).");
                    }
                } else if (sortChest.block.getChunk().isLoaded() && !util.isValidInventoryBlock(chest)) {
                    removedChests.add(sortChest);
                    removeNetMapBlock.add(chest);
                    removeNetMapBlock.add(sign);
                    somethingCleaned = true;
                    LOGGER.info(pluginName + ": Chest at " + chest.getWorld().getName() + "," + chest.getX() + "," + chest.getY() + "," + chest.getZ() + " in network " + network.netName + " removed (Not a chest block).");
                }
                if (chestChunkLoaded) chest.getChunk().unload();
                if (signChunkLoaded) sign.getChunk().unload();
            }
            for (SortChest chest : removedChests) {
                network.sortChests.remove(chest);
            }
        }

        for (Entry<UUID, List<SortNetwork>> nets : networks.entrySet())
            for (SortNetwork network : nets.getValue())
                if (network.sortChests.size() == 0 && network.depositChests.size() == 0 && network.withdrawChests.size() == 0 && network.dropSigns.size() == 0) {
                    removedNets.add(network);
                    somethingCleaned = true;
                    LOGGER.info(pluginName + ": Network " + network.netName + " removed (Empty Network).");
                }

        for (SortNetwork netName : removedNets)
            networks.get(netName.owner).remove(netName);

        for (Block block : removeNetMapBlock)
            allNetworkBlocks.remove(block);

        for (Entry<UUID, List<SortNetwork>> nets : networks.entrySet())
            if (nets.getValue().size() == 0) players.add(nets.getKey());

        for (UUID player : players) {
            networks.remove(player);
            somethingCleaned = true;
            LOGGER.info(pluginName + ": Player UUID: " + player + " removed from database, No active networks.");
        }
        return somethingCleaned;
    }

    public void saveVersion6Network() {
        cleanupNetwork();

        getCustomConfig().set("version", SAVEVERSION);

        // Save Owners
        ConfigurationSection netsSec = getCustomConfig().createSection("Owners");
        for (Entry<UUID, List<SortNetwork>> nets : networks.entrySet()) {
            UUID key = nets.getKey();

            // Save Networks
            ConfigurationSection owners = netsSec.createSection(key.toString());
            ConfigurationSection newnet = owners.createSection("NetworkNames");
            for (SortNetwork net : nets.getValue()) {

                // Save Members
                ConfigurationSection netnames = newnet.createSection(net.netName);
                List<String> UUIDStrings = new ArrayList<>();
                for (UUID id : net.members) {
                    UUIDStrings.add(id.toString());
                }
                netnames.set("Members", UUIDStrings);

                // Save Sort Chests
                ConfigurationSection chestSec = netnames.createSection("Chests");
                for (SortChest chest : net.sortChests) {
                    Location loc = chest.block.getLocation();
                    String locStr = loc.getWorld().getName().replace(".", "(dot)") + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                    ConfigurationSection cSec = chestSec.createSection(locStr);
                    Location signLoc = chest.sign.getLocation();
                    String signLocStr = signLoc.getWorld().getName().replace(".", "(dot)") + "," + signLoc.getBlockX() + "," + signLoc.getBlockY() + "," + signLoc.getBlockZ();
                    cSec.set("Sign", signLocStr);
                    cSec.set("SignText", chest.signText);
                    cSec.set("Priority", chest.priority);
                    cSec.set("DisregardDamage", chest.disregardDamage);
                }

                // Save Deposit Chests
                ConfigurationSection depChestSec = netnames.createSection("DepositChests");
                for (Entry<Block, NetworkItem> depChest : net.depositChests.entrySet()) {
                    if (depChest.getValue().network.owner.equals(key) && depChest.getValue().network.netName.equals(net.netName)) {
                        Location depLoc = depChest.getKey().getLocation();
                        Location depSignLoc = depChest.getValue().sign.getLocation();
                        String locString = depLoc.getWorld().getName().replace(".", "(dot)") + "," + depLoc.getBlockX() + "," + depLoc.getBlockY() + "," + depLoc.getBlockZ();
                        String signLocString = depSignLoc.getWorld().getName().replace(".", "(dot)") + "," + depSignLoc.getBlockX() + "," + depSignLoc.getBlockY() + "," + depSignLoc.getBlockZ();
                        ConfigurationSection dSec = depChestSec.createSection(locString);
                        dSec.set("Sign", signLocString);
                    }
                }

                // Save WithdrawChests
                ConfigurationSection wdChestSec = netnames.createSection("WithdrawChests");
                for (Entry<Block, NetworkItem> wdChest : net.withdrawChests.entrySet()) {
                    if (wdChest.getValue().network.owner.equals(key) && wdChest.getValue().network.netName.equals(net.netName)) {
                        Location wdLoc = wdChest.getKey().getLocation();
                        Location wdSignLoc = wdChest.getValue().sign.getLocation();
                        String locString = wdLoc.getWorld().getName().replace(".", "(dot)") + "," + wdLoc.getBlockX() + "," + wdLoc.getBlockY() + "," + wdLoc.getBlockZ();
                        String signLocString = wdSignLoc.getWorld().getName().replace(".", "(dot)") + "," + wdSignLoc.getBlockX() + "," + wdSignLoc.getBlockY() + "," + wdSignLoc.getBlockZ();
                        ConfigurationSection wSec = wdChestSec.createSection(locString);
                        wSec.set("Sign", signLocString);
                    }
                }

                // Save Drop Signs
                ConfigurationSection dSignSec = netnames.createSection("DropSigns");
                for (Entry<Block, NetworkItem> dSign : net.dropSigns.entrySet()) {
                    if (dSign.getValue().network.owner.equals(key) && dSign.getValue().network.netName.equals(net.netName)) {
                        Location dSignLoc = dSign.getKey().getLocation();
                        String locString = dSignLoc.getWorld().getName().replace(".", "(dot)") + "," + dSignLoc.getBlockX() + "," + dSignLoc.getBlockY() + "," + dSignLoc.getBlockZ();
                        dSignSec.createSection(locString);
                    }
                }
            }
        }
        saveCustomConfig();
    }

    public void reloadCustomConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "networks.yml");
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);

        // Look for defaults in the jar
        InputStream defConfigStream = getResource("networks.yml");
        InputStreamReader isr = new InputStreamReader(defConfigStream);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(isr);
            customConfig.setDefaults(defConfig);
        }
    }

    public FileConfiguration getCustomConfig() {
        if (customConfig == null) {
            reloadCustomConfig();
        }
        return customConfig;
    }

    public void saveCustomConfig() {
        if (customConfig == null || customConfigFile == null) {
            return;
        }
        try {
            customConfig.save(customConfigFile);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, pluginName + ": Could not save config to " + customConfigFile, ex);
        }
    }

    // Find a network
    public SortNetwork findNetwork(UUID owner, String netName) {
        if (!networks.containsKey(owner)) return null;
        List<SortNetwork> netList = networks.get(owner);
        SortNetwork net = null;
        for (SortNetwork sn : netList) {
            if (sn.owner.equals(owner) && sn.netName.equals(netName)) {
                net = sn;
                break;
            }
        }
        return net;
    }
}
