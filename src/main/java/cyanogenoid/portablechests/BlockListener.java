package cyanogenoid.portablechests;

import org.bukkit.block.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;


public class BlockListener implements Listener {

    @EventHandler
    public void on(BlockBreakEvent e) {
        if (!e.isDropItems()) return;

        Block block = e.getBlock();
        BlockState blockState = block.getState();
        if (!(PortableChests.isContainer(blockState))) return;

        Inventory blockInventory;
        if (blockState instanceof Chest) blockInventory = ((Chest) blockState).getBlockInventory();
        else blockInventory = ((Container) blockState).getInventory();
        if (Arrays.stream(blockInventory.getContents()).allMatch(Objects::isNull)) return;



        ItemStack blockItemStack = block.getDrops(e.getPlayer().getInventory().getItemInMainHand()).stream()
                                        .filter(itemStack -> itemStack != null && itemStack.getType().equals(block.getType()))
                                        .findFirst()
                                        .orElse(null);
        if (blockItemStack == null) return;

        if (blockState instanceof ShulkerBox) {
            e.setDropItems(false);
            PortableChests.setShulkerBoxNestingData(blockInventory, blockItemStack);
            block.getWorld().dropItemNaturally(block.getLocation(), blockItemStack);
        }
        else if (e.getPlayer().hasPermission(Permissions.canCreatePortableChest)) {
            e.setDropItems(false);
            block.getWorld().dropItemNaturally(block.getLocation(), PortableChests.makePortableContainer(blockInventory, blockItemStack));
            blockInventory.clear();
        }
    }

    @EventHandler
    public void on(BlockPlaceEvent e) {
        Block block = e.getBlock();
        BlockState blockState = block.getState();
        if (!(PortableChests.isContainer(blockState)) || blockState instanceof ShulkerBox) return;
        if (!PortableChests.isPortableContainer(e.getItemInHand())) return;

        Inventory blockInventory = ((Container) blockState).getInventory();

        try {
            PortableChests.fillPortableContainer(blockInventory, e.getItemInHand());
        } catch (InvalidConfigurationException invalidConfigurationException) {
            e.getPlayer().sendMessage("Invalid chest content!");
            e.setCancelled(true);
        }
    }
}
