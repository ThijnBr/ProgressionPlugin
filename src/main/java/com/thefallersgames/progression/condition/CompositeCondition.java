package com.thefallersgames.progression.condition;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;

import com.thefallersgames.progression.api.condition.ProgressCondition;
import com.thefallersgames.progression.api.factory.ConditionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condition implementation that evaluates multiple conditions with an AND operator.
 * All conditions must be met for the composite condition to be met.
 * Prerequisites must be met before progress on other conditions starts counting.
 */
public class CompositeCondition implements ProgressCondition {
    
    private final List<ProgressCondition> conditions;
    private final List<ProgressCondition> prerequisites;
    private final List<ProgressCondition> otherConditions;
    
    /**
     * Create a new CompositeCondition from configuration
     * 
     * @param config The configuration section
     * @param conditionFactory The factory for creating conditions
     */
    public CompositeCondition(ConfigurationSection config, ConditionFactory conditionFactory) {
        this.conditions = new ArrayList<>();
        this.prerequisites = new ArrayList<>();
        this.otherConditions = new ArrayList<>();
        
        // Get the conditions list
        List<ConfigurationSection> conditionSections = new ArrayList<>();
        
        if (config.isList("conditions")) {
            // If conditions is a list in the YAML
            List<?> conditionList = config.getList("conditions");
            if (conditionList != null) {
                for (Object obj : conditionList) {
                    // Convert map to configuration section
                    if (obj instanceof Map) {
                        MemoryConfiguration memoryConfig = new MemoryConfiguration();
                        ConfigurationSection section = memoryConfig.createSection("condition", (Map<?, ?>) obj);
                        conditionSections.add(section);
                    }
                }
            }
        } else if (config.isConfigurationSection("conditions")) {
            // If conditions is a section in the YAML
            ConfigurationSection conditionsSection = config.getConfigurationSection("conditions");
            if (conditionsSection != null) {
                for (String key : conditionsSection.getKeys(false)) {
                    ConfigurationSection section = conditionsSection.getConfigurationSection(key);
                    if (section != null) {
                        conditionSections.add(section);
                    }
                }
            }
        }
        
        // Create conditions from each section
        for (ConfigurationSection section : conditionSections) {
            try {
                String type = section.getString("type");
                if (type == null) {
                    Bukkit.getLogger().warning("[Progression] Missing type in condition section");
                    continue;
                }
                
                ProgressCondition condition = conditionFactory.createCondition(section);
                if (condition != null) {
                    conditions.add(condition);
                    
                    // Separate prerequisites from other conditions
                    if (condition instanceof PrerequisiteCondition) {
                        prerequisites.add(condition);
                    } else {
                        otherConditions.add(condition);
                    }
                } else {
                    Bukkit.getLogger().warning("[Progression] Failed to create sub-condition of type: " + type);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Progression] Error creating sub-condition: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (conditions.isEmpty()) {
            Bukkit.getLogger().warning("[Progression] Composite condition has no valid sub-conditions!");
        }
    }
    
    @Override
    public boolean isMet(Player player) {
        // All conditions must be met (AND logic)
        if (conditions.isEmpty()) {
            return true; // No conditions means it's met by default
        }
        
        // First check all prerequisite conditions
        for (ProgressCondition condition : prerequisites) {
            if (!condition.isMet(player)) {
                return false; // Prerequisites must be met first
            }
        }
        
        // Then check all other conditions
        for (ProgressCondition condition : otherConditions) {
            if (!condition.isMet(player)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public int getCurrentProgress(Player player) {
        // First check if all prerequisites are met
        for (ProgressCondition prerequisite : prerequisites) {
            if (!prerequisite.isMet(player)) {
                // If any prerequisite is not met, report its progress
                return prerequisite.getCurrentProgress(player);
            }
        }
        
        // All prerequisites are met, now check other conditions
        for (ProgressCondition condition : otherConditions) {
            if (!condition.isMet(player)) {
                return condition.getCurrentProgress(player);
            }
        }
        
        // All conditions are met, return the last condition's progress
        if (!conditions.isEmpty()) {
            return conditions.get(conditions.size() - 1).getCurrentProgress(player);
        }
        return 1;
    }
    
    @Override
    public int getRequiredProgress() {
        // First check prerequisites
        for (ProgressCondition prerequisite : prerequisites) {
            // If we have prerequisites, report the first one's required progress
            return prerequisite.getRequiredProgress();
        }
        
        // If no prerequisites, check other conditions
        if (!otherConditions.isEmpty()) {
            return otherConditions.get(0).getRequiredProgress();
        }
        
        return 1; // Default if no conditions
    }
    
    @Override
    public String getDescription() {
        StringBuilder description = new StringBuilder("Meet all conditions: ");
        
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                description.append(" AND ");
            }
            description.append(conditions.get(i).getDescription());
        }
        
        return description.toString();
    }
    
    /**
     * Check if all prerequisites are met for a player
     * 
     * @param player The player to check
     * @return true if all prerequisites are met, false otherwise
     */
    public boolean arePrerequisitesMet(Player player) {
        for (ProgressCondition prerequisite : prerequisites) {
            if (!prerequisite.isMet(player)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the list of conditions that make up this composite
     * 
     * @return The list of conditions
     */
    public List<ProgressCondition> getConditions() {
        return conditions;
    }
} 