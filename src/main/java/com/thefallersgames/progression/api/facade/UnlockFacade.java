package com.thefallersgames.progression.api.facade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Facade interface that simplifies progression operations for other code.
 * This follows the Facade pattern by providing a simplified interface to the underlying subsystems.
 */
public interface UnlockFacade {
    
    /**
     * Check if a player can use an item
     * 
     * @param player The player to check
     * @param item The item to check
     * @return true if the player can use the item, false otherwise
     */
    boolean canUseItem(Player player, ItemStack item);
    
    /**
     * Handle when a player tries to use an item that might be locked
     * 
     * @param player The player using the item
     * @param item The item being used
     * @return true if the action should be allowed, false if it should be blocked
     */
    boolean handleItemUse(Player player, ItemStack item);
    
    /**
     * Check if an item is tracked by the progression system
     * 
     * @param item The item to check
     * @return true if the item is tracked, false otherwise
     */
    boolean isTrackedItem(ItemStack item);
    
    /**
     * Get a formatted message about the item's unlock status
     * 
     * @param player The player requesting info
     * @param item The item to check
     * @return A formatted status message
     */
    String getItemStatusMessage(Player player, ItemStack item);
} 