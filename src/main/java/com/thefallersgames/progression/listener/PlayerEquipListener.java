package com.thefallersgames.progression.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import com.thefallersgames.progression.api.facade.UnlockFacade;

/**
 * Listener for equipment events that checks if players can equip or use locked items.
 * Uses the UnlockFacade to hide complexity of the progression system.
 */
public class PlayerEquipListener implements Listener {
    
    private final UnlockFacade unlockFacade;
    private final InventoryListener inventoryListener;
    private final Plugin plugin;
    
    /**
     * Create a new PlayerEquipListener
     * 
     * @param unlockFacade The facade for progression checking
     * @param inventoryListener The inventory listener for checking locked items
     * @param plugin The plugin instance for scheduling tasks
     */
    public PlayerEquipListener(UnlockFacade unlockFacade, InventoryListener inventoryListener, Plugin plugin) {
        this.unlockFacade = unlockFacade;
        this.inventoryListener = inventoryListener;
        this.plugin = plugin;
    }
    
    /**
     * Handle item held events
     * This prevents players from switching to locked items in their hotbar
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        if (item == null) {
            return;
        }
        
        // Check if the item is tracked and the player can use it
        if (unlockFacade.isTrackedItem(item) && !unlockFacade.canUseItem(player, item)) {
            // Instead of just cancelling, schedule a task to drop the item
            new BukkitRunnable() {
                @Override
                public void run() {
                    inventoryListener.dropLockedItems(player);
                }
            }.runTaskLater(plugin, 1L);
            
            event.setCancelled(true);
            player.sendMessage(unlockFacade.getItemStatusMessage(player, item));
        }
    }
    
    /**
     * Handle block place events
     * This prevents players from placing blocks that are locked
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        // Check if the item is tracked and the player can use it
        if (unlockFacade.isTrackedItem(item) && !unlockFacade.canUseItem(player, item)) {
            // Instead of just cancelling, schedule a task to drop the item
            new BukkitRunnable() {
                @Override
                public void run() {
                    inventoryListener.dropLockedItems(player);
                }
            }.runTaskLater(plugin, 1L);
            
            event.setCancelled(true);
            player.sendMessage(unlockFacade.getItemStatusMessage(player, item));
        }
    }
    
    /**
     * Handle entity pickup item events (modern alternative to PlayerPickupItemEvent)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        
        // Check if the item is tracked and the player can use it
        if (unlockFacade.isTrackedItem(item) && !unlockFacade.canUseItem(player, item)) {
            event.setCancelled(true);
            
            // Only send message if the player is close to the item
            // This prevents spam if a player stands near an item they can't pick up
            if (event.getItem().getLocation().distanceSquared(player.getLocation()) < 4) {
                player.sendMessage(unlockFacade.getItemStatusMessage(player, item));
            }
        }
    }
    
    /**
     * Handle player respawn to check inventory for locked items
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Check inventory after respawn to drop any locked items
        final Player player = event.getPlayer();
        
        // Schedule a task to run after the respawn process is complete
        new BukkitRunnable() {
            @Override
            public void run() {
                inventoryListener.dropLockedItems(player);
            }
        }.runTaskLater(plugin, 1L);
    }
} 