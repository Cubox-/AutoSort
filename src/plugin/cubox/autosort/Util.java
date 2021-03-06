package plugin.cubox.autosort;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import plugin.cubox.autosort.network.SortChest;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Map;
import java.util.UUID;

public class Util {

    private static AutoSort plugin;

    @SuppressWarnings("unused")
    public Util(AutoSort plugin) {
        Util.plugin = plugin;
    }

    public static String getName(UUID pId) {
        OfflinePlayer op = plugin.getServer().getOfflinePlayer(pId);
        if (op != null) return op.getName();
        return "unknown";
    }

    /**
     * Will find the exact player and is case sensitive
     *
     * @param name - The players name
     * @return Player object or null if not found
     */
    @SuppressWarnings("deprecation")
    public static Player getPlayer(String name) {
        for (Player test : Bukkit.matchPlayer(name)) {
            if (test.getName().equals(name)) return test;
        }
        return null;
    }

    public static Material parseMaterial(String str) {
        if (str != null) {
            if (str.equalsIgnoreCase("MISC")) {
                return Material.AIR;
            } else {
                Material mat = Material.matchMaterial(str);
                if (mat != null) {
                    return mat;
                } else {
                    AutoSort.LOGGER.warning("We tried to give Material.matchMaterial " + str + " but it returned null.");
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean isNumeric(String str) {
        if (str.equalsIgnoreCase("")) {
            return false;
        }
        if (str.contains(",")) {
            return false;
        }
        NumberFormat formatter = NumberFormat.getInstance();
        ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
    }

    public static Inventory getInventory(Block block) {
        if (!block.getChunk().isLoaded()) block.getChunk().load();
        if (!block.getChunk().isLoaded()) return null;
        BlockState state = block.getState();
        if (state instanceof InventoryHolder)
            return ((InventoryHolder) state).getInventory();
        else
            return null;
    }

    public boolean isValidInventoryBlock(Block block) {
        return isValidInventoryBlock(null, block, false);
    }

    public boolean isValidInventoryBlock(Player player, Block block, Boolean isEventCheck) {
        Material blockType = block.getType();
        if (plugin.sortBlocks.containsKey(blockType)) {
            return true;
        } else {
            if (isEventCheck) player.sendMessage(ChatColor.RED + "That's not a recognized inventory block!");
            return false;
        }
    }

    public boolean isValidDepositBlock(Block block) {
        return isValidDepositBlock(null, block, false);
    }

    public boolean isValidDepositBlock(Player player, Block block, Boolean isEventCheck) {
        Material blockType = block.getType();
        if (plugin.depositBlocks.containsKey(blockType)) {
            return true;
        } else {
            if (isEventCheck) player.sendMessage(ChatColor.RED + "That's not a recognized inventory block!");
            return false;
        }
    }

    public boolean isValidWithdrawBlock(Block block) {
        return isValidWithdrawBlock(null, block, false);
    }

    public boolean isValidWithdrawBlock(Player player, Block block, Boolean isEventCheck) {
        Material blockType = block.getType();
        if (plugin.withdrawBlocks.containsKey(blockType)) {
            return true;
        } else {
            if (isEventCheck) player.sendMessage(ChatColor.RED + "That's not a recognized inventory block!");
            return false;
        }
    }

    @SuppressWarnings("unused")
    public Block findSign(Block block) {
        BlockFace[] surchest = {BlockFace.SELF, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (BlockFace face : surchest) {
            Block sign = block.getRelative(face);
            if (sign.getType().equals(Material.WALL_SIGN)) {
                return sign;
            }
        }
        return null;
    }

    public Block doubleChest(Block block) {
        if (block.getType().equals(Material.CHEST) || block.getType().equals(Material.TRAPPED_CHEST)) {
            Chest chest = (Chest)block.getState();
            InventoryHolder chestHolder = chest.getInventory().getHolder();
            if (chestHolder instanceof DoubleChest) {
                DoubleChest doublechest = (DoubleChest) chestHolder;
                Chest left = (Chest)doublechest.getLeftSide();
                Chest right = (Chest)doublechest.getRightSide();

                if (right.equals(chest)) {
                    return left.getBlock();
                } else if (left.equals(chest)) {
                    return right.getBlock();
                } else {
                    AutoSort.LOGGER.severe("We failed to find the right DoubleChest for the chest. It's not possible.");
                }
            }
        }
        return block;
    }

    // Roll through the network and pull out the correct amount of resources.
    // If not enough space return a false
    // true is successful
    @SuppressWarnings({"UnusedReturnValue", "unused"})
    private boolean makeWithdraw(Player player, CustomPlayer settings) {
        int wantedAmount = settings.wantedAmount;
        ItemStack wantedItem = settings.inventory.get(settings.currentItemIdx).item;
        Map<Integer, ItemStack> couldntFit = null;
        Inventory networkInv;
        for (SortChest chest : settings.sortNetwork.sortChests) {
            if (!chest.block.getChunk().isLoaded())
                chest.block.getChunk().load();
            networkInv = getInventory(chest.block);
            if (networkInv == null) return false;
            for (int idx = 0; idx < networkInv.getSize(); idx++) {
                ItemStack networkItem = networkInv.getItem(idx);
                if (networkItem != null) {
                    if (networkItem.equals(wantedItem)) {
                        int foundAmount = networkItem.getAmount();
                        Inventory withdrawInv = settings.withdrawInventory;
                        if (wantedAmount >= foundAmount && foundAmount != 0) { // Found amount and was less then wanted
                            couldntFit = withdrawInv.addItem(networkItem);
                            if (couldntFit != null && !couldntFit.isEmpty()) {
                                return false;
                            }
                            wantedAmount -= foundAmount;
                            settings.wantedAmount = wantedAmount;
                            networkInv.clear(idx);
                        } else if (wantedAmount != 0 && wantedAmount < foundAmount) { // Found amount and was more then wanted
                            while (wantedAmount > 0) {
                                couldntFit = withdrawInv.addItem(networkItem);
                                if (couldntFit != null && !couldntFit.isEmpty()) {
                                    return false;
                                }
                                wantedAmount -= foundAmount;
                                settings.wantedAmount = wantedAmount;
                                networkInv.clear(idx);
                            }
                            if (couldntFit != null && !couldntFit.isEmpty()) {
                                return false;
                            }
                            wantedAmount -= foundAmount;
                            settings.wantedAmount = wantedAmount;
                        }
                    }
                }
            }
        }
        settings.wantedAmount = wantedAmount;
        return true;
    }

    public void updateChestInventory(Player player, CustomPlayer settings) {
        ItemStack dummyItem = new ItemStack(Material.POTION, 1);
        try {
            if (settings.block != null && !settings.block.getChunk().isLoaded())
                settings.block.getChunk().load();
            if (tooManyItems(player, settings))
                player.sendMessage(ChatColor.GOLD + settings.netName + ChatColor.RED + " is too full to replace withdrawchest Items!");
            settings.withdrawInventory.clear();
            settings.withdrawInventory.setItem(0, dummyItem);
            settings.withdrawInventory.setItem(8, dummyItem);

            for (settings.currentItemIdx = settings.startItemIdx; settings.currentItemIdx < settings.inventory.size(); settings.currentItemIdx++) {
                settings.wantedAmount = settings.inventory.get(settings.currentItemIdx).amount;
                makeWithdraw(player, settings);
            }
            if (settings.withdrawInventory.firstEmpty() != -1 && settings.startItemIdx != 0) {
                for (int count = 0; count < settings.startItemIdx; count++) {
                    settings.currentItemIdx = count;
                    settings.wantedAmount = settings.inventory.get(count).amount;
                    makeWithdraw(player, settings);
                }
            }
        } catch (Exception e) {
            ConsoleCommandSender sender = plugin.getServer().getConsoleSender();
            sender.sendMessage(ChatColor.RED + "AutoSort critical Withdraw Chest error!");
            sender.sendMessage(settings.block != null ? "Chest at " + settings.block.getLocation() : "Player at " + player.getLocation());
            sender.sendMessage("Player was " + player.getName());
            sender.sendMessage("Owner was " + settings.owner);
            sender.sendMessage("Network was " + settings.netName);
            sender.sendMessage("Error is as follows: ");
            sender.sendMessage(ChatColor.RED + "---------------------------------------");
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "---------------------------------------");
        } finally {
            settings.withdrawInventory.setItem(0, new ItemStack(Material.AIR));
            settings.withdrawInventory.setItem(8, new ItemStack(Material.AIR));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public boolean tooManyItems(Player player, CustomPlayer settings) {
        boolean tooManyItems = false;
        Inventory inv = settings.withdrawInventory;
        Location dropLoc = player.getLocation();
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) != null) {
                if (!settings.sortNetwork.quickSortItem(inv.getItem(i))) {
                    dropLoc.getWorld().dropItem(dropLoc, inv.getItem(i));
                    tooManyItems = true;
                }
            }
        }
        return tooManyItems;
    }

    public boolean updateInventoryList(@SuppressWarnings("unused") Player player, CustomPlayer settings) {
        for (SortChest chest : settings.sortNetwork.sortChests) {
            Inventory inv = Util.getInventory(chest.block);
            if (inv == null) continue;
            for (ItemStack item : inv) {
                if (item != null) {
                    int index = settings.findItem(item);
                    if (index != -1) {
                        settings.inventory.get(index).amount += item.getAmount();
                    } else {
                        settings.inventory.add(new InventoryItem(item, item.getAmount()));
                    }
                }
            }
        }
        return settings.inventory.size() > 0;
    }

    public void updateChestTask(final Player player, final CustomPlayer settings) {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (settings.withdrawInventory != null)
                plugin.util.updateChestInventory(player, settings);
        }, 3);
    }

    public void restoreWithdrawnInv(CustomPlayer settings, Player player) {
        if (settings.sortNetwork != null) {
            if (plugin.asListener.chestLock.containsKey(player.getName())) {
                plugin.asListener.chestLock.remove(player.getName());
                Inventory inv = settings.withdrawInventory;
                for (int i = 0; i < inv.getSize(); i++) {
                    if (inv.getItem(i) != null) {
                        settings.sortNetwork.sortItem(inv.getItem(i));
                    }
                }
                inv.clear();
                settings.clearPlayer();
            }
        }
    }
}
