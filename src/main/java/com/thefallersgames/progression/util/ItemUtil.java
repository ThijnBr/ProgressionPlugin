package com.thefallersgames.progression.util;

import org.bukkit.inventory.ItemStack;

/**
 * Utility class for item-related operations.
 */
public class ItemUtil {
    
    /**
     * Get a unique identifier for an item type
     * 
     * @param item The item to identify
     * @return A string identifier for the item type
     */
    public static String getItemId(ItemStack item) {
        if (item == null) {
            return "null";
        }
        
        return item.getType().name().toLowerCase();
    }
    
    /**
     * Check if an item matches another item's type
     * 
     * @param item The item to check
     * @param other The other item to compare against
     * @return true if the items are of the same type
     */
    public static boolean isSameType(ItemStack item, ItemStack other) {
        if (item == null || other == null) {
            return false;
        }
        
        return item.getType() == other.getType();
    }
} 