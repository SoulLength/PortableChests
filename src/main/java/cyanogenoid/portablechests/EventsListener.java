package cyanogenoid.portablechests;

import org.bukkit.block.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class EventsListener implements Listener {

    @EventHandler
    public void onBlockBreaks(BlockBreakEvent e) {
        Block block = e.getBlock();
        BlockState blockState = block.getState();
        if (!(PortableChests.isContainer(blockState))) return;

        if (!e.getPlayer().hasPermission(Permissions.canCreatePortableChest)) return;

        Inventory blockInventory;
        if (blockState instanceof Chest) blockInventory = ((Chest) blockState).getBlockInventory();
        else blockInventory = ((Container) blockState).getInventory();
        if (!e.isDropItems() || blockInventory.isEmpty()) return;

        ItemStack blockItemStack = block.getDrops(e.getPlayer().getInventory().getItemInMainHand()).stream()
                                        .filter(itemStack -> itemStack != null && itemStack.getType().equals(block.getType()))
                                        .findFirst()
                                        .orElse(null);
        if (blockItemStack == null) return;

        e.setDropItems(false);
        block.getWorld().dropItemNaturally(block.getLocation(), PortableChests.makePortableContainer(blockInventory, blockItemStack));
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent e) {
        Block block = e.getBlock();
        BlockState blockState = block.getState();
        if (!(PortableChests.isContainer(blockState))) return;
        if (!PortableChests.isPortableContainer(e.getItemInHand())) return;

        Inventory blockInventory = ((Container) blockState).getInventory();

        try {
            PortableChests.fillPortableContainer(blockInventory, e.getItemInHand());
        } catch (InvalidConfigurationException invalidConfigurationException) {
            e.getPlayer().sendMessage("Invalid chest content!");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent e) {
        if (!(PortableChests.isContainer(e.getInventory()) || !PortableChests.isPortableContainer(e.getItem().getItemStack()))) return;
        if (!PortableChests.canNestItemStack(e.getItem().getItemStack())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        if (!(PortableChests.isContainer(e.getDestination()) || !PortableChests.isPortableContainer(e.getItem()))) return;
        if (!PortableChests.canNestItemStack(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!PortableChests.isContainer(e.getInventory())) return;
        ItemStack itemStack = this.findMovingItemStack(e);
        if (!PortableChests.isPortableContainer(itemStack)) return;
        if (this.isPlayerMovingItemStackToContainer(e) && !PortableChests.canNestItemStack(itemStack, e.getWhoClicked())) e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!PortableChests.isContainer(e.getInventory())) return;
        if (!PortableChests.isPortableContainer(e.getOldCursor())) return;
        if (e.getRawSlots().stream().anyMatch(slotID -> slotID <= e.getInventory().getSize() && !PortableChests.canNestItemStack(e.getOldCursor(), e.getWhoClicked()))) e.setCancelled(true);
    }


    private boolean isPlayerMovingItemStackToContainer(InventoryClickEvent e) {
        return e.getClickedInventory() != null && (e.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY) && e.getClickedInventory().getType().equals(InventoryType.PLAYER)) ||
                ((e.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD) || e.getAction().equals(InventoryAction.HOTBAR_SWAP))  && PortableChests.isContainer(e.getClickedInventory())) ||
                ((e.getAction().equals(InventoryAction.PLACE_ALL) || e.getAction().equals(InventoryAction.PLACE_ONE) || e.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) && PortableChests.isContainer(e.getClickedInventory()));
    }

    private ItemStack findMovingItemStack(InventoryClickEvent e) {
        if (e.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) return e.getCurrentItem();
        if (e.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD) || e.getAction().equals(InventoryAction.HOTBAR_SWAP)) return e.getWhoClicked().getInventory().getItem(e.getHotbarButton());
        if (e.getAction().equals(InventoryAction.PLACE_ALL) || e.getAction().equals(InventoryAction.PLACE_ONE) || e.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) return e.getCursor();
        return null;
    }
}
