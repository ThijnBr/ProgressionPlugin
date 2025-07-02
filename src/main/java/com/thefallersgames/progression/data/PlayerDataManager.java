package com.thefallersgames.progression.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages player progression data storage and retrieval.
 */
public class PlayerDataManager {
    
    private final JavaPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, Map<String, Map<String, Integer>>> playerData;
    
    /**
     * Create a new PlayerDataManager
     * 
     * @param plugin The plugin instance
     */
    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.playerData = new ConcurrentHashMap<>();
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    /**
     * Load data for a player
     * 
     * @param playerId The player's UUID
     */
    public void loadPlayerData(UUID playerId) {
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        
        Map<String, Map<String, Integer>> playerProgress = new HashMap<>();
        
        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            
            for (String conditionType : config.getKeys(false)) {
                Map<String, Integer> typeProgress = new HashMap<>();
                
                if (config.isConfigurationSection(conditionType)) {
                    for (String key : config.getConfigurationSection(conditionType).getKeys(false)) {
                        int value = config.getInt(conditionType + "." + key);
                        typeProgress.put(key, value);
                    }
                }
                
                playerProgress.put(conditionType, typeProgress);
            }
        }
        
        playerData.put(playerId, playerProgress);
    }
    
    /**
     * Save data for a player
     * 
     * @param playerId The player's UUID
     */
    public void savePlayerData(UUID playerId) {
        Map<String, Map<String, Integer>> playerProgress = playerData.get(playerId);
        
        if (playerProgress == null) {
            return;
        }
        
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        for (String conditionType : playerProgress.keySet()) {
            Map<String, Integer> typeProgress = playerProgress.get(conditionType);
            
            for (String key : typeProgress.keySet()) {
                config.set(conditionType + "." + key, typeProgress.get(key));
            }
        }
        
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + playerId);
            e.printStackTrace();
        }
    }
    
    /**
     * Save data for all online players
     */
    public void saveAllPlayerData() {
        for (UUID playerId : playerData.keySet()) {
            savePlayerData(playerId);
        }
    }
    
    /**
     * Get the current progress value for a player
     * 
     * @param playerId The player's UUID
     * @param conditionType The type of condition
     * @param key The specific key for the condition
     * @return The progress value
     */
    public int getProgress(UUID playerId, String conditionType, String key) {
        Map<String, Map<String, Integer>> playerProgress = playerData.get(playerId);
        
        if (playerProgress == null) {
            return 0;
        }
        
        Map<String, Integer> typeProgress = playerProgress.get(conditionType);
        
        if (typeProgress == null) {
            return 0;
        }
        
        Integer value = typeProgress.get(key);
        return value == null ? 0 : value;
    }
    
    /**
     * Set the progress value for a player
     * 
     * @param playerId The player's UUID
     * @param conditionType The type of condition
     * @param key The specific key for the condition
     * @param value The progress value
     */
    public void setProgress(UUID playerId, String conditionType, String key, int value) {
        Map<String, Map<String, Integer>> playerProgress = playerData.get(playerId);
        
        if (playerProgress == null) {
            playerProgress = new HashMap<>();
            playerData.put(playerId, playerProgress);
        }
        
        Map<String, Integer> typeProgress = playerProgress.get(conditionType);
        
        if (typeProgress == null) {
            typeProgress = new HashMap<>();
            playerProgress.put(conditionType, typeProgress);
        }
        
        typeProgress.put(key, value);
    }
    
    /**
     * Add to the progress value for a player
     * 
     * @param playerId The player's UUID
     * @param conditionType The type of condition
     * @param key The specific key for the condition
     * @param amount The amount to add
     * @return The new progress value
     */
    public int addProgress(UUID playerId, String conditionType, String key, int amount) {
        int currentValue = getProgress(playerId, conditionType, key);
        int newValue = currentValue + amount;
        
        setProgress(playerId, conditionType, key, newValue);
        return newValue;
    }
    
    /**
     * Clear all data for a player
     * 
     * @param playerId The player's UUID
     */
    public void clearPlayerData(UUID playerId) {
        // Remove from memory
        playerData.remove(playerId);
        
        // Delete the file
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        if (playerFile.exists()) {
            playerFile.delete();
        }
    }
} 