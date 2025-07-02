package com.thefallersgames.progression.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.thefallersgames.progression.api.adapter.PlaceholderAdapter;
import com.thefallersgames.progression.api.condition.ProgressCondition;
import com.thefallersgames.progression.api.event.ItemUnlockEvent;
import com.thefallersgames.progression.api.factory.ConditionFactory;
import com.thefallersgames.progression.api.service.ProgressService;
import com.thefallersgames.progression.condition.CollectCondition;
import com.thefallersgames.progression.condition.BreakCondition;
import com.thefallersgames.progression.condition.KillsCondition;
import com.thefallersgames.progression.condition.CompositeCondition;
import com.thefallersgames.progression.condition.PrerequisiteCondition;
import com.thefallersgames.progression.data.PlayerDataManager;
import com.thefallersgames.progression.util.ItemUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Default implementation of the ProgressService.
 * This is a core component following the Hexagonal Architecture pattern.
 */
public class DefaultProgressService implements ProgressService {
    
    private final PlayerDataManager dataManager;
    private final ConditionFactory conditionFactory;
    private final PlaceholderAdapter placeholderAdapter;
    private final Map<String, ProgressCondition> itemConditions;
    private final Map<String, String> itemMessages;
    
    /**
     * Create a new DefaultProgressService
     * 
     * @param dataManager The data manager for player progression
     * @param conditionFactory The factory for creating conditions
     * @param placeholderAdapter The adapter for PlaceholderAPI
     */
    public DefaultProgressService(
            PlayerDataManager dataManager, 
            ConditionFactory conditionFactory,
            PlaceholderAdapter placeholderAdapter) {
        this.dataManager = dataManager;
        this.conditionFactory = conditionFactory;
        this.placeholderAdapter = placeholderAdapter;
        this.itemConditions = new HashMap<>();
        this.itemMessages = new HashMap<>();
    }
    
    /**
     * Load item conditions from configuration
     * 
     * @param config The configuration section containing locked item definitions
     */
    public void loadItemConditions(ConfigurationSection config) {
        itemConditions.clear();
        itemMessages.clear();
        
        if (config == null) {
            return;
        }
        
        for (String itemId : config.getKeys(false)) {
            ConfigurationSection itemSection = config.getConfigurationSection(itemId);
            
            if (itemSection == null) {
                continue;
            }
            
            String message = itemSection.getString("message", "You haven't unlocked this item yet.");
            
            // Get the condition configuration to generate dynamic placeholders
            ConfigurationSection conditionSection = itemSection.getConfigurationSection("condition");
            if (conditionSection != null) {
                String conditionType = conditionSection.getString("type");
                
                if (conditionType != null) {
                    // Generate placeholders based on condition type and target
                    String target = null;
                    
                    // Different condition types use different target fields
                    if (conditionType.equals("kills")) {
                        target = conditionSection.getString("entity");
                    } else if (conditionType.equals("collect") || conditionType.equals("break")) {
                        target = conditionSection.getString("material");
                        if (target != null) {
                            target = target.toLowerCase();
                        }
                    }
                    
                    // Dynamic placeholder generation removed as requested
                }
                
                try {
                    ProgressCondition condition = conditionFactory.createCondition(conditionSection);
                    itemConditions.put(itemId.toLowerCase(), condition);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Failed to load condition for item " + itemId + ": " + e.getMessage());
                }
            }
            
            itemMessages.put(itemId.toLowerCase(), message);
        }
    }
    
    @Override
    public boolean isItemLocked(Player player, ItemStack item) {
        ProgressCondition condition = getItemCondition(item);
        
        if (condition == null) {
            return false;
        }
        
        return !meetsCondition(player, condition);
    }
    
    @Override
    public ProgressCondition getItemCondition(ItemStack item) {
        if (item == null) {
            return null;
        }
        
        String itemId = ItemUtil.getItemId(item);
        return itemConditions.get(itemId);
    }
    
    @Override
    public String getItemLockMessage(Player player, ItemStack item) {
        if (item == null) {
            return "This item is not available.";
        }
        
        String itemId = ItemUtil.getItemId(item);
        String message = itemMessages.get(itemId);
        
        if (message == null) {
            return "This item is not available yet.";
        }
        
        // Process our own item-based placeholders
        message = processItemPlaceholders(player, message, itemId);
        
        // Use PlaceholderAPI for any remaining placeholders
        if (placeholderAdapter != null && placeholderAdapter.isAvailable()) {
            message = placeholderAdapter.setPlaceholders(player, message);
        }
        
        return message;
    }
    
    /**
     * Process custom item-based placeholders in a message
     * 
     * @param player The player
     * @param message The message with placeholders
     * @param itemId The item ID
     * @return The message with item placeholders replaced
     */
    private String processItemPlaceholders(Player player, String message, String itemId) {
        if (message == null || itemId == null) {
            return message;
        }
        
        ProgressCondition condition = itemConditions.get(itemId.toLowerCase());
        if (condition == null) {
            return message;
        }
        
        // Replace basic placeholders with prog_ prefix for PlaceholderAPI
        message = message.replace("%prog_" + itemId + "_progress%", String.valueOf(getItemProgress(player, condition)));
        message = message.replace("%prog_" + itemId + "_amount%", String.valueOf(condition.getRequiredProgress()));
        message = message.replace("%prog_" + itemId + "_type%", getConditionType(condition));
        
        int progress = getItemProgress(player, condition);
        int required = condition.getRequiredProgress();
        int percentage = required <= 0 ? 100 : Math.min(100, (progress * 100) / required);
        message = message.replace("%prog_" + itemId + "_percentage%", String.valueOf(percentage));
        
        message = message.replace("%prog_" + itemId + "_unlocked%", meetsCondition(player, condition) ? "yes" : "no");
        message = message.replace("%prog_" + itemId + "_locked%", meetsCondition(player, condition) ? "no" : "yes");
        
        // Handle specific condition types
        if (condition instanceof KillsCondition) {
            KillsCondition killsCondition = (KillsCondition) condition;
            message = message.replace("%prog_" + itemId + "_entity%", killsCondition.getEntityType().toString().toLowerCase());
        } else if (condition instanceof CompositeCondition) {
            CompositeCondition composite = (CompositeCondition) condition;
            
            // Process composite condition specific placeholders
            for (ProgressCondition subCondition : composite.getConditions()) {
                String subType = getConditionType(subCondition);
                String target = "";
                
                if (subCondition instanceof KillsCondition) {
                    KillsCondition killsCondition = (KillsCondition) subCondition;
                    target = killsCondition.getEntityType().toString().toLowerCase();
                    message = message.replace("%prog_" + itemId + "_" + subType + "_" + target + "_progress%", 
                            String.valueOf(getConditionProgress(player, subCondition)));
                    message = message.replace("%prog_" + itemId + "_" + subType + "_" + target + "_amount%", 
                            String.valueOf(subCondition.getRequiredProgress()));
                } else if (subCondition instanceof CollectCondition) {
                    CollectCondition collectCondition = (CollectCondition) subCondition;
                    target = collectCondition.getMaterialType().toString().toLowerCase();
                    message = message.replace("%prog_" + itemId + "_" + subType + "_" + target + "_progress%", 
                            String.valueOf(getConditionProgress(player, subCondition)));
                    message = message.replace("%prog_" + itemId + "_" + subType + "_" + target + "_amount%", 
                            String.valueOf(subCondition.getRequiredProgress()));
                } else if (subCondition instanceof BreakCondition) {
                    BreakCondition breakCondition = (BreakCondition) subCondition;
                    target = breakCondition.getMaterialType().toString().toLowerCase();
                    message = message.replace("%prog_" + itemId + "_" + subType + "_" + target + "_progress%", 
                            String.valueOf(getConditionProgress(player, subCondition)));
                    message = message.replace("%prog_" + itemId + "_" + subType + "_" + target + "_amount%", 
                            String.valueOf(subCondition.getRequiredProgress()));
                } else if (subCondition instanceof PrerequisiteCondition) {
                    PrerequisiteCondition prereqCondition = (PrerequisiteCondition) subCondition;
                    target = prereqCondition.getPrerequisiteItem().toString().toLowerCase();
                    message = message.replace("%prog_" + itemId + "_" + subType + "_" + target + "_progress%", 
                            meetsCondition(player, subCondition) ? "1" : "0");
                    message = message.replace("%prog_" + itemId + "_" + subType + "_" + target + "_amount%", "1");
                }
            }
            
            // Fallbacks for simple placeholders
            for (ProgressCondition subCondition : composite.getConditions()) {
                if (subCondition instanceof KillsCondition) {
                    KillsCondition killsCondition = (KillsCondition) subCondition;
                    message = message.replace("%prog_" + itemId + "_entity%", killsCondition.getEntityType().toString().toLowerCase());
                    break;
                }
            }
        }
        
        if (condition instanceof CollectCondition) {
            CollectCondition collectCondition = (CollectCondition) condition;
            message = message.replace("%prog_" + itemId + "_material%", collectCondition.getMaterialType().toString().toLowerCase());
        } else if (condition instanceof BreakCondition) {
            BreakCondition breakCondition = (BreakCondition) condition;
            message = message.replace("%prog_" + itemId + "_material%", breakCondition.getMaterialType().toString().toLowerCase());
        } else if (condition instanceof CompositeCondition) {
            CompositeCondition composite = (CompositeCondition) condition;
            for (ProgressCondition subCondition : composite.getConditions()) {
                if (subCondition instanceof CollectCondition) {
                    CollectCondition collectCondition = (CollectCondition) subCondition;
                    message = message.replace("%prog_" + itemId + "_material%", collectCondition.getMaterialType().toString().toLowerCase());
                    break;
                } else if (subCondition instanceof BreakCondition) {
                    BreakCondition breakCondition = (BreakCondition) subCondition;
                    message = message.replace("%prog_" + itemId + "_material%", breakCondition.getMaterialType().toString().toLowerCase());
                    break;
                }
            }
        }
        
        return message;
    }
    
    /**
     * Get the type of a condition as a string
     * 
     * @param condition The condition
     * @return The condition type
     */
    private String getConditionType(ProgressCondition condition) {
        if (condition instanceof KillsCondition) return "kills";
        if (condition instanceof CollectCondition) return "collect";
        if (condition instanceof BreakCondition) return "break";
        if (condition instanceof com.thefallersgames.progression.condition.PlaceholderCondition) return "placeholder";
        if (condition instanceof com.thefallersgames.progression.condition.PrerequisiteCondition) return "prerequisite";
        if (condition instanceof CompositeCondition) {
            // For composite, return the most relevant sub-condition type
            CompositeCondition composite = (CompositeCondition) condition;
            for (ProgressCondition subCondition : composite.getConditions()) {
                if (!(subCondition instanceof com.thefallersgames.progression.condition.PrerequisiteCondition)) {
                    return getConditionType(subCondition);
                }
            }
            return "composite";
        }
        return condition.getClass().getSimpleName().replace("Condition", "").toLowerCase();
    }
    
    /**
     * Get the progress for a specific item's condition
     * 
     * @param player The player
     * @param condition The condition
     * @return The progress value
     */
    private int getItemProgress(Player player, ProgressCondition condition) {
        if (condition instanceof CompositeCondition) {
            CompositeCondition composite = (CompositeCondition) condition;
            
            // For composite conditions, find the non-prerequisite condition for progress
            for (ProgressCondition subCondition : composite.getConditions()) {
                if (!(subCondition instanceof com.thefallersgames.progression.condition.PrerequisiteCondition)) {
                    return getConditionProgress(player, subCondition);
                }
            }
            return 0;
        }
        
        return getConditionProgress(player, condition);
    }
    
    /**
     * Get the progress for a specific condition
     * 
     * @param player The player
     * @param condition The condition
     * @return The progress value
     */
    private int getConditionProgress(Player player, ProgressCondition condition) {
        if (condition instanceof KillsCondition) {
            KillsCondition killsCondition = (KillsCondition) condition;
            return dataManager.getProgress(player.getUniqueId(), "kills", killsCondition.getEntityName());
        }
        
        if (condition instanceof CollectCondition) {
            CollectCondition collectCondition = (CollectCondition) condition;
            return dataManager.getProgress(player.getUniqueId(), "collect", collectCondition.getMaterialName());
        }
        
        if (condition instanceof BreakCondition) {
            BreakCondition breakCondition = (BreakCondition) condition;
            return dataManager.getProgress(player.getUniqueId(), "break", breakCondition.getMaterialName());
        }
        
        if (condition instanceof com.thefallersgames.progression.condition.PlaceholderCondition) {
            com.thefallersgames.progression.condition.PlaceholderCondition placeholderCondition = 
                (com.thefallersgames.progression.condition.PlaceholderCondition) condition;
            return dataManager.getProgress(player.getUniqueId(), "placeholder", placeholderCondition.getPlaceholder());
        }
        
        return 0;
    }
    
    @Override
    public void recordProgress(Player player, String conditionType, String key, int amount) {
        if (player == null || conditionType == null || key == null) {
            return;
        }
        
        // Always record progress - removed prerequisite checking here as it was blocking collection
        int newValue = dataManager.addProgress(player.getUniqueId(), conditionType.toLowerCase(), key.toLowerCase(), amount);
        
        // Check for newly unlocked items
        for (String itemId : itemConditions.keySet()) {
            ProgressCondition condition = itemConditions.get(itemId);
            
            // For composite conditions, we will check prerequisites during the meetsCondition call
            // Here we just check if the numerical progress requirement is met
            boolean progressMet = false;
            int requiredValue = condition.getRequiredProgress();
            
            if (condition instanceof CompositeCondition) {
                // For composite conditions, we need to check if the specific progress type matches
                CompositeCondition composite = (CompositeCondition) condition;
                for (ProgressCondition subCondition : composite.getConditions()) {
                    if (!(subCondition instanceof PrerequisiteCondition)) {
                        // Found a non-prerequisite condition (progress-tracking condition)
                        if (matchesProgressType(subCondition, conditionType, key) && 
                            getConditionProgress(player, subCondition) >= subCondition.getRequiredProgress() &&
                            getConditionProgress(player, subCondition) - amount < subCondition.getRequiredProgress()) {
                            // This progress update caused this specific subcondition to be fulfilled
                            progressMet = true;
                            break;
                        }
                    }
                }
            } else if (matchesProgressType(condition, conditionType, key) && 
                      newValue >= requiredValue && (newValue - amount) < requiredValue) {
                progressMet = true;
            }
            
            // Only fire unlock event if progress requirement is met AND condition is fully met
            // (which will include checking prerequisites)
            if (progressMet && meetsCondition(player, condition)) {
                try {
                    // Create an unlock event
                    ItemStack item = new ItemStack(Material.valueOf(itemId.toUpperCase()));
                    ItemUnlockEvent event = new ItemUnlockEvent(player, item, conditionType);
                    Bukkit.getPluginManager().callEvent(event);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Failed to create unlock event for item: " + itemId);
                }
            }
        }
    }
    
    /**
     * Check if a condition matches the given progress type and key
     * 
     * @param condition The condition to check
     * @param conditionType The progress type (e.g., "collect", "break", "kills")
     * @param key The progress key (e.g., "grass_block", "zombie")
     * @return true if the condition is affected by this progress type and key
     */
    private boolean matchesProgressType(ProgressCondition condition, String conditionType, String key) {
        if (condition instanceof CollectCondition) {
            CollectCondition collectCondition = (CollectCondition) condition;
            return conditionType.equals("collect") && 
                   key.equalsIgnoreCase(collectCondition.getMaterialName());
        } else if (condition instanceof KillsCondition) {
            KillsCondition killsCondition = (KillsCondition) condition;
            return conditionType.equals("kills") && 
                   key.equalsIgnoreCase(killsCondition.getEntityName());
        } else if (condition instanceof BreakCondition) {
            BreakCondition breakCondition = (BreakCondition) condition;
            return conditionType.equals("break") && 
                   key.equalsIgnoreCase(breakCondition.getMaterialName());
        }
        return false;
    }
    
    @Override
    public boolean meetsCondition(Player player, ProgressCondition condition) {
        if (player == null || condition == null) {
            return false;
        }
        
        return condition.isMet(player);
    }
    
    /**
     * Get the item conditions map
     * 
     * @return The map of item conditions
     */
    @Override
    public Map<String, ProgressCondition> getAllConditions() {
        return new HashMap<>(itemConditions);
    }
} 