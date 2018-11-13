package plugin.cubox.autosort;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import plugin.cubox.autosort.network.NetworkItem;
import plugin.cubox.autosort.network.SortChest;
import plugin.cubox.autosort.network.SortNetwork;

import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings({"ConstantConditions", "BooleanMethodIsAlwaysInverted", "UnusedReturnValue", "SameReturnValue"})
class CommandHandler {

    private final AutoSort plugin;
    private final BukkitScheduler scheduler;

    public CommandHandler(AutoSort plugin) {
        this.plugin = plugin;
        scheduler = plugin.getServer().getScheduler();
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public void inGame(CommandSender sender, Command cmd, @SuppressWarnings("unused") String commandLabel, String[] args) {
        String commandName = cmd.getName();
        Player player = (Player) sender;
        UUID ownerUUID = player.getUniqueId();
        if (commandName.equalsIgnoreCase("autosort")) {
            if (!plugin.hasPermission(player, "autosort.autosort")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("$Public")) args[0] = args[0].toUpperCase();
                SortNetwork network = plugin.findNetwork(ownerUUID, args[0]);
                if (network == null) {
                    sender.sendMessage(ChatColor.RED + "The network " + ChatColor.YELLOW + "'" + args[0] + "'" + ChatColor.RED + " could not be found.");
                    sender.sendMessage("Try " + ChatColor.YELLOW + " /autosort <ownerName> " + args[0]);
                    return;
                }
                sortPlayerInventory(9, sender, player.getName(), args[0], network);
            } else if (args.length == 2) {
                if (args[1].equalsIgnoreCase("$Public")) args[1] = args[1].toUpperCase();
                UUID uuid = getPlayerUUID(args[0], sender);
                if (uuid == null) return;
                SortNetwork network = plugin.findNetwork(uuid, args[1]);
                if (network == null) {
                    sender.sendMessage(ChatColor.RED + "The network " + ChatColor.YELLOW + "'" + args[1] + "'" + ChatColor.RED + " could not be found.");
                    return;
                }
                if ((network.owner.equals(ownerUUID) || network.members.contains(ownerUUID)) || plugin.hasPermission(player, "autosort.override")) {
                    sortPlayerInventory(9, sender, args[0], args[1], network);
                } else {
                    sender.sendMessage(ChatColor.RED + "Sorry you are not a member of the " + ChatColor.YELLOW + args[1] + ChatColor.WHITE + " network.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Incorrect command arguments");
                sender.sendMessage("Try " + ChatColor.YELLOW + " /autosort <networkName>");
            }
        } else if (commandName.equalsIgnoreCase("autosortall")) {
            if (!plugin.hasPermission(player, "autosort.autosort")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("$Public")) args[0] = args[0].toUpperCase();
                SortNetwork network = plugin.findNetwork(ownerUUID, args[0]);
                if (network == null) {
                    sender.sendMessage(ChatColor.RED + "The network " + ChatColor.YELLOW + "'" + args[0] + "'" + ChatColor.RED + " could not be found.");
                    sender.sendMessage("Try " + ChatColor.YELLOW + " /autosortall <ownerName> " + args[0]);
                    return;
                }
                sortPlayerInventory(0, sender, player.getName(), args[0], network);
            } else if (args.length == 2) {
                if (args[1].equalsIgnoreCase("$Public")) args[1] = args[1].toUpperCase();
                UUID uuid = getPlayerUUID(args[0], sender);
                if (uuid == null) return;
                SortNetwork network = plugin.findNetwork(uuid, args[1]);
                if (network == null) {
                    sender.sendMessage(ChatColor.RED + "The network " + ChatColor.YELLOW + "'" + args[1] + "'" + ChatColor.RED + " could not be found.");
                    return;
                }
                if ((network.owner.equals(player.getUniqueId()) || network.members.contains(player.getUniqueId())) || plugin.hasPermission(player, "autosort.override")) {
                    sortPlayerInventory(0, sender, args[0], args[1], network);
                } else {
                    sender.sendMessage(ChatColor.RED + "Sorry you are not a member of the " + ChatColor.YELLOW + args[1] + ChatColor.WHITE + " network.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Incorrect command arguments");
                sender.sendMessage("Try " + ChatColor.YELLOW + " /autosortall <networkName>");
            }
        } else if (commandName.equalsIgnoreCase("asreload")) {
            if (!plugin.hasPermission(player, "autosort.reload")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            reload(sender);
        } else if (commandName.equalsIgnoreCase("addasgroup")) {
            if (!plugin.hasPermission(player, "autosort.addasgroup")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            if (args.length > 1) {
                String groupName = args[0].toUpperCase();
                List<Material> matList = new ArrayList<>();
                List<String> ids = new ArrayList<>();
                for (int i = 1; i < args.length; i++) {
                    String mat = args[i];
                    if (Util.parseMaterial(mat) != null) {
                        matList.add(Util.parseMaterial(mat));
                        ids.add(mat);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Invalid Material: " + mat);
                    }
                }
                ConfigurationSection groupsSec = plugin.getConfig().getConfigurationSection("customGroups");
                groupsSec.set(groupName, ids);
                plugin.saveConfig();
                AutoSort.customMatGroups.put(groupName, matList);
                sender.sendMessage(ChatColor.GREEN + "AutoSort group added.");
            } else {
                sender.sendMessage(ChatColor.RED + "Incorrect command arguments");
                sender.sendMessage("Try " + ChatColor.YELLOW + " /addasgroup <groupName> <itemID>");
            }
        } else if (commandName.equalsIgnoreCase("modasgroup")) {
            if (!plugin.hasPermission(player, "autosort.modasgroup")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            if (args.length > 1) {
                String groupName = args[0].toUpperCase();
                if (AutoSort.customMatGroups.containsKey(groupName)) {
                    List<Material> matList = AutoSort.customMatGroups.get(groupName);
                    for (int i = 1; i < args.length; i++) {
                        String mat = args[i];
                        if (Util.parseMaterial(mat) != null) {
                            matList.add(Util.parseMaterial(mat));
                        } else {
                            if (args[i].startsWith("-")) {
                                Iterator<Material> itms = matList.iterator();
                                while (itms.hasNext()) {
                                    Material item = itms.next();
                                    String modArg = args[i].substring(1);
                                    Material parsedItem = Util.parseMaterial(modArg);
                                    if (parsedItem == null) continue;
                                    if (item == parsedItem)
                                        itms.remove();
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "Invalid Material: " + mat);
                            }
                        }
                    }
                    List<String> ids = new ArrayList<>();
                    for (Material is : matList) {
                        ids.add(is.name());
                    }
                    ConfigurationSection groupsSec = plugin.getConfig().getConfigurationSection("customGroups");
                    groupsSec.set(groupName, ids);
                    plugin.saveConfig();
                    AutoSort.customMatGroups.put(groupName, matList);
                    sender.sendMessage(ChatColor.GREEN + "AutoSort group modified.");
                } else {
                    sender.sendMessage(ChatColor.RED + "That group does not exist!");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Incorrect command arguments");
                sender.sendMessage("Try " + ChatColor.YELLOW + " /modasgroup <groupName>");
            }
        } else if (commandName.equalsIgnoreCase("delasgroup")) {
            if (!plugin.hasPermission(player, "autosort.delasgroup")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            if (args.length == 1) {
                String groupName = args[0].toUpperCase();
                if (AutoSort.customMatGroups.containsKey(groupName)) {
                    ConfigurationSection groupsSec = plugin.getConfig().getConfigurationSection("customGroups");
                    groupsSec.set(groupName, null);
                    plugin.saveConfig();
                    AutoSort.customMatGroups.remove(groupName);
                    sender.sendMessage(ChatColor.GREEN + "AutoSort group deleted.");
                } else {
                    sender.sendMessage(ChatColor.RED + "That group does not exist!");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Incorrect command arguments");
                sender.sendMessage("Try " + ChatColor.YELLOW + " /delasgroup <groupName>");
            }
        } else if (commandName.equalsIgnoreCase("ascleanup")) {
            if (!plugin.hasPermission(player, "autosort.ascleanup")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            sender.sendMessage(ChatColor.BLUE + "Cleaning up all AutoSort networks...");
            AutoSort.LOGGER.info("AutoSort: Command Cleanup Process Started.");
            if (!plugin.cleanupNetwork()) AutoSort.LOGGER.info("AutoSort: All networks are clean.");
            sender.sendMessage("Check server log for information on cleanup procedure.");
            AutoSort.LOGGER.info("AutoSort: Finished Command Cleanup Process.");
            sender.sendMessage(ChatColor.BLUE + "Done.");
        } else if (commandName.equalsIgnoreCase("addtonet")) {
            if (!plugin.hasPermission(player, "autosort.addtonet")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            if (args.length > 1) {
                String netName = args[0];
                if (netName.equalsIgnoreCase("$Public")) {
                    sender.sendMessage(ChatColor.YELLOW + "Public networks allow everyone already.");
                    return;
                }
                SortNetwork net = plugin.findNetwork(ownerUUID, netName);
                if (net != null) {
                    int count = 0;
                    for (int i = 1; i < args.length; i++) {
                        UUID memberId = getPlayerUUID(args[i], sender);
                        if (memberId == null) continue;
                        if (net.members.contains(memberId)) {
                            sender.sendMessage(ChatColor.YELLOW + args[i] + " already added to the network.");
                        } else {
                            net.members.add(memberId);
                            count++;
                        }
                    }
                    sender.sendMessage(count + " " + ChatColor.BLUE + "Player(s) successfully added to the network.");
                } else if (net == null && plugin.hasPermission(player, "autosort.override")) {
                    if (args.length > 2) {
                        netName = args[1];
                        UUID uuid = getPlayerUUID(args[0], sender);
                        if (uuid == null) return;
                        net = plugin.findNetwork(uuid, netName);
                        if (net != null) {
                            int count = 0;
                            for (int i = 2; i < args.length; i++) {
                                UUID memberId = getPlayerUUID(args[i], sender);
                                if (memberId == null) continue;
                                if (net.members.contains(memberId)) {
                                    sender.sendMessage(ChatColor.YELLOW + args[i] + " already added to the network.");
                                } else {
                                    net.members.add(memberId);
                                    count++;
                                }
                            }
                            sender.sendMessage(count + " " + ChatColor.BLUE + "Player(s) successfully added to the network.");
                        } else {
                            sender.sendMessage(ChatColor.RED + "The network '" + netName + "' could not be found.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /addtonet [ownerName] [netName] [players...]");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "The network '" + netName + "' could not be found.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /addtonet [netName] [players...]");
            }
        } else if (commandName.equalsIgnoreCase("remfromnet")) {
            if (!plugin.hasPermission(player, "autosort.remfromnet")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            if (args.length > 1) {
                String netName = args[0];
                if (netName.equalsIgnoreCase("$Public")) {
                    sender.sendMessage(ChatColor.YELLOW + "Public networks allow everyone.");
                    return;
                }
                SortNetwork net = plugin.findNetwork(ownerUUID, netName);
                if (net != null) {
                    int count = 0;
                    for (int i = 1; i < args.length; i++) {
                        if (!net.members.contains(args[i])) {
                            sender.sendMessage(ChatColor.YELLOW + args[i] + " is not a member of the network.");
                        } else {
                            net.members.remove(args[i]);
                            count++;
                        }
                    }
                    sender.sendMessage(count + " " + ChatColor.BLUE + "Player(s) successfully removed from the network.");
                } else if (net == null && plugin.hasPermission(player, "autosort.override")) {
                    if (args.length > 2) {
                        netName = args[1];
                        UUID ownerId = getPlayerUUID(args[0], sender);
                        if (ownerId == null) return;
                        net = plugin.findNetwork(ownerId, netName);
                        if (net != null) {
                            int count = 0;
                            for (int i = 2; i < args.length; i++) {
                                UUID memberId = getPlayerUUID(args[0], sender);
                                if (memberId == null) continue;
                                if (!net.members.contains(memberId)) {
                                    sender.sendMessage(ChatColor.YELLOW + args[i] + " is not a member of the network.");
                                } else {
                                    net.members.remove(memberId);
                                    count++;
                                }
                            }
                            sender.sendMessage(count + " " + ChatColor.BLUE + "Player(s) successfully removed from the network.");
                        } else {
                            sender.sendMessage(ChatColor.RED + "The network '" + netName + "' could not be found.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /remfromnet [ownerName] [netName] [players...]");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "The network '" + netName + "' could not be found.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /remfromnet [netName] [players...]");
            }
        } else if (commandName.equalsIgnoreCase("listasgroups")) {
            if (!plugin.hasPermission(player, "autosort.listasgroups")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            sender.sendMessage(ChatColor.GOLD + "Custom AutoSort material groups:");
            List<Material> items;
            StringBuilder list;
            int count = 0;
            for (String groupName : AutoSort.customMatGroups.keySet()) {
                list = new StringBuilder();
                items = AutoSort.customMatGroups.get(groupName);
                list.append(ChatColor.WHITE);
                list.append(groupName);
                list.append(ChatColor.GOLD);
                list.append(": ");
                for (Material item : items) {
                    list.append(getMaterialName(item));
                    list.append(ChatColor.WHITE);
                    list.append(count == items.size() - 1 ? "" : ", ");
                    list.append(ChatColor.GOLD);
                    count++;
                }
                count = 0;
                String msg = list.substring(0, list.length() - 2);
                sender.sendMessage(msg);
            }
        } else if (commandName.equalsIgnoreCase("listasmembers")) {
            if (!plugin.hasPermission(player, "autosort.listasmembers")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            boolean doList = false;
            SortNetwork network = null;
            if (args.length == 1) { // /listasmembers <owner> <netName>
                String owner = player.getName();
                String netName = args[0];
                if (netName.equalsIgnoreCase("$Public")) {
                    sender.sendMessage(ChatColor.YELLOW + "Public networks allow everyone.");
                    return;
                }
                network = plugin.findNetwork(ownerUUID, netName);
                if (network == null) {
                    sender.sendMessage(ChatColor.RED + "Could not find network " + ChatColor.RESET + args[0] + ChatColor.RED + " owned by " + ChatColor.RESET + owner);
                    sender.sendMessage("Try " + ChatColor.YELLOW + " /listasmembers <ownerName> " + args[0]);
                    return;
                }
                doList = true;
            } else if (args.length == 2) { // /listasmembers <ownerName> <netName>
                UUID ownerId = getPlayerUUID(args[0], sender);
                if (ownerId == null) return;
                String netName = args[1];
                if (netName.equalsIgnoreCase("$Public")) {
                    sender.sendMessage(ChatColor.YELLOW + "Public networks allow everyone.");
                    return;
                }
                network = plugin.findNetwork(ownerId, netName);
                if (network == null) {
                    sender.sendMessage(ChatColor.RED + "Could not find network " + ChatColor.RESET + args[1] + ChatColor.RED + " owned by " + ChatColor.RESET + args[0]);
                    return;
                }
                doList = true;
            }
            if (doList) {
                listMembers(sender, network);
            }
        } else if (commandName.equalsIgnoreCase("asremnet")) {
            if (!plugin.hasPermission(player, "autosort.remnet")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Incorrect command arguments");
                sender.sendMessage("Try " + ChatColor.YELLOW + " /asremnet <networkName>");
                return;
            }
            // /asremnet <OwnerName> <networkName>
            String ownerName = args[0];
            UUID ownerId = getPlayerUUID(args[0], sender);
            if (ownerId == null) return;
            String netName = args[1];
            if (!deleteNetwork(sender, ownerId, ownerName, netName, sender.getName())) return;
            sender.sendMessage(ChatColor.YELLOW + "The network ( " + ChatColor.WHITE + netName + ChatColor.YELLOW + " ) owned by ( " + ChatColor.WHITE + ownerName + ChatColor.YELLOW + " ) is deleted.");
            plugin.saveVersion6Network();
        } else if (commandName.equals("aswithdraw")) {
            if (!plugin.hasPermission(player, "autosort.use.withdrawcommand")) {
                sender.sendMessage(ChatColor.RED + "Sorry you do not have permission for " + ChatColor.YELLOW + commandName + ChatColor.RED + " command.");
                return;
            }
            if (args.length == 1) { // /aswithdraw <netName>
                String owner = player.getName();
                UUID ownerId = player.getUniqueId();
                String netName = args[0];
                if (netName.equalsIgnoreCase("$Public")) netName = netName.toUpperCase();
                SortNetwork network = plugin.findNetwork(ownerUUID, netName);
                if (network == null) {
                    sender.sendMessage(ChatColor.RED + "Could not find network " + ChatColor.RESET + args[0] + ChatColor.RED + " owned by " + ChatColor.RESET + owner);
                    sender.sendMessage("Try " + ChatColor.YELLOW + " /aswithdraw <ownerName> " + args[0]);
                    return;
                }
                doCommandWithdraw(player, network, ownerId, netName);
            } else if (args.length == 2) { // /aswithdraw <ownerName> <netName>
                UUID ownerId = getPlayerUUID(args[0], sender);
                if (ownerId == null) return;
                String netName = args[1];
                SortNetwork network = plugin.findNetwork(ownerId, netName);
                if (network == null) {
                    sender.sendMessage(ChatColor.RED + "Could not find network " + ChatColor.RESET + args[1] + ChatColor.RED + " owned by " + ChatColor.RESET + args[0]);
                    return;
                }
                if ((network.owner.equals(player.getUniqueId()) || network.members.contains(player.getUniqueId()) || network.netName.equalsIgnoreCase("$Public")) || plugin.hasPermission(player, "autosort.override")) {
                    doCommandWithdraw(player, network, ownerId, netName);
                } else {
                    sender.sendMessage(ChatColor.RED + "Sorry you are not a member of the " + ChatColor.YELLOW + args[1] + ChatColor.WHITE + " network.");
                }
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Incorrect command arguments");
            sender.sendMessage("Try " + ChatColor.YELLOW + " /aswithdraw <networkName>");
        }
    }

    private UUID getPlayerUUID(String name, CommandSender sender) {
        UUID uuid = FindUUID.getUUIDFromPlayerName(name);
        if (uuid == null) {
            sender.sendMessage(ChatColor.RED + "The player name " + ChatColor.YELLOW + "'" + name + "'" + ChatColor.RED + " could not be found.");
        }
        return uuid;
    }

    private void reload(final CommandSender sender) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                sender.sendMessage(ChatColor.AQUA + "AutoSort reloading...");
                CustomPlayer.playerSettings.clear();
                plugin.items.clear();
                plugin.stillItems.clear();
                plugin.allNetworkBlocks.clear();
                plugin.networks.clear();
                plugin.sortBlocks.clear();
                plugin.depositBlocks.clear();
                plugin.withdrawBlocks.clear();
                AutoSort.customMatGroups.clear();
                AutoSort.proximities.clear();
                sender.sendMessage(ChatColor.YELLOW + "AutoSort variables cleared.");

                plugin.loadConfig();
                sender.sendMessage(ChatColor.YELLOW + "AutoSort config reloaded.");
                plugin.loadCustomGroups();
                sender.sendMessage(ChatColor.YELLOW + "AutoSort custom groups reloaded.");
                plugin.loadInventoryBlocks();
                sender.sendMessage(ChatColor.YELLOW + "AutoSort inventory block list reloaded.");
                plugin.loadDatabase();
                sender.sendMessage(ChatColor.YELLOW + "AutoSort database reloaded.");
                sender.sendMessage(ChatColor.GREEN + "AutoSort reload finished successfully.");
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "AutoSort reload failed.");
            }
        }, 0);
    }

    public void inConsole(CommandSender sender, Command cmd, @SuppressWarnings("unused") String commandLabel, String[] args) {
        String commandName = cmd.getName();
        if (commandName.equalsIgnoreCase("asreload")) {
            if (args.length == 0) {
                reload(sender);
            }
        } else if (commandName.equalsIgnoreCase("addtonet")) {
            if (args.length > 2) {
                String netName = args[1];
                if (netName.equalsIgnoreCase("$Public")) {
                    sender.sendMessage(ChatColor.YELLOW + "Public networks allow everyone already.");
                    return;
                }
                UUID ownerId = getPlayerUUID(args[0], sender);
                if (ownerId == null) return;
                SortNetwork net = plugin.findNetwork(ownerId, netName);
                if (net != null) {
                    int count = 0;
                    for (int i = 2; i < args.length; i++) {
                        UUID memberId = getPlayerUUID(args[i], sender);
                        if (memberId == null) continue;
                        if (net.members.contains(memberId)) {
                            sender.sendMessage(ChatColor.YELLOW + args[i] + " already added to the network.");
                        } else {
                            net.members.add(memberId);
                            count++;
                        }
                    }
                    sender.sendMessage(count + " " + ChatColor.BLUE + "Player(s) successfully added to the network.");
                } else {
                    sender.sendMessage(ChatColor.RED + "The network '" + netName + "' could not be found.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /addtonet [ownerName] [netName] [players...]");
            }
        } else if (commandName.equalsIgnoreCase("remfromnet")) {
            if (args.length > 2) {
                String netName = args[1];
                if (netName.equalsIgnoreCase("$Public")) {
                    sender.sendMessage(ChatColor.YELLOW + "Public networks allow everyone.");
                    return;
                }
                UUID ownerId = getPlayerUUID(args[0], sender);
                if (ownerId == null) return;
                SortNetwork net = plugin.findNetwork(ownerId, netName);
                if (net != null) {
                    int count = 0;
                    for (int i = 2; i < args.length; i++) {
                        UUID memberId = getPlayerUUID(args[i], sender);
                        if (memberId == null) continue;
                        if (!net.members.contains(memberId)) {
                            sender.sendMessage(ChatColor.YELLOW + args[i] + " is not a member of the network.");
                        } else {
                            net.members.remove(memberId);
                            count++;
                        }
                    }
                    sender.sendMessage(count + " " + ChatColor.BLUE + "Player(s) successfully removed from the network.");
                } else {
                    sender.sendMessage(ChatColor.RED + "The network '" + netName + "' could not be found.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /remfromnet [ownerName] [netName] [players...]");
            }
        } else if (commandName.equalsIgnoreCase("addasgroup")) {
            if (args.length > 1) {
                String groupName = args[0].toUpperCase();
                List<Material> matList = new ArrayList<>();
                List<String> ids = new ArrayList<>();
                for (int i = 1; i < args.length; i++) {
                    String mat = args[i];
                    if (Util.parseMaterial(mat) != null) {
                        matList.add(Util.parseMaterial(mat));
                        ids.add(mat);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Invalid Material: " + mat);
                    }
                }
                ConfigurationSection groupsSec = plugin.getConfig().getConfigurationSection("customGroups");
                groupsSec.set(groupName, ids);
                plugin.saveConfig();
                AutoSort.customMatGroups.put(groupName, matList);
                sender.sendMessage(ChatColor.GREEN + "AutoSort group added.");
            }
        } else if (commandName.equalsIgnoreCase("modasgroup")) {
            if (args.length > 1) {
                String groupName = args[0].toUpperCase();
                if (AutoSort.customMatGroups.containsKey(groupName)) {
                    List<Material> matList = new ArrayList<>();
                    for (int i = 1; i < args.length; i++) {
                        String mat = args[i];
                        if (Util.parseMaterial(mat) != null) {
                            matList.add(Util.parseMaterial(mat));
                        } else {
                            sender.sendMessage(ChatColor.RED + "Invalid Material: " + mat);
                        }
                    }
                    List<String> ids = new ArrayList<>();
                    for (Material is : matList) {
                        ids.add(is.name());
                    }
                    ConfigurationSection groupsSec = plugin.getConfig().getConfigurationSection("customGroups");
                    groupsSec.set(groupName, ids);
                    plugin.saveConfig();
                    AutoSort.customMatGroups.put(groupName, matList);
                    sender.sendMessage(ChatColor.GREEN + "AutoSort group modified.");
                } else {
                    sender.sendMessage(ChatColor.RED + "That group does not exist!");
                }
            }
        } else if (commandName.equalsIgnoreCase("delasgroup")) {
            if (args.length == 1) {
                String groupName = args[0].toUpperCase();
                if (AutoSort.customMatGroups.containsKey(groupName)) {
                    ConfigurationSection groupsSec = plugin.getConfig().getConfigurationSection("customGroups");
                    groupsSec.set(groupName, null);
                    plugin.saveConfig();
                    AutoSort.customMatGroups.remove(groupName);
                    sender.sendMessage(ChatColor.GREEN + "AutoSort group deleted.");
                } else {
                    sender.sendMessage(ChatColor.RED + "That group does not exist!");
                }
            }
        } else if (commandName.equalsIgnoreCase("ascleanup")) {
            sender.sendMessage(ChatColor.BLUE + "Cleaning up all AutoSort networks...");
            if (!plugin.cleanupNetwork()) AutoSort.LOGGER.info("AutoSort: All networks are clean.");
            sender.sendMessage(ChatColor.BLUE + "Done.");
        } else if (commandName.equalsIgnoreCase("listasgroups")) {
            sender.sendMessage(ChatColor.GOLD + "Custom AutoSort material groups:");
            List<Material> items;
            StringBuilder list;
            int count = 0;
            for (String groupName : AutoSort.customMatGroups.keySet()) {
                list = new StringBuilder();
                items = AutoSort.customMatGroups.get(groupName);
                list.append(ChatColor.WHITE);
                list.append(groupName);
                list.append(ChatColor.GOLD);
                list.append(": ");
                for (Material item : items) {
                    list.append(getMaterialName(item));
                    list.append(ChatColor.WHITE);
                    list.append(count == items.size() - 1 ? "" : ", ");
                    list.append(ChatColor.GOLD);
                    count++;
                }
                count = 0;
                String msg = list.substring(0, list.length() - 2);
                sender.sendMessage(msg);
            }
        } else if (commandName.equalsIgnoreCase("listasmembers")) {
            boolean doList = false;
            SortNetwork network = null;
            if (args.length == 2) { // /listasmembers <ownerName> <netName>
                UUID uuid = getPlayerUUID(args[0], sender);
                if (uuid == null) return;
                String netName = args[1];
                if (netName.equalsIgnoreCase("$Public")) {
                    sender.sendMessage(ChatColor.YELLOW + "Public networks allow everyone.");
                    return;
                }
                network = plugin.findNetwork(uuid, netName);
                if (network == null) {
                    sender.sendMessage(ChatColor.RED + "Could not find network " + ChatColor.RESET + args[1] + ChatColor.RED + " owned by " + ChatColor.RESET + args[0]);
                    return;
                }
                doList = true;
            }
            if (doList) {
                listMembers(sender, network);
            }
        } else if (commandName.equalsIgnoreCase("asremnet")) {
            // /asremnet <OwnerName> <networkName>
            String ownerName = args[0];
            UUID uuid = getPlayerUUID(args[0], sender);
            if (uuid == null) return;
            String netName = args[1];
            if (!deleteNetwork(sender, uuid, ownerName, netName, sender.getName())) return;
            sender.sendMessage(ChatColor.YELLOW + "The network ( " + ChatColor.WHITE + netName + ChatColor.YELLOW + " ) owned by ( " + ChatColor.WHITE + ownerName + ChatColor.YELLOW + " ) is deleted.");
            plugin.saveVersion6Network();
        }
    }

    private boolean deleteNetwork(CommandSender player, UUID ownerUUID, String ownerName, String netName, String whoDeleted) {
        SortNetwork network = plugin.findNetwork(ownerUUID, netName);
        if (network == null) {
            player.sendMessage(ChatColor.RED + "The network ( " + ChatColor.WHITE + netName + ChatColor.RED + " ) owned by ( " + ChatColor.WHITE + ownerName + ChatColor.RED + " ) is not found.");
            return false;
        } else if (checkIfInUse(player, network)) return false;

        List<Block> netItemsToDel = new ArrayList<>();
        for (Entry<Block, NetworkItem> wchest : network.withdrawChests.entrySet()) {
            if (wchest.getValue().network.equals(network)) {
                plugin.allNetworkBlocks.remove(wchest.getValue().chest);
                plugin.allNetworkBlocks.remove(plugin.util.doubleChest(wchest.getValue().chest));
                plugin.allNetworkBlocks.remove(wchest.getValue().sign);
                updateSign(wchest.getValue().sign, netName, whoDeleted);
                netItemsToDel.add(wchest.getKey());
            }
        }
        for (Entry<Block, NetworkItem> dchest : network.depositChests.entrySet()) {
            if (dchest.getValue().network.equals(network)) {
                plugin.allNetworkBlocks.remove(dchest.getValue().chest);
                plugin.allNetworkBlocks.remove(plugin.util.doubleChest(dchest.getValue().chest));
                plugin.allNetworkBlocks.remove(dchest.getValue().sign);
                updateSign(dchest.getValue().sign, netName, whoDeleted);
                netItemsToDel.add(dchest.getKey());
            }
        }
        for (Entry<Block, NetworkItem> dsign : network.dropSigns.entrySet()) {
            if (dsign.getValue().network.equals(network)) {
                plugin.allNetworkBlocks.remove(dsign.getValue().sign);
                updateSign(dsign.getValue().sign, netName, whoDeleted);
                netItemsToDel.add(dsign.getKey());
            }
        }
        for (SortChest chest : network.sortChests) {
            plugin.allNetworkBlocks.remove(chest.block);
            plugin.allNetworkBlocks.remove(plugin.util.doubleChest(chest.block));
            plugin.allNetworkBlocks.remove(chest.sign);
            updateSign(chest.sign, netName, whoDeleted);
        }
        for (Block netBlock : netItemsToDel) {
            network.depositChests.remove(netBlock);
            network.depositChests.remove(plugin.util.doubleChest(netBlock));
            network.withdrawChests.remove(netBlock);
            network.withdrawChests.remove(plugin.util.doubleChest(netBlock));
            network.dropSigns.remove(netBlock);
        }
        plugin.networks.get(ownerUUID).remove(network);
        return true;
    }

    private void updateSign(final Block sign, final String netName, final String whoDeleted) {
        scheduler.runTask(plugin, () -> {
            if (sign.getType().equals(Material.WALL_SIGN)) {
                BlockState sgn = sign.getState();
                Sign s = (Sign) sign.getState();
                s.setLine(0, "[ " + netName + " ]");
                s.setLine(1, "deleted by");
                s.setLine(2, "" + whoDeleted);
                s.setLine(3, "");
                sgn.update(true);
                s.update(true);
            }
        });
    }

    private String getMaterialName(Material item) {
        if (item == null) {
            AutoSort.LOGGER.warning("We were passed a null ItemStack");
            return "null";
        }
        return item.name();
    }

    private void sortPlayerInventory(final int startIndex, final CommandSender sender, final String owner, final String netName, final SortNetwork net) {
        scheduler.runTask(plugin, () -> {
            Player player = (Player) sender;
            Inventory inv = player.getInventory();
            ItemStack[] contents = inv.getContents();
            ItemStack is;
            for (int i = startIndex; i < contents.length; i++) {
                is = contents[i];
                if (is != null) {
                    if (net.sortItem(is)) {
                        contents[i] = null;
                    }
                }
            }
            inv.setContents(contents);
            sender.sendMessage(ChatColor.GREEN + "Inventory sorted into " + ChatColor.YELLOW + netName + ChatColor.WHITE + " owned by " + ChatColor.YELLOW + owner);
        });
    }

    private boolean checkIfInUse(CommandSender player, SortNetwork network) {
        if (plugin.asListener.chestLock.containsValue(network)) {
            String user = "";
            for (Entry<String, SortNetwork> sortNet : plugin.asListener.chestLock.entrySet()) {
                if (sortNet.getValue().equals(network)) {
                    user = sortNet.getKey();
                    break;
                }
            }
            //Transaction Fail someone else is using the withdraw function
            player.sendMessage("The network " + ChatColor.YELLOW + network.netName + ChatColor.WHITE + " is being withdrawn from by " + ChatColor.YELLOW + user);
            player.sendMessage(ChatColor.GOLD + "Please wait...");
            return true;
        }
        return false;
    }

    private boolean doCommandWithdraw(final Player player, SortNetwork network, UUID owner, final String netName) {
        final CustomPlayer settings = CustomPlayer.getSettings(player);
        if (checkIfInUse(player, network)) return true;
        plugin.asListener.chestLock.put(player.getName(), network);
        settings.netName = netName;
        settings.owner = owner;
        settings.playerName = player.getName();
        settings.sortNetwork = network;
        settings.withdrawInventory = Bukkit.createInventory(null, 54, netName + " network inventory");
        scheduler.runTask(plugin, () -> {
            if (plugin.util.updateInventoryList(player, settings)) {
                settings.inventory.sort(new StringComparator());
                plugin.util.updateChestInventory(player, settings);
                player.openInventory(settings.withdrawInventory);
            } else {
                player.sendMessage("The network - " + ChatColor.YELLOW + netName + ChatColor.WHITE + " - is empty.");
                plugin.asListener.chestLock.remove(player.getName());
                settings.clearPlayer();
            }
        });
        return true;
    }

    private boolean listMembers(CommandSender sender, SortNetwork network) {
        if (network.members.size() == 0) {
            sender.sendMessage(ChatColor.RED + "There are no members of network " + ChatColor.RESET + network.netName + ChatColor.RED + " owned by " + ChatColor.RESET + network.owner);
            return true;
        }
        StringBuilder sb = new StringBuilder();
        String name, netName = network.netName;
        sender.sendMessage("Network: " + ChatColor.GOLD + netName);
        sb.append("Members: ");
        sb.append(ChatColor.AQUA);
        for (int i = 0; i < network.members.size(); i++) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(network.members.get(i));
            if (op == null) continue;
            name = op.getName() == null ? "Unknown: " + network.members.get(i).toString() : op.getName();
            if ((sb.length() + name.length()) > 80) {
                sender.sendMessage(sb.toString());
                sb = new StringBuilder();
                sb.append(ChatColor.AQUA);
            }
            if (i < network.members.size() - 1) {
                sb.append(name);
                sb.append(ChatColor.RESET);
                sb.append(", ");
                sb.append(ChatColor.AQUA);
            } else
                sb.append(name);
        }
        sender.sendMessage(sb.toString());
        return true;
    }
}