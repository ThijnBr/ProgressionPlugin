package com.thefallersgames.progression.condition;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.thefallersgames.progression.api.condition.ProgressCondition;
import com.thefallersgames.progression.data.PlayerDataManager;

/**
 * Condition implementation for collecting specific item types.
 */
public class CollectCondition implements ProgressCondition {
    
    private final PlayerDataManager dataManager;
    private Material materialType;
    private final int requiredAmount;
    private final String materialName; // Store the actual material name used for lookups
    private final boolean isCustomItem; // Flag indicating if this is a custom namespaced item
    
    /**
     * Create a new CollectCondition from configuration
     * 
     * @param config The configuration section
     * @param dataManager The player data manager
     */
    public CollectCondition(ConfigurationSection config, PlayerDataManager dataManager) {
        this.dataManager = dataManager;
        
        String materialName = config.getString("material", "APPLE");
        
        // Check if this is a custom namespaced item (contains ':')
        if (materialName.contains(":")) {
            // This is a custom item with a namespaced ID
            this.isCustomItem = true;
            this.materialName = materialName.toLowerCase(); // Store as-is for lookups
            this.materialType = null; // No direct Material type for custom items
        } else {
            // This is a regular Bukkit Material
            this.isCustomItem = false;
            
            try {
                this.materialType = Material.valueOf(materialName.toUpperCase());
                // Store the normalized name that will be used for progress tracking
                this.materialName = this.materialType.toString().toLowerCase();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid material type: " + materialName);
            }
        }
        
        this.requiredAmount = config.getInt("amount", 50);
    }
    
    @Override
    public boolean isMet(Player player) {
        return getCurrentProgress(player) >= requiredAmount;
    }
    
    @Override
    public int getCurrentProgress(Player player) {
        // Use the consistent material name for lookups
        return dataManager.getProgress(player.getUniqueId(), "collect", materialName);
    }
    
    @Override
    public int getRequiredProgress() {
        return requiredAmount;
    }
    
    @Override
    public String getDescription() {
        return "Collect " + requiredAmount + " " + materialName;
    }
    
    /**
     * Get the material type for this condition
     * 
     * @return The material type, or null if this is a custom item
     */
    public Material getMaterialType() {
        return materialType;
    }
    
    /**
     * Check if this condition is for a custom namespaced item
     * 
     * @return true if this is a custom item, false if it's a vanilla material
     */
    public boolean isCustomItem() {
        return isCustomItem;
    }
    
    /**
     * Get the normalized material name used for progress tracking
     * 
     * @return The material name
     */
    public String getMaterialName() {
        return materialName;
    }
} 