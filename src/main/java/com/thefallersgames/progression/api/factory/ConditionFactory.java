package com.thefallersgames.progression.api.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.configuration.ConfigurationSection;

import com.thefallersgames.progression.api.condition.ProgressCondition;

/**
 * Factory for creating ProgressCondition instances based on configuration.
 * Follows the Factory Method pattern allowing extensibility without modifying core code.
 */
public class ConditionFactory {
    
    private final Map<String, Function<ConfigurationSection, ProgressCondition>> creators = new HashMap<>();
    
    /**
     * Register a condition type with its creator function
     * 
     * @param type The condition type identifier
     * @param creator A function that creates a condition from config
     */
    public void registerCondition(String type, Function<ConfigurationSection, ProgressCondition> creator) {
        creators.put(type.toLowerCase(), creator);
    }
    
    /**
     * Create a condition based on the configuration
     * 
     * @param config The configuration section containing condition details
     * @return A new ProgressCondition instance
     * @throws IllegalArgumentException if the condition type is not registered
     */
    public ProgressCondition createCondition(ConfigurationSection config) {
        if (config == null) {
            throw new IllegalArgumentException("Condition configuration cannot be null");
        }
        
        String type = config.getString("type");
        if (type == null) {
            throw new IllegalArgumentException("Condition type must be specified");
        }
        
        Function<ConfigurationSection, ProgressCondition> creator = creators.get(type.toLowerCase());
        if (creator == null) {
            throw new IllegalArgumentException("Unknown condition type: " + type);
        }
        
        return creator.apply(config);
    }
} 