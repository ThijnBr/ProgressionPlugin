package com.thefallersgames.progression.condition;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.thefallersgames.progression.api.condition.ProgressCondition;
import com.thefallersgames.progression.api.service.ProgressService;
import com.thefallersgames.progression.api.factory.ConditionFactory;

/**
 * Condition implementation for requiring another item to be unlocked first.
 * This enables creating progression chains where items must be unlocked in sequence.
 */
public class PrerequisiteCondition implements ProgressCondition {
    
    private final ProgressService progressService;
    private final Material prerequisiteItem;
    private ProgressCondition additionalCondition;
    
    /**
     * Create a new PrerequisiteCondition from configuration
     * 
     * @param config The configuration section
     * @param progressService The progress service to check other item conditions
     */
    public PrerequisiteCondition(ConfigurationSection config, ProgressService progressService) {
        this.progressService = progressService;
        
        String itemName = config.getString("item", "WOODEN_SWORD").toUpperCase();
        try {
            this.prerequisiteItem = Material.valueOf(itemName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid prerequisite item: " + itemName);
        }
        
        // Initialize with no additional condition
        this.additionalCondition = null;
    }
    
    /**
     * Create a new PrerequisiteCondition with an additional condition
     * 
     * @param config The configuration section
     * @param progressService The progress service to check other item conditions
     * @param conditionFactory The factory to create additional conditions
     */
    public PrerequisiteCondition(ConfigurationSection config, ProgressService progressService, ConditionFactory conditionFactory) {
        this.progressService = progressService;
        
        String itemName = config.getString("item", "WOODEN_SWORD").toUpperCase();
        try {
            this.prerequisiteItem = Material.valueOf(itemName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid prerequisite item: " + itemName);
        }
        
        // Check for additional direct condition
        ConfigurationSection additionalConfig = config.getConfigurationSection("additional_condition");
        if (additionalConfig != null && conditionFactory != null) {
            try {
                this.additionalCondition = conditionFactory.createCondition(additionalConfig);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to create additional condition: " + e.getMessage());
                this.additionalCondition = null;
            }
        } else {
            this.additionalCondition = null;
        }
    }
    
    @Override
    public boolean isMet(Player player) {
        // First check if the prerequisite item is unlocked
        ItemStack prerequisite = new ItemStack(prerequisiteItem);
        ProgressCondition condition = progressService.getItemCondition(prerequisite);
        
        boolean prerequisiteMet;
        if (condition == null) {
            // If the prerequisite item doesn't have a condition, consider it automatically met
            prerequisiteMet = true;
        } else {
            prerequisiteMet = progressService.meetsCondition(player, condition);
        }
        
        // If we have an additional condition, check that too
        if (additionalCondition != null) {
            return prerequisiteMet && additionalCondition.isMet(player);
        }
        
        return prerequisiteMet;
    }
    
    @Override
    public int getCurrentProgress(Player player) {
        // For prerequisite conditions, we return 0 or 1 (not met/met)
        // If there's an additional condition, we can return its progress
        if (!isMet(player) && additionalCondition != null) {
            return additionalCondition.getCurrentProgress(player);
        }
        
        return isMet(player) ? 1 : 0;
    }
    
    @Override
    public int getRequiredProgress() {
        // For prerequisite conditions, 1 means condition is met
        // If there's an additional condition, we can return its required progress
        if (additionalCondition != null) {
            return additionalCondition.getRequiredProgress();
        }
        
        return 1;
    }
    
    @Override
    public String getDescription() {
        String desc = "Unlock " + prerequisiteItem.name().toLowerCase() + " first";
        
        if (additionalCondition != null) {
            desc += " and " + additionalCondition.getDescription();
        }
        
        return desc;
    }
    
    /**
     * Get the prerequisite item for this condition
     * 
     * @return The prerequisite item material
     */
    public Material getPrerequisiteItem() {
        return prerequisiteItem;
    }
} 