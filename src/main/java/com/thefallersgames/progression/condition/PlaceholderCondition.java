package com.thefallersgames.progression.condition;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.thefallersgames.progression.api.condition.ProgressCondition;
import com.thefallersgames.progression.api.adapter.PlaceholderAdapter;

/**
 * Condition implementation that uses PlaceholderAPI values.
 * This is an example of the Adapter pattern, using PlaceholderAdapter to abstract the external API.
 */
public class PlaceholderCondition implements ProgressCondition {
    
    private final PlaceholderAdapter placeholderAdapter;
    private final String placeholder;
    private final double requiredAmount;
    
    /**
     * Create a new PlaceholderCondition from configuration
     * 
     * @param config The configuration section
     * @param placeholderAdapter The adapter for PlaceholderAPI
     */
    public PlaceholderCondition(ConfigurationSection config, PlaceholderAdapter placeholderAdapter) {
        this.placeholderAdapter = placeholderAdapter;
        
        String placeholderName = config.getString("placeholder");
        if (placeholderName == null) {
            throw new IllegalArgumentException("Placeholder name must be specified");
        }
        
        // Ensure the placeholder has the % prefix and suffix if needed
        if (!placeholderName.startsWith("%")) {
            placeholderName = "%" + placeholderName;
        }
        if (!placeholderName.endsWith("%")) {
            placeholderName = placeholderName + "%";
        }
        
        this.placeholder = placeholderName;
        this.requiredAmount = config.getDouble("amount", 1000);
        
        // Check if PlaceholderAPI is available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            throw new IllegalStateException("PlaceholderAPI is required for placeholder conditions");
        }
    }
    
    @Override
    public boolean isMet(Player player) {
        double currentValue = getCurrentProgressDouble(player);
        return currentValue >= requiredAmount;
    }
    
    @Override
    public int getCurrentProgress(Player player) {
        return (int) getCurrentProgressDouble(player);
    }
    
    /**
     * Get the current progress value as a double
     * 
     * @param player The player to check
     * @return The current progress value
     */
    public double getCurrentProgressDouble(Player player) {
        String value = placeholderAdapter.setPlaceholders(player, placeholder);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    @Override
    public int getRequiredProgress() {
        return (int) requiredAmount;
    }
    
    @Override
    public String getDescription() {
        return "Reach " + requiredAmount + " " + placeholder;
    }
    
    /**
     * Get the placeholder string for this condition
     * 
     * @return The placeholder string
     */
    public String getPlaceholder() {
        return this.placeholder;
    }
} 