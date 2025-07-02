package com.thefallersgames.progression.condition;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.thefallersgames.progression.api.condition.ProgressCondition;
import com.thefallersgames.progression.data.PlayerDataManager;

/**
 * Condition implementation for breaking specific block types.
 */
public class BreakCondition implements ProgressCondition {
    
    private final PlayerDataManager dataManager;
    private final Material materialType;
    private final int requiredAmount;
    private final String materialName; // Store the actual material name used for lookups
    
    /**
     * Create a new BreakCondition from configuration
     * 
     * @param config The configuration section
     * @param dataManager The player data manager
     */
    public BreakCondition(ConfigurationSection config, PlayerDataManager dataManager) {
        this.dataManager = dataManager;
        
        String materialName = config.getString("material", "STONE").toUpperCase();
        try {
            this.materialType = Material.valueOf(materialName);
            // Store the normalized name that will be used for progress tracking
            this.materialName = this.materialType.toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid material type: " + materialName);
        }
        
        this.requiredAmount = config.getInt("amount", 100);
    }
    
    @Override
    public boolean isMet(Player player) {
        return getCurrentProgress(player) >= requiredAmount;
    }
    
    @Override
    public int getCurrentProgress(Player player) {
        // Use the consistent material name for lookups
        return dataManager.getProgress(player.getUniqueId(), "break", materialName);
    }
    
    @Override
    public int getRequiredProgress() {
        return requiredAmount;
    }
    
    @Override
    public String getDescription() {
        return "Break " + requiredAmount + " " + materialName + " blocks";
    }
    
    /**
     * Get the material type for this condition
     * 
     * @return The material type
     */
    public Material getMaterialType() {
        return materialType;
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