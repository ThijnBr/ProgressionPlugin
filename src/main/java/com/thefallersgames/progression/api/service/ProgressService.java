package com.thefallersgames.progression.api.service;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.thefallersgames.progression.api.condition.ProgressCondition;

import java.util.Map;

/**
 * Core service interface for progression functionality.
 * Part of the Hexagonal Architecture pattern - this is a port in the core domain.
 */
public interface ProgressService {
    
    /**
     * Check if an item is locked for a player
     * 
     * @param player The player to check
     * @param item The item to check
     * @return true if the item is locked, false if it's unlocked
     */
    boolean isItemLocked(Player player, ItemStack item);
    
    /**
     * Get the condition associated with an item
     * 
     * @param item The item to check
     * @return The condition or null if item is not locked
     */
    ProgressCondition getItemCondition(ItemStack item);
    
    /**
     * Get the lock message for a locked item
     * 
     * @param player The player
     * @param item The locked item
     * @return The formatted message to show the player
     */
    String getItemLockMessage(Player player, ItemStack item);
    
    /**
     * Record progress for a player towards a specific condition type
     * 
     * @param player The player making progress
     * @param conditionType The type of condition (e.g., "kills", "collect")
     * @param key Additional identifier (e.g., entity type, material)
     * @param amount The amount of progress to add
     */
    void recordProgress(Player player, String conditionType, String key, int amount);
    
    /**
     * Check if a player meets a specific condition
     * 
     * @param player The player to check
     * @param condition The condition to evaluate
     * @return true if the condition is met, false otherwise
     */
    boolean meetsCondition(Player player, ProgressCondition condition);
    
    /**
     * Get all registered item conditions
     * 
     * @return Map of item IDs to their associated conditions
     */
    Map<String, ProgressCondition> getAllConditions();
} 