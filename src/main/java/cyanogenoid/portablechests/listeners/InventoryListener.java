package cyanogenoid.portablechests.listeners;

import com.google.common.base.Strings;
import cyanogenoid.portablechests.PortableChests;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void on(InventoryPickupItemEvent e) {
        if (!(PortableChests.isContainer(e.getInventory()) || !PortableChests.isPortableContainer(e.getItem().getItemStack())))
            return;
        if (!PortableChests.canNestItemStack(e.getInventory(), e.getItem().getItemStack())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void on(InventoryMoveItemEvent e) {
        if (!(PortableChests.isContainer(e.getDestination()) || !PortableChests.isPortableContainer(e.getItem())))
            return;
        if (!PortableChests.canNestItemStack(e.getDestination(), e.getItem())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void on(InventoryClickEvent e) {
        ItemStack itemStack = this.findMovingItemStack(e);
        if (itemStack == null || !PortableChests.isPortableContainer(itemStack)) return;
        if (this.isMovingIntoBundle(e.getAction()) && !PortableChests.configs.ALLOW_BUNDLES) {
            String message = PortableChests.configs.BUNDLE_CANNOT_PLACE_MESSAGE;
            if (!Strings.isNullOrEmpty(message)) e.getWhoClicked().sendMessage(message);
            e.setCancelled(true);
            return;
        }

        if (!PortableChests.isContainer(e.getInventory())) return;
        if (this.isPlayerMovingItemStackToContainer(e) && !PortableChests.canNestItemStack(e.getInventory(), itemStack)) {
            e.setCancelled(true);
            if (!Strings.isNullOrEmpty(PortableChests.configs.NESTING_LIMIT_MESSAGE))
                e.getWhoClicked().sendMessage(PortableChests.configs.NESTING_LIMIT_MESSAGE);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void on(InventoryDragEvent e) {
        if (!PortableChests.isContainer(e.getInventory())) return;
        if (!PortableChests.isPortableContainer(e.getOldCursor())) return;
        if (e.getRawSlots().stream().anyMatch(slotID -> slotID <= e.getInventory().getSize() && !PortableChests.canNestItemStack(e.getInventory(), e.getOldCursor()))) {
            e.setCancelled(true);
            if (!Strings.isNullOrEmpty(PortableChests.configs.NESTING_LIMIT_MESSAGE))
                e.getWhoClicked().sendMessage(PortableChests.configs.NESTING_LIMIT_MESSAGE);
        }
    }


    private boolean isPlayerMovingItemStackToContainer(InventoryClickEvent e) {
        return e.getClickedInventory() != null && (e.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY) && e.getClickedInventory().getType().equals(InventoryType.PLAYER)) ||
                (e.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD) || e.getAction().equals(InventoryAction.HOTBAR_SWAP) && PortableChests.isContainer(e.getClickedInventory())) ||
                ((e.getAction().equals(InventoryAction.PLACE_ALL) || e.getAction().equals(InventoryAction.PLACE_ONE) || e.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) && PortableChests.isContainer(e.getClickedInventory()));
    }

    private ItemStack findMovingItemStack(InventoryClickEvent e) {
        if (e.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY) || e.getAction().name().equals("PICKUP_SOME_INTO_BUNDLE") || e.getAction().name().equals("PICKUP_ALL_INTO_BUNDLE"))
            return e.getCurrentItem();
        if (e.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD) || e.getAction().equals(InventoryAction.HOTBAR_SWAP))
            return e.getHotbarButton() > 0 ? e.getWhoClicked().getInventory().getItem(e.getHotbarButton()) : null;
        if (e.getAction().equals(InventoryAction.PLACE_ALL) || e.getAction().equals(InventoryAction.PLACE_ONE) || e.getAction().equals(InventoryAction.SWAP_WITH_CURSOR) || e.getAction().name().equals("PLACE_ALL_INTO_BUNDLE") || e.getAction().name().equals("PLACE_SOME_INTO_BUNDLE"))
            return e.getCursor();
        return null;
    }

    private boolean isMovingIntoBundle(InventoryAction a) {
        return a.name().contains("INTO_BUNDLE");
    }
}
