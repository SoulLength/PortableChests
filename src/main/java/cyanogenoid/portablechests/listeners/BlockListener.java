package cyanogenoid.portablechests.listeners;

import cyanogenoid.portablechests.Permissions;
import cyanogenoid.portablechests.PortableChests;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class BlockListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on(BlockBreakEvent e) {
        if (!e.isDropItems()) return;

        Block block = e.getBlock();
        BlockState blockState = block.getState();

        if (!PortableChests.isContainer(blockState) || PortableChests.shouldIgnoreCustomNamed(blockState)) return;
        if (!PortableChests.canCreateInWorld(block.getWorld()) && !e.getPlayer().hasPermission(Permissions.canCreatePortableContainersAnywhere)) return;

        Inventory blockInventory;
        if (blockState instanceof Chest) blockInventory = ((Chest) blockState).getBlockInventory();
        else blockInventory = ((Container) blockState).getInventory();
        if (Arrays.stream(blockInventory.getContents()).allMatch(Objects::isNull)) return;

        ItemStack handledItem = e.getPlayer().getInventory().getItemInMainHand();
        ItemStack blockItemStack = block.getDrops(handledItem).stream()
                                        .filter(itemStack -> itemStack != null && itemStack.getType().equals(block.getType()))
                                        .findFirst()
                                        .orElse(null);
        if (blockItemStack == null) return;
        if (!e.getPlayer().hasPermission(Permissions.canSkipEnchantment) && !PortableChests.hasRequiredEnchantment(handledItem)) return;

        if (blockState instanceof ShulkerBox) {
            e.setCancelled(true);
            e.getBlock().setType(Material.AIR);
            PortableChests.setShulkerBoxNestingData(blockInventory, blockItemStack);
            block.getWorld().dropItemNaturally(block.getLocation(), blockItemStack);
        } else if (e.getPlayer().hasPermission(Permissions.canCreatePortableContainers)) {
            e.setDropItems(false);
            block.getWorld().dropItemNaturally(block.getLocation(), PortableChests.makePortableContainer(blockInventory, blockItemStack));
            blockInventory.clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on(BlockPlaceEvent e) {
        Block block = e.getBlock();
        BlockState blockState = block.getState();

        if (!(PortableChests.isContainer(blockState)) || blockState instanceof ShulkerBox) return;
        if (!PortableChests.isPortableContainer(e.getItemInHand())) return;

        if (!PortableChests.canPlaceInWorld(block.getWorld()) && !e.getPlayer().hasPermission(Permissions.canPlacePortableContainersAnywhere)) {
            e.setCancelled(true);
            if (!PortableChests.CANNOT_PLACE_MESSAGE.isEmpty()) e.getPlayer().sendMessage(PortableChests.CANNOT_PLACE_MESSAGE);
            return;
        }

        PortableChests.removeBlockDisplayNameMark(blockState);

        Inventory blockInventory = ((Container) e.getBlock().getState()).getInventory();
        try {
            PortableChests.fillPortableContainer(blockInventory, e.getItemInHand());
        } catch (InvalidConfigurationException invalidConfigurationException) {
            e.getPlayer().sendMessage("Invalid chest content!");
            e.setCancelled(true);
        }
    }
}
