package com.thefallersgames.progression;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import com.thefallersgames.progression.adapter.PlaceholderAPIAdapter;
import com.thefallersgames.progression.api.adapter.PlaceholderAdapter;
import com.thefallersgames.progression.api.facade.UnlockFacade;
import com.thefallersgames.progression.api.factory.ConditionFactory;
import com.thefallersgames.progression.api.service.ProgressService;
import com.thefallersgames.progression.command.ProgressionCommand;
import com.thefallersgames.progression.condition.BreakCondition;
import com.thefallersgames.progression.condition.CollectCondition;
import com.thefallersgames.progression.condition.KillsCondition;
import com.thefallersgames.progression.condition.PlaceholderCondition;
import com.thefallersgames.progression.data.PlayerDataManager;
import com.thefallersgames.progression.facade.ProgressionFacade;
import com.thefallersgames.progression.listener.InventoryListener;
import com.thefallersgames.progression.listener.ItemPickupListener;
import com.thefallersgames.progression.listener.ItemUseListener;
import com.thefallersgames.progression.listener.PlayerEquipListener;
import com.thefallersgames.progression.listener.PlayerListener;
import com.thefallersgames.progression.listener.ProgressionListener;
import com.thefallersgames.progression.placeholder.ProgressionExpansion;
import com.thefallersgames.progression.service.DefaultProgressService;
import com.thefallersgames.progression.condition.PrerequisiteCondition;
import com.thefallersgames.progression.condition.CompositeCondition;

/**
 * Main plugin class for Progression.
 * This follows a modular architecture with dependency injection.
 */
public class Progression extends JavaPlugin {
    
    // Core components
    private PlayerDataManager playerDataManager;
    private ConditionFactory conditionFactory;
    private PlaceholderAdapter placeholderAdapter;
    private ProgressService progressService;
    private UnlockFacade unlockFacade;
    
    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize core components
        initializeComponents();
        
        // Register event listeners
        registerListeners();
        
        // Register commands
        registerCommands();
        
        // Register PlaceholderAPI expansion if available
        registerPlaceholderExpansion();
        
        getLogger().info("Progression plugin enabled!");
    }
    
    /**
     * Initialize the core components of the plugin
     */
    private void initializeComponents() {
        // Create data manager
        playerDataManager = new PlayerDataManager(this);
        
        // Create factory and register condition types
        conditionFactory = new ConditionFactory();
        registerConditionTypes();
        
        // Create placeholder adapter
        placeholderAdapter = new PlaceholderAPIAdapter();
        
        // Create progress service
        progressService = new DefaultProgressService(playerDataManager, conditionFactory, placeholderAdapter);
        
        // Register conditions that depend on progressService
        conditionFactory.registerCondition("prerequisite", config -> 
            new PrerequisiteCondition(config, progressService, conditionFactory));
            
        // Register composite condition
        conditionFactory.registerCondition("composite", config ->
            new CompositeCondition(config, conditionFactory));
        
        // Load conditions from config
        ConfigurationSection lockedItems = getConfig().getConfigurationSection("locked-items");
        ((DefaultProgressService) progressService).loadItemConditions(lockedItems);
        
        // Create unlock facade
        unlockFacade = new ProgressionFacade(progressService);
    }
    
    /**
     * Register condition types with the factory
     */
    private void registerConditionTypes() {
        // Register kill condition
        conditionFactory.registerCondition("kills", config -> 
            new KillsCondition(config, playerDataManager));
        
        // Register placeholder condition
        conditionFactory.registerCondition("placeholder", config -> 
            new PlaceholderCondition(config, placeholderAdapter));
        
        // Register collect condition
        conditionFactory.registerCondition("collect", config -> 
            new CollectCondition(config, playerDataManager));
        
        // Register break condition
        conditionFactory.registerCondition("break", config -> 
            new BreakCondition(config, playerDataManager));
    }
    
    /**
     * Register event listeners
     */
    private void registerListeners() {
        // Create inventory listener first since other listeners depend on it
        InventoryListener inventoryListener = new InventoryListener(unlockFacade);
        
        // Register inventory listener
        getServer().getPluginManager().registerEvents(inventoryListener, this);
        
        // Register item use listener
        getServer().getPluginManager().registerEvents(
            new ItemUseListener(unlockFacade, inventoryListener, this), this);
        
        // Register item pickup listener
        getServer().getPluginManager().registerEvents(
            new ItemPickupListener(unlockFacade, inventoryListener, this), this);
        
        // Register player equip listener
        getServer().getPluginManager().registerEvents(
            new PlayerEquipListener(unlockFacade, inventoryListener, this), this);
        
        // Register player login listener
        getServer().getPluginManager().registerEvents(
            new PlayerListener(playerDataManager, inventoryListener, this), this);
        
        // Register progression listener for tracking events
        getServer().getPluginManager().registerEvents(
            new ProgressionListener(progressService, inventoryListener, this), this);
    }
    
    /**
     * Register commands
     */
    private void registerCommands() {
        getCommand("prog").setExecutor(new ProgressionCommand(this, progressService, unlockFacade));
    }
    
    /**
     * Register PlaceholderAPI expansion
     */
    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ProgressionExpansion(this, progressService).register();
            getLogger().info("PlaceholderAPI found - registering expansion");
        } else {
            getLogger().warning("PlaceholderAPI not found - placeholders won't work");
        }
    }
    
    /**
     * Reload the plugin configuration and reinitialize necessary components
     */
    public void reloadPluginConfig() {
        // Reload the config from disk
        reloadConfig();
        
        // Reload item conditions
        ConfigurationSection lockedItems = getConfig().getConfigurationSection("locked-items");
        ((DefaultProgressService) progressService).loadItemConditions(lockedItems);
        
        getLogger().info("Progression configuration reloaded");
    }
    
    @Override
    public void onDisable() {
        // Save player data
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        
        getLogger().info("Progression plugin disabled!");
    }
    
    /**
     * Get the progress service
     * 
     * @return The progress service
     */
    public ProgressService getProgressService() {
        return progressService;
    }
    
    /**
     * Get the unlock facade
     * 
     * @return The unlock facade
     */
    public UnlockFacade getUnlockFacade() {
        return unlockFacade;
    }
    
    /**
     * Get the player data manager
     * 
     * @return The player data manager
     */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
} 