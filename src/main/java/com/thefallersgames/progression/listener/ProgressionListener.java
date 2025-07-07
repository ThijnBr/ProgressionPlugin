package com.thefallersgames.progression.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import com.thefallersgames.progression.api.service.ProgressService;

/**
 * Listener for progression-related events to track player progress.
 */
public class ProgressionListener implements Listener {
    
    private final ProgressService progressService;
    private final InventoryListener inventoryListener;
    private final Plugin plugin;
    
    /**
     * Create a new ProgressionListener
     * 
     * @param progressService The progress service
     * @param inventoryListener The inventory listener for checking locked items
     * @param plugin The plugin instance for scheduling tasks
     */
    public ProgressionListener(ProgressService progressService, InventoryListener inventoryListener, Plugin plugin) {
        this.progressService = progressService;
        this.inventoryListener = inventoryListener;
        this.plugin = plugin;
    }
    
    /**
     * Handle entity kill events
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        // Check if the killer is a player
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        Player player = event.getEntity().getKiller();
        String entityType = event.getEntityType().toString().toLowerCase(); // Use toString() for consistency
        
        // Record the kill
        progressService.recordProgress(player, "kills", entityType, 1);
        
        // After recording progress, check for locked items
        // This is important if killing a mob unlocked/locked an item
        new BukkitRunnable() {
            @Override
            public void run() {
                inventoryListener.dropLockedItems(player);
            }
        }.runTaskLater(plugin, 1L);
    }
    
    /**
     * Handle item collection events
     */
    @SuppressWarnings("deprecation") // Using deprecated API for compatibility
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        
        // Check for custom items with ItemModel (1.21.5+)
        String itemIdentifier;
        if (item.hasItemMeta() && item.getItemMeta().hasItemModel()) {
            // This is a custom item with a namespaced ID
            itemIdentifier = item.getItemMeta().getItemModel().toString();
            // Convert to the format namespace:key by removing 'minecraft:' prefix if present
            if (itemIdentifier.startsWith("minecraft:")) {
                itemIdentifier = itemIdentifier.substring(10);
            }
        } else {
            // Regular vanilla item
            itemIdentifier = item.getType().toString().toLowerCase();
        }
        
        // Record the collection
        progressService.recordProgress(player, "collect", itemIdentifier, item.getAmount());
        
        // After recording progress, check for locked items
        // This is important if collecting an item unlocked/locked another item
        new BukkitRunnable() {
            @Override
            public void run() {
                inventoryListener.dropLockedItems(player);
            }
        }.runTaskLater(plugin, 1L);
    }
    
    /**
     * Handle block break events
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().toString().toLowerCase(); // Use toString() for consistency
        
        // Record the break
        progressService.recordProgress(player, "break", blockType, 1);
        
        // After recording progress, check for locked items
        // This is important if breaking a block unlocked/locked an item
        new BukkitRunnable() {
            @Override
            public void run() {
                inventoryListener.dropLockedItems(player);
            }
        }.runTaskLater(plugin, 1L);
    }
} 