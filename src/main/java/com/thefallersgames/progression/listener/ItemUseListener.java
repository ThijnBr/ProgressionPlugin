package com.thefallersgames.progression.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import com.thefallersgames.progression.api.facade.UnlockFacade;

/**
 * Listener for item use events that checks if players can use locked items.
 * Uses the UnlockFacade to hide complexity of the progression system.
 */
public class ItemUseListener implements Listener {
    
    private final UnlockFacade unlockFacade;
    private final InventoryListener inventoryListener;
    private final Plugin plugin;
    
    /**
     * Create a new ItemUseListener
     * 
     * @param unlockFacade The facade for progression checking
     * @param inventoryListener The inventory listener for checking locked items
     * @param plugin The plugin instance for scheduling tasks
     */
    public ItemUseListener(UnlockFacade unlockFacade, InventoryListener inventoryListener, Plugin plugin) {
        this.unlockFacade = unlockFacade;
        this.inventoryListener = inventoryListener;
        this.plugin = plugin;
    }
    
    /**
     * Handle player interaction with items
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Skip if there's no item or the action isn't using the item
        if (item == null || (event.getAction() != Action.RIGHT_CLICK_AIR 
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        
        // Check if the player can use the item, cancel if not
        if (!unlockFacade.handleItemUse(player, item)) {
            // Schedule a task to drop locked items from inventory
            new BukkitRunnable() {
                @Override
                public void run() {
                    inventoryListener.dropLockedItems(player);
                }
            }.runTaskLater(plugin, 1L);
            
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle player consumption of food items like golden apples
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if the player can use the item, cancel if not
        if (!unlockFacade.handleItemUse(player, item)) {
            // Schedule a task to drop locked items from inventory
            new BukkitRunnable() {
                @Override
                public void run() {
                    inventoryListener.dropLockedItems(player);
                }
            }.runTaskLater(plugin, 1L);
            
            event.setCancelled(true);
        }
    }
} 