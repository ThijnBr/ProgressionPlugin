package com.thefallersgames.progression.api.condition;

import org.bukkit.entity.Player;

/**
 * Represents a condition that must be met for progression.
 * This follows the Interface Segregation Principle by keeping interfaces small and focused.
 */
public interface ProgressCondition {
    
    /**
     * Check if the player meets this condition
     * 
     * @param player The player to check
     * @return true if condition is met, false otherwise
     */
    boolean isMet(Player player);
    
    /**
     * Get the current progress value for a player
     * 
     * @param player The player to check
     * @return The current progress value
     */
    int getCurrentProgress(Player player);
    
    /**
     * Get the required progress value to meet this condition
     * 
     * @return The required progress value
     */
    int getRequiredProgress();
    
    /**
     * Get a human-readable description of this condition
     * 
     * @return The condition description
     */
    String getDescription();
} 