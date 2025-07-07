package com.thefallersgames.progression.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        
        // Check for custom item model (1.21.5+)
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasItemModel()) {
                NamespacedKey itemModel = meta.getItemModel();
                if (itemModel != null) {
                    return itemModel.getNamespace() + ":" + itemModel.getKey();
                }
            }
        }
        
        // Fall back to regular material name
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
        
        // First check for custom item models
        if (item.hasItemMeta() && other.hasItemMeta()) {
            ItemMeta itemMeta = item.getItemMeta();
            ItemMeta otherMeta = other.getItemMeta();
            
            if (itemMeta != null && otherMeta != null) {
                if (itemMeta.hasItemModel() && otherMeta.hasItemModel()) {
                    NamespacedKey itemModel = itemMeta.getItemModel();
                    NamespacedKey otherModel = otherMeta.getItemModel();
                    
                    if (itemModel != null && otherModel != null) {
                        return itemModel.equals(otherModel);
                    }
                }
            }
        }
        
        // Fall back to regular material type check
        return item.getType() == other.getType();
    }
} 