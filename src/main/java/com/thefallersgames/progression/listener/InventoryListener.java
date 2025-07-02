package com.thefallersgames.progression.listener;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import com.thefallersgames.progression.api.facade.UnlockFacade;

/**
 * Listener for inventory events that checks if players can use locked items.
 * Uses the UnlockFacade to hide complexity of the progression system.
 */
public class InventoryListener implements Listener {
    
    private final UnlockFacade unlockFacade;
    
    /**
     * Create a new InventoryListener
     * 
     * @param unlockFacade The facade for progression checking
     */
    public InventoryListener(UnlockFacade unlockFacade) {
        this.unlockFacade = unlockFacade;
    }
    
    /**
     * Handle inventory click events
     * This prevents players from clicking on locked items in inventories
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        if (item == null) {
            return;
        }
        
        // Check if the item is tracked and the player can use it
        if (unlockFacade.isTrackedItem(item) && !unlockFacade.canUseItem(player, item)) {
            event.setCancelled(true);
            player.sendMessage(unlockFacade.getItemStatusMessage(player, item));
            player.closeInventory();
        }
    }
    
    /**
     * Handle inventory drag events
     * This prevents players from dragging locked items in inventories
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getOldCursor();
        
        if (item == null) {
            return;
        }
        
        // Check if the item is tracked and the player can use it
        if (unlockFacade.isTrackedItem(item) && !unlockFacade.canUseItem(player, item)) {
            event.setCancelled(true);
            player.sendMessage(unlockFacade.getItemStatusMessage(player, item));
            player.closeInventory();
        }
    }
    
    /**
     * Check a player's inventory for locked items and drop them
     * 
     * @param player The player whose inventory should be checked
     */
    public void dropLockedItems(Player player) {
        // Keep track of types of items dropped to avoid spamming messages
        Set<String> droppedItemTypes = new HashSet<>();
        boolean anyDropped = false;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            
            if (item == null) {
                continue;
            }
            
            if (unlockFacade.isTrackedItem(item) && !unlockFacade.canUseItem(player, item)) {
                // Drop the item at the player's location
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                
                // Remove the item from the inventory
                player.getInventory().setItem(i, null);
                
                // Track that we dropped something of this type
                String itemKey = item.getType().toString();
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    itemKey += "-" + item.getItemMeta().getDisplayName();
                }
                droppedItemTypes.add(itemKey);
                anyDropped = true;
            }
        }
        
        // Send a single message if any items were dropped
        if (anyDropped) {
            player.sendMessage("§cSome items you can't use yet have been dropped at your feet.");
        }
    }
    
    /**
     * Handle player join events to check inventory for locked items
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        dropLockedItems(event.getPlayer());
    }
} 