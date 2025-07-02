# Developer Guide

This document provides guidance for developers who want to extend or integrate with the Progression plugin.

## API Overview

The Progression plugin provides a clean API for developers to interact with the progression system. This API allows you to:

1. Check if items are locked for players
2. Get player progress for specific conditions
3. Record progress programmatically
4. Create custom condition types

## Adding Progression as a Dependency

### Maven

```xml
<repositories>
    <repository>
        <id>spigot-repo</id>
        <url>https://hub.spigotmc.org/nexus/content/repositories/snapshots/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.thefallersgames</groupId>
        <artifactId>progression</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle

```groovy
repositories {
    maven { url 'https://hub.spigotmc.org/nexus/content/repositories/snapshots/' }
}

dependencies {
    compileOnly 'com.thefallersgames:progression:1.0-SNAPSHOT'
}
```

### plugin.yml

```yaml
depend: [Progression]
# or
softdepend: [Progression]
```

## Basic API Usage

### Getting the Progression API

```java
public class MyPlugin extends JavaPlugin {
    
    private Progression progression;
    private ProgressService progressService;
    private UnlockFacade unlockFacade;
    
    @Override
    public void onEnable() {
        // Get the Progression plugin instance
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Progression");
        if (plugin instanceof Progression) {
            progression = (Progression) plugin;
            
            // Get the main API components
            progressService = progression.getProgressService();
            unlockFacade = progression.getUnlockFacade();
            
            getLogger().info("Successfully hooked into Progression!");
        } else {
            getLogger().warning("Could not find Progression plugin!");
        }
    }
}
```

### Checking if Items are Locked

```java
/**
 * Check if a player can use an item
 */
public boolean canPlayerUseItem(Player player, ItemStack item) {
    // Quick check with the facade
    return unlockFacade.canUseItem(player, item);
}

/**
 * Get a status message for an item
 */
public String getItemStatus(Player player, ItemStack item) {
    return unlockFacade.getItemStatusMessage(player, item);
}

/**
 * Example usage in an event handler
 */
@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();
    
    if (item != null) {
        if (!unlockFacade.canUseItem(player, item)) {
            event.setCancelled(true);
            player.sendMessage("You cannot use this item yet!");
        }
    }
}
```

### Working with Progress Data

```java
/**
 * Get a player's progress for a specific condition
 */
public int getPlayerProgress(Player player, ItemStack item) {
    ProgressCondition condition = progressService.getItemCondition(item);
    if (condition != null) {
        return condition.getCurrentProgress(player);
    }
    return 0;
}

/**
 * Get the required progress amount for an item
 */
public int getRequiredProgress(ItemStack item) {
    ProgressCondition condition = progressService.getItemCondition(item);
    if (condition != null) {
        return condition.getRequiredProgress();
    }
    return 0;
}

/**
 * Record progress for a player
 */
public void addProgress(Player player, String conditionType, String key, int amount) {
    progressService.recordProgress(player, conditionType, key, amount);
}
```

### Directly Managing Player Progression

You can directly manipulate player progression data using the PlayerDataManager:

```java
/**
 * Access the PlayerDataManager
 */
private PlayerDataManager getPlayerDataManager() {
    return progression.getPlayerDataManager();
}

/**
 * Set a player's progress to a specific value
 * Useful for unlocking or locking progression
 */
public void setPlayerProgress(Player player, String conditionType, String key, int value) {
    PlayerDataManager dataManager = getPlayerDataManager();
    dataManager.setProgress(player.getUniqueId(), conditionType, key, value);
}

/**
 * Reset all progression data for a player
 */
public void resetPlayerProgress(Player player) {
    PlayerDataManager dataManager = getPlayerDataManager();
    dataManager.clearPlayerData(player.getUniqueId());
    dataManager.loadPlayerData(player.getUniqueId());
}

/**
 * Check if a player meets a specific condition requirement
 */
public boolean checkConditionRequirement(Player player, String conditionType, String key, int requiredAmount) {
    PlayerDataManager dataManager = getPlayerDataManager();
    int currentAmount = dataManager.getProgress(player.getUniqueId(), conditionType, key);
    return currentAmount >= requiredAmount;
}

/**
 * Unlock an item for a player by setting progress to a high value
 */
public void unlockItem(Player player, ItemStack item) {
    ProgressCondition condition = progressService.getItemCondition(item);
    if (condition == null) return; // Item isn't tracked
    
    // Set progress to a very high value to ensure it's unlocked
    // In a real implementation, you'd need to determine the condition type and key
    setPlayerProgress(player, "kills", "zombie", 999999); // Example for zombie kills
}
```

## Creating Custom Condition Types

### 1. Create a Custom Condition Class

```java
public class TimeCondition implements ProgressCondition {
    
    private final long requiredPlayTime; // in ticks
    private final StatisticsManager statsManager;
    
    /**
     * Create a new time condition
     * 
     * @param config The configuration
     * @param statsManager The stats manager
     */
    public TimeCondition(ConfigurationSection config, StatisticsManager statsManager) {
        this.statsManager = statsManager;
        
        // Get required time in minutes, convert to ticks
        int minutes = config.getInt("minutes", 60);
        this.requiredPlayTime = minutes * 60 * 20; // Convert to ticks
    }
    
    @Override
    public boolean isMet(Player player) {
        return getCurrentProgress(player) >= requiredPlayTime;
    }
    
    @Override
    public int getCurrentProgress(Player player) {
        // Get the player's play time
        Statistic playTimeStat = Statistic.PLAY_ONE_MINUTE; // Despite the name, this is in ticks
        return player.getStatistic(playTimeStat);
    }
    
    @Override
    public int getRequiredProgress() {
        return (int) requiredPlayTime;
    }
    
    @Override
    public String getDescription() {
        return "Play for " + (requiredPlayTime / (60 * 20)) + " minutes";
    }
}
```

### 2. Register Your Condition Type

In your plugin's onEnable method, register your custom condition:

```java
@Override
public void onEnable() {
    // Get the Progression plugin
    Progression progression = (Progression) Bukkit.getPluginManager().getPlugin("Progression");
    
    // Get the condition factory from the plugin
    ConditionFactory factory = progression.getConditionFactory();
    
    // Create a stats manager
    StatisticsManager statsManager = new StatisticsManager();
    
    // Register your custom condition type
    factory.registerCondition("time", config -> new TimeCondition(config, statsManager));
    
    getLogger().info("Registered custom time condition!");
}
```

### 3. Use Your Condition in Configuration

Users can now use your custom condition in their config.yml:

```yaml
locked-items:
  netherite_sword:
    message: "You need to play for at least 2 hours to use a Netherite Sword!"
    condition:
      type: time
      minutes: 120
```

## Using Events

The plugin provides custom events you can listen for in your own plugin:

### ItemUnlockEvent

```java
@EventHandler
public void onItemUnlock(ItemUnlockEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();
    String conditionType = event.getConditionType();
    
    // Do something when a player unlocks an item
    player.sendMessage("Congratulations! You've unlocked " + item.getType().name());
    
    // Maybe give a reward based on the condition type
    if (conditionType.equals("kills")) {
        player.giveExp(100);
    }
}
```

## Best Practices

1. **Always check if the API is available** before using it, as Progression might not be loaded or could be disabled.

2. **Use the facade** whenever possible for the simplest integration.

3. **Don't modify conditions directly** - instead use the provided API methods.

4. **Handle exceptions** that might occur when integrating with Progression.

5. **Test thoroughly** as configuration changes can affect how conditions are evaluated.

6. **Document your integration** so users know how your plugin works with Progression.

## Common Patterns

### Integration Pattern 1: Enhancement

Enhance Progression by adding new condition types:

```java
public class MyEnhancementPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Register new conditions
        Progression prog = (Progression) Bukkit.getPluginManager().getPlugin("Progression");
        prog.getConditionFactory().registerCondition("mycondition", 
            config -> new MyCustomCondition(config));
    }
}
```

### Integration Pattern 2: Consumer

Use Progression's API to check if items are locked:

```java
public class MyShopPlugin extends JavaPlugin {
    private UnlockFacade unlockFacade;
    
    @Override
    public void onEnable() {
        setupProgression();
    }
    
    private void setupProgression() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Progression");
        if (plugin instanceof Progression) {
            unlockFacade = ((Progression) plugin).getUnlockFacade();
        }
    }
    
    public boolean canPlayerBuy(Player player, ItemStack item) {
        // First check if player has enough money
        boolean hasEnoughMoney = economy.has(player, getPrice(item));
        
        // Then check if the item is unlocked
        boolean isUnlocked = true;
        if (unlockFacade != null) {
            isUnlocked = unlockFacade.canUseItem(player, item);
        }
        
        return hasEnoughMoney && isUnlocked;
    }
}
```

### Integration Pattern 3: Progress Provider

Provide progress data to Progression:

```java
public class MyProgressPlugin extends JavaPlugin implements Listener {
    private ProgressService progressService;
    
    @Override
    public void onEnable() {
        setupProgression();
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    private void setupProgression() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Progression");
        if (plugin instanceof Progression) {
            progressService = ((Progression) plugin).getProgressService();
        }
    }
    
    @EventHandler
    public void onCustomAction(CustomActionEvent event) {
        Player player = event.getPlayer();
        String action = event.getActionType();
        int amount = event.getAmount();
        
        // Record progress for this custom action
        if (progressService != null) {
            progressService.recordProgress(player, "custom", action, amount);
        }
    }
}
```

## Troubleshooting

### Common Issues

1. **NoClassDefFoundError**: Make sure you're properly setting Progression as a dependency in your plugin.yml.

2. **NullPointerException**: Check if the Progression plugin is installed and enabled before accessing its API.

3. **IllegalArgumentException**: This usually happens when creating invalid conditions. Check your configuration.

### Debugging Tips

1. Use the `/prog status` command to check if items are properly registered.

2. Enable debug logging in Progression's config.yml to see more detailed information.

3. Test your condition types thoroughly with different configuration values.

## API Reference

See the [Javadoc](https://thefallersgames.github.io/progression/javadoc/) for a complete API reference. 