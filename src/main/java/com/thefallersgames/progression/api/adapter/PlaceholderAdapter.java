package com.thefallersgames.progression.api.adapter;

import org.bukkit.OfflinePlayer;

/**
 * Adapter interface for PlaceholderAPI.
 * This follows the Adapter pattern to decouple the core logic from the external PlaceholderAPI.
 */
public interface PlaceholderAdapter {
    
    /**
     * Replace placeholders in a string with their values for a player
     * 
     * @param player The player to get values for
     * @param text The text with placeholders
     * @return Text with placeholders replaced by their values
     */
    String setPlaceholders(OfflinePlayer player, String text);
    
    /**
     * Replace placeholders in a string with their values without a player
     * 
     * @param text The text with placeholders
     * @return Text with placeholders replaced by their values
     */
    String setPlaceholders(String text);
    
    /**
     * Check if the adapter is available
     * 
     * @return true if PlaceholderAPI is available, false otherwise
     */
    boolean isAvailable();
} 