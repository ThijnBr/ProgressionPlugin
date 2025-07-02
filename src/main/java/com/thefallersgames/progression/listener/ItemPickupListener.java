package com.thefallersgames.progression.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import com.thefallersgames.progression.api.facade.UnlockFacade;

/**
 * Listener for item pickup events that checks if players can pick up locked items.
 * Uses the UnlockFacade to hide complexity of the progression system.
 */
@SuppressWarnings("deprecation") // Using deprecated API for compatibility
public class ItemPickupListener implements Listener {
    
    private final UnlockFacade unlockFacade;
    private final InventoryListener inventoryListener;
    private final Plugin plugin;
    
    // Track when messages were last sent to a player about specific locked items
    private final Map<UUID, Map<UUID, Long>> lastMessageTime = new HashMap<>();
    // Cooldown in milliseconds (5 seconds)
    private static final long MESSAGE_COOLDOWN = 5000;
    
    /**
     * Create a new ItemPickupListener
     * 
     * @param unlockFacade The facade for progression checking
     * @param inventoryListener The inventory listener for checking locked items
     * @param plugin The plugin instance for scheduling tasks
     */
    public ItemPickupListener(UnlockFacade unlockFacade, InventoryListener inventoryListener, Plugin plugin) {
        this.unlockFacade = unlockFacade;
        this.inventoryListener = inventoryListener;
        this.plugin = plugin;
    }
    
    /**
     * Handle player pickup of items
     * This prevents players from picking up items they haven't unlocked
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        Item itemEntity = event.getItem();
        ItemStack item = itemEntity.getItemStack();
        
        // Check if the item is tracked and the player can use it
        if (unlockFacade.isTrackedItem(item) && !unlockFacade.canUseItem(player, item)) {
            event.setCancelled(true);
            
            // Only send message if the player is close to the item AND we haven't sent a message recently
            if (event.getItem().getLocation().distanceSquared(player.getLocation()) < 4) {
                // Check if we should send a message (based on cooldown)
                UUID playerUuid = player.getUniqueId();
                UUID itemEntityUuid = itemEntity.getUniqueId();
                
                // Get or create the player's message map
                Map<UUID, Long> playerMessages = lastMessageTime.computeIfAbsent(playerUuid, k -> new HashMap<>());
                
                // Get the last time we sent a message about this item
                Long lastTime = playerMessages.get(itemEntityUuid);
                long currentTime = System.currentTimeMillis();
                
                // If we haven't sent a message recently, send one and update the time
                if (lastTime == null || (currentTime - lastTime) > MESSAGE_COOLDOWN) {
                    player.sendMessage(unlockFacade.getItemStatusMessage(player, item));
                    playerMessages.put(itemEntityUuid, currentTime);
                }
            }
        } else {
            // The item was picked up, schedule a task to check the player's inventory
            // for any other locked items (that might have just become locked)
            new BukkitRunnable() {
                @Override
                public void run() {
                    inventoryListener.dropLockedItems(player);
                }
            }.runTaskLater(plugin, 1L);
        }
    }
} 