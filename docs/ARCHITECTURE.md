# Architecture Overview

This document describes the architecture of the Progression plugin, detailing its design patterns, component organization, and how the different parts of the system work together.

## Design Philosophy

The Progression plugin is built on several key design principles:

1. **Hexagonal Architecture (Ports & Adapters)**: Core domain logic is isolated from external dependencies.
2. **Dependency Injection**: All components receive their dependencies through constructors rather than using singletons.
3. **SOLID Principles**: Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, and Dependency Inversion.
4. **Design Patterns**: Factory, Adapter, Facade, and Event-Driven patterns for solving common problems.

## Package Structure

```
com.thefallersgames.progression/
 ├── Progression.java                 # Main plugin class
 ├── api/                             # Core domain interfaces (ports)
 │    ├── adapter/                    # External system adapters
 │    ├── condition/                  # Progression condition interfaces
 │    ├── event/                      # Custom events
 │    ├── facade/                     # Simplified access interfaces
 │    ├── factory/                    # Factory interfaces
 │    └── service/                    # Core service interfaces
 ├── adapter/                         # Adapter implementations
 ├── command/                         # Command handlers
 ├── condition/                       # Condition implementations
 ├── data/                            # Data persistence
 ├── facade/                          # Facade implementations
 ├── listener/                        # Event listeners
 ├── placeholder/                     # PlaceholderAPI integration
 ├── service/                         # Service implementations
 └── util/                            # Utility classes
```

## Core Components

### Interfaces (Ports)

The core domain defines interfaces that describe the capabilities of the system:

- `ProgressCondition`: Represents a condition that must be met for progression
- `ProgressService`: Core service for checking and recording progression
- `UnlockFacade`: Simplified interface for other plugins to interact with
- `PlaceholderAdapter`: Interface for interacting with placeholder systems

### Implementations (Adapters)

Concrete implementations of the interfaces that connect to the outside world:

- `KillsCondition`: Implementation for entity kill conditions
- `PlaceholderCondition`: Implementation that uses PlaceholderAPI values
- `DefaultProgressService`: Main service implementation
- `ProgressionFacade`: Implementation of the unlock facade
- `PlaceholderAPIAdapter`: Adapter for PlaceholderAPI

## Key Design Patterns

### Hexagonal Architecture

The plugin uses the Hexagonal Architecture pattern (also known as Ports & Adapters) to separate core business logic from external systems.

```
                    ┌─────────────────────────┐
                    │     External Systems    │
                    │ (Bukkit, PlaceholderAPI)│
                    └───────────┬─────────────┘
                                │
                                │
┌───────────────────┐  ┌───────▼────────┐  ┌───────────────────┐
│      Adapters     │◄─┤     Ports      │─►│  Domain Model     │
│(Implementations of│  │  (Interfaces)   │  │ (Core Business    │
│   interfaces)     │  │                 │  │    Logic)         │
└───────────────────┘  └────────────────┘  └───────────────────┘
```

- **Ports**: Core interfaces in the api package
- **Adapters**: Implementations that connect to external systems
- **Domain Model**: Core business logic independent of external systems

### Factory Pattern

The `ConditionFactory` is used to create different types of `ProgressCondition` implementations:

```java
// Register condition types
conditionFactory.registerCondition("kills", config -> 
    new KillsCondition(config, playerDataManager));

// Create a condition based on configuration
ProgressCondition condition = conditionFactory.createCondition(config);
```

This allows adding new condition types without modifying existing code (Open/Closed Principle).

### Dependency Injection

All components receive their dependencies through constructors rather than accessing them statically:

```java
// Before (static access)
ConfigManager.getInstance().loadConfig();

// After (dependency injection)
public DefaultProgressService(
    PlayerDataManager dataManager, 
    ConditionFactory conditionFactory,
    PlaceholderAdapter placeholderAdapter) {
    this.dataManager = dataManager;
    this.conditionFactory = conditionFactory;
    this.placeholderAdapter = placeholderAdapter;
}
```

This improves testability and reduces coupling.

### Facade Pattern

The `UnlockFacade` provides a simplified interface for checking if players can use items:

```java
// Complex logic internally
public boolean handleItemUse(Player player, ItemStack item) {
    if (!isTrackedItem(item)) {
        return true;
    }
    
    if (canUseItem(player, item)) {
        return true;
    } else {
        player.sendMessage(ChatColor.RED + progressService.getItemLockMessage(player, item));
        return false;
    }
}

// Simple external usage
if (!unlockFacade.handleItemUse(player, item)) {
    event.setCancelled(true);
}
```

### Adapter Pattern

The `PlaceholderAdapter` interface abstracts away the details of PlaceholderAPI:

```java
// Interface (port)
public interface PlaceholderAdapter {
    String setPlaceholders(OfflinePlayer player, String text);
    boolean isAvailable();
}

// Implementation (adapter)
public class PlaceholderAPIAdapter implements PlaceholderAdapter {
    @Override
    public String setPlaceholders(OfflinePlayer player, String text) {
        if (!available) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
```

### Event-Driven Design

Custom events are used to communicate across components without tight coupling:

```java
// Fire event when an item is unlocked
ItemUnlockEvent event = new ItemUnlockEvent(player, item, conditionType);
Bukkit.getPluginManager().callEvent(event);

// Listen for the event in another component
@EventHandler
public void onItemUnlock(ItemUnlockEvent event) {
    // Handle the event
}
```

## Data Flow

1. **Initialization**:
   - Plugin loads and creates core components
   - ConditionFactory registers condition types
   - Event listeners are registered
   
2. **Configuration Loading**:
   - Configuration is read from config.yml
   - Conditions are created via ConditionFactory
   - ProgressService stores condition mappings
   
3. **Player Interactions**:
   - Players perform actions (kill mobs, break blocks, etc.)
   - Listeners detect these actions and call ProgressService.recordProgress()
   - Progress is stored in PlayerDataManager
   
4. **Item Use Checking**:
   - When a player tries to use an item, the ItemUseListener is triggered
   - The listener uses UnlockFacade to check if the player can use the item
   - If the item is locked, the action is cancelled and a message is sent

## Extending the System

### Adding a New Condition Type

1. Create a new implementation of the `ProgressCondition` interface:

```java
public class LocationCondition implements ProgressCondition {
    private final String worldName;
    private final double x, y, z;
    private final double radius;
    
    public LocationCondition(ConfigurationSection config) {
        this.worldName = config.getString("world");
        this.x = config.getDouble("x");
        this.y = config.getDouble("y");
        this.z = config.getDouble("z");
        this.radius = config.getDouble("radius", 10.0);
    }
    
    @Override
    public boolean isMet(Player player) {
        Location playerLoc = player.getLocation();
        if (!playerLoc.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        Location targetLoc = new Location(player.getWorld(), x, y, z);
        return playerLoc.distance(targetLoc) <= radius;
    }
    
    // Implement other methods...
}
```

2. Register the new condition type with the factory:

```java
conditionFactory.registerCondition("location", config -> 
    new LocationCondition(config));
```

3. Add documentation for the new condition type.

### Integrating with Another Plugin

1. Create a new adapter for the external plugin:

```java
public interface EconomyAdapter {
    double getBalance(UUID playerId);
    boolean hasEnough(UUID playerId, double amount);
}

public class VaultEconomyAdapter implements EconomyAdapter {
    private final Economy economy;
    
    public VaultEconomyAdapter() {
        RegisteredServiceProvider<Economy> rsp = 
            Bukkit.getServicesManager().getRegistration(Economy.class);
        this.economy = rsp.getProvider();
    }
    
    @Override
    public double getBalance(UUID playerId) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return economy.getBalance(player);
    }
    
    // Implement other methods...
}
```

2. Create a condition type that uses the adapter:

```java
public class EconomyCondition implements ProgressCondition {
    private final EconomyAdapter economyAdapter;
    private final double amount;
    
    public EconomyCondition(ConfigurationSection config, EconomyAdapter economyAdapter) {
        this.economyAdapter = economyAdapter;
        this.amount = config.getDouble("amount");
    }
    
    @Override
    public boolean isMet(Player player) {
        return economyAdapter.hasEnough(player.getUniqueId(), amount);
    }
    
    // Implement other methods...
}
```

3. Register the new condition type with the factory.

## Testing Strategies

The architecture is designed to be testable through:

1. **Interfaces**: All major components implement interfaces, allowing for mock implementations in tests.
2. **Dependency Injection**: Dependencies are provided to components rather than created internally, making it easy to provide test doubles.
3. **Separation of Concerns**: Each class has a single responsibility, making unit testing simpler.

Example test for KillsCondition:

```java
@Test
public void testKillsConditionIsMet() {
    // Create a mock data manager
    PlayerDataManager mockDataManager = mock(PlayerDataManager.class);
    when(mockDataManager.getProgress(any(), eq("kills"), eq("zombie"))).thenReturn(60);
    
    // Create the condition
    ConfigurationSection config = mock(ConfigurationSection.class);
    when(config.getString("entity", "zombie")).thenReturn("zombie");
    when(config.getInt("amount", 50)).thenReturn(50);
    
    KillsCondition condition = new KillsCondition(config, mockDataManager);
    
    // Create a mock player
    Player player = mock(Player.class);
    UUID playerId = UUID.randomUUID();
    when(player.getUniqueId()).thenReturn(playerId);
    
    // Test the condition
    assertTrue(condition.isMet(player));
}
``` 