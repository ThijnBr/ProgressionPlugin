package com.thefallersgames.progression.condition;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.thefallersgames.progression.api.condition.ProgressCondition;
import com.thefallersgames.progression.data.PlayerDataManager;

/**
 * Condition implementation for killing specific entity types.
 */
public class KillsCondition implements ProgressCondition {
    
    private final PlayerDataManager dataManager;
    private final EntityType entityType;
    private final int requiredAmount;
    private final String entityName; // Store the actual entity name used for lookups
    
    /**
     * Create a new KillsCondition from configuration
     * 
     * @param config The configuration section
     * @param dataManager The player data manager
     */
    public KillsCondition(ConfigurationSection config, PlayerDataManager dataManager) {
        this.dataManager = dataManager;
        
        String entityName = config.getString("entity", "zombie").toUpperCase();
        try {
            this.entityType = EntityType.valueOf(entityName);
            // Store the normalized name that will be used for progress tracking
            this.entityName = this.entityType.toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid entity type: " + entityName);
        }
        
        this.requiredAmount = config.getInt("amount", 50);
    }
    
    @Override
    public boolean isMet(Player player) {
        return getCurrentProgress(player) >= requiredAmount;
    }
    
    @Override
    public int getCurrentProgress(Player player) {
        // Use the consistent entity name for lookups
        return dataManager.getProgress(player.getUniqueId(), "kills", entityName);
    }
    
    @Override
    public int getRequiredProgress() {
        return requiredAmount;
    }
    
    @Override
    public String getDescription() {
        return "Kill " + requiredAmount + " " + entityName + "s";
    }
    
    /**
     * Get the entity type for this condition
     * 
     * @return The entity type
     */
    public EntityType getEntityType() {
        return entityType;
    }
    
    /**
     * Get the normalized entity name used for progress tracking
     * 
     * @return The entity name
     */
    public String getEntityName() {
        return entityName;
    }
} 