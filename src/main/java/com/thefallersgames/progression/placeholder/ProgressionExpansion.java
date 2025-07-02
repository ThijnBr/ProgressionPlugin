package com.thefallersgames.progression.placeholder;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.thefallersgames.progression.Progression;
import com.thefallersgames.progression.api.condition.ProgressCondition;
import com.thefallersgames.progression.api.service.ProgressService;
import com.thefallersgames.progression.condition.CompositeCondition;
import com.thefallersgames.progression.condition.KillsCondition;
import com.thefallersgames.progression.condition.CollectCondition;
import com.thefallersgames.progression.condition.BreakCondition;
import com.thefallersgames.progression.condition.PlaceholderCondition;
import com.thefallersgames.progression.condition.PrerequisiteCondition;
import com.thefallersgames.progression.data.PlayerDataManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.Map;

/**
 * PlaceholderAPI expansion for the Progression plugin.
 * This is an adapter between PlaceholderAPI and our plugin.
 */
public class ProgressionExpansion extends PlaceholderExpansion {
    
    private final Progression plugin;
    private final ProgressService progressService;
    private final PlayerDataManager dataManager;
    
    /**
     * Create a new ProgressionExpansion
     * 
     * @param plugin The plugin instance
     * @param progressService The progress service
     */
    public ProgressionExpansion(Progression plugin, ProgressService progressService) {
        this.plugin = plugin;
        this.progressService = progressService;
        this.dataManager = plugin.getPlayerDataManager();
    }
    
    @Override
    public String getIdentifier() {
        return "prog";
    }
    
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        
        // Support for %prog_itemid_conditiontype_target_progress% and %prog_itemid_conditiontype_target_amount%
        // Example: %prog_golden_apple_collect_apple_progress%
        String[] parts = identifier.split("_");
        if (parts.length >= 4) {
            String itemId = parts[0];
            String conditionType = parts[1];
            String target = parts[2];
            String property = parts[3];

            // Try to get the composite condition for this item
            ProgressCondition condition = null;
            Material material = null;
            try {
                material = Material.valueOf(itemId.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = null;
            }
            
            if (material != null) {
                condition = progressService.getItemCondition(new ItemStack(material));
            } else {
                Map<String, ProgressCondition> conditions = progressService.getAllConditions();
                if (conditions.containsKey(itemId.toLowerCase())) {
                    condition = conditions.get(itemId.toLowerCase());
                }
            }
            
            if (condition instanceof CompositeCondition) {
                CompositeCondition composite = (CompositeCondition) condition;
                for (ProgressCondition subCondition : composite.getConditions()) {
                    if (matchesSubCondition(subCondition, conditionType, target)) {
                        if (property.equalsIgnoreCase("progress")) {
                            return String.valueOf(getConditionProgress(player, subCondition));
                        } else if (property.equalsIgnoreCase("amount")) {
                            return String.valueOf(subCondition.getRequiredProgress());
                        }
                    }
                }
            }
        }
        
        // Legacy support for old placeholders
        // Pattern: progress_[conditionType]_[key]
        // Example: %prog_progress_kills_zombie% - shows player's zombie kill count
        if (identifier.startsWith("progress_")) {
            String[] partsProgress = identifier.split("_", 4);
            
            if (partsProgress.length >= 3) {
                String conditionType = partsProgress[1];
                String key = partsProgress[2];
                
                // For multi-part keys (e.g. "zombie_pigman")
                if (partsProgress.length > 3) {
                    StringBuilder keyBuilder = new StringBuilder(key);
                    for (int i = 3; i < partsProgress.length; i++) {
                        keyBuilder.append("_").append(partsProgress[i]);
                    }
                    key = keyBuilder.toString();
                }
                
                // Get the progress for this condition
                int progress = getProgress(player, conditionType, key);
                return String.valueOf(progress);
            }
        }
        
        // Legacy support
        // Pattern: unlocked_[material]
        // Example: %prog_unlocked_diamond_sword% - returns "yes" or "no"
        if (identifier.startsWith("unlocked_")) {
            String itemName = identifier.substring(9).toUpperCase();
            
            try {
                Material material = Material.valueOf(itemName);
                ItemStack item = new ItemStack(material);
                
                ProgressCondition condition = progressService.getItemCondition(item);
                if (condition == null) {
                    return "yes"; // Not tracked, so yes it's unlocked
                }
                
                return progressService.meetsCondition(player, condition) ? "yes" : "no";
            } catch (IllegalArgumentException e) {
                return "invalid";
            }
        }
        
        // Legacy support
        // Pattern: required_[material]
        // Example: %prog_required_diamond_sword% - returns required amount
        if (identifier.startsWith("required_")) {
            String itemName = identifier.substring(9).toUpperCase();
            
            try {
                Material material = Material.valueOf(itemName);
                ItemStack item = new ItemStack(material);
                
                ProgressCondition condition = progressService.getItemCondition(item);
                if (condition == null) {
                    return "0"; // Not tracked
                }
                
                return String.valueOf(condition.getRequiredProgress());
            } catch (IllegalArgumentException e) {
                return "0";
            }
        }
        
        // New item-based placeholders
        // Format: %prog_item_id_property%
        // Example: %prog_diamond_sword_progress%
        String[] partsSimple = identifier.split("_");
        if (partsSimple.length >= 2) {
            // Last part is the property, everything else is the item ID
            String property = partsSimple[partsSimple.length - 1];
            StringBuilder itemIdBuilder = new StringBuilder(partsSimple[0]);
            
            for (int i = 1; i < partsSimple.length - 1; i++) {
                itemIdBuilder.append("_").append(partsSimple[i]);
            }
            
            String itemId = itemIdBuilder.toString();
            Material material;
            
            try {
                material = Material.valueOf(itemId.toUpperCase());
            } catch (IllegalArgumentException e) {
                // This could be a custom item ID from the config, not a Bukkit Material
                material = null;
            }
            
            // Try to get the condition for this item
            ProgressCondition condition = null;
            if (material != null) {
                condition = progressService.getItemCondition(new ItemStack(material));
            } else {
                // Check if we have a condition by this name in the config
                Map<String, ProgressCondition> conditions = progressService.getAllConditions();
                if (conditions.containsKey(itemId.toLowerCase())) {
                    condition = conditions.get(itemId.toLowerCase());
                }
            }
            
            if (condition != null) {
                return handleItemPlaceholder(player, condition, property, itemId);
            }
        }
        
        return null;
    }
    
    /**
     * Handle an item-specific placeholder
     * 
     * @param player The player
     * @param condition The condition for the item
     * @param property The property being requested
     * @param itemId The item ID
     * @return The placeholder value
     */
    private String handleItemPlaceholder(Player player, ProgressCondition condition, String property, String itemId) {
        switch (property.toLowerCase()) {
            case "type":
                return getConditionType(condition);
                
            case "progress":
                return String.valueOf(getItemProgress(player, condition));
                
            case "amount":
                return String.valueOf(condition.getRequiredProgress());
                
            case "percentage":
                int progress = getItemProgress(player, condition);
                int required = condition.getRequiredProgress();
                if (required <= 0) return "100";
                return String.valueOf(Math.min(100, (progress * 100) / required));
                
            case "unlocked":
                return progressService.meetsCondition(player, condition) ? "yes" : "no";
                
            case "locked":
                return progressService.meetsCondition(player, condition) ? "no" : "yes";
                
            case "entity":
                if (condition instanceof KillsCondition) {
                    return ((KillsCondition) condition).getEntityType().toString().toLowerCase();
                }
                // Handle composite condition
                if (condition instanceof CompositeCondition) {
                    CompositeCondition composite = (CompositeCondition) condition;
                    for (ProgressCondition subCondition : composite.getConditions()) {
                        if (subCondition instanceof KillsCondition) {
                            return ((KillsCondition) subCondition).getEntityType().toString().toLowerCase();
                        }
                    }
                }
                return "";
                
            case "material":
                if (condition instanceof CollectCondition) {
                    return ((CollectCondition) condition).getMaterialType().toString().toLowerCase();
                }
                if (condition instanceof BreakCondition) {
                    return ((BreakCondition) condition).getMaterialType().toString().toLowerCase();
                }
                // Handle composite condition
                if (condition instanceof CompositeCondition) {
                    CompositeCondition composite = (CompositeCondition) condition;
                    for (ProgressCondition subCondition : composite.getConditions()) {
                        if (subCondition instanceof CollectCondition) {
                            return ((CollectCondition) subCondition).getMaterialType().toString().toLowerCase();
                        }
                        if (subCondition instanceof BreakCondition) {
                            return ((BreakCondition) subCondition).getMaterialType().toString().toLowerCase();
                        }
                    }
                }
                return "";
                
            case "placeholder":
                if (condition instanceof PlaceholderCondition) {
                    return ((PlaceholderCondition) condition).getPlaceholder();
                }
                // Handle composite condition
                if (condition instanceof CompositeCondition) {
                    CompositeCondition composite = (CompositeCondition) condition;
                    for (ProgressCondition subCondition : composite.getConditions()) {
                        if (subCondition instanceof PlaceholderCondition) {
                            return ((PlaceholderCondition) subCondition).getPlaceholder();
                        }
                    }
                }
                return "";
                
            default:
                return "";
        }
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
        if (condition instanceof PlaceholderCondition) return "placeholder";
        if (condition instanceof PrerequisiteCondition) return "prerequisite";
        if (condition instanceof CompositeCondition) {
            // For composite, return the most relevant sub-condition type
            CompositeCondition composite = (CompositeCondition) condition;
            for (ProgressCondition subCondition : composite.getConditions()) {
                if (!(subCondition instanceof PrerequisiteCondition)) {
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
                if (!(subCondition instanceof PrerequisiteCondition)) {
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
            return getProgress(player, "kills", killsCondition.getEntityType().toString().toLowerCase());
        }
        
        if (condition instanceof CollectCondition) {
            CollectCondition collectCondition = (CollectCondition) condition;
            return getProgress(player, "collect", collectCondition.getMaterialType().toString().toLowerCase());
        }
        
        if (condition instanceof BreakCondition) {
            BreakCondition breakCondition = (BreakCondition) condition;
            return getProgress(player, "break", breakCondition.getMaterialType().toString().toLowerCase());
        }
        
        if (condition instanceof PlaceholderCondition) {
            PlaceholderCondition placeholderCondition = (PlaceholderCondition) condition;
            return getProgress(player, "placeholder", placeholderCondition.getPlaceholder());
        }
        
        return 0;
    }
    
    /**
     * Get the progress for a specific condition type and key
     * 
     * @param player The player
     * @param conditionType The condition type
     * @param key The condition key
     * @return The progress value
     */
    private int getProgress(Player player, String conditionType, String key) {
        // Get the progress directly from the data manager
        if (dataManager != null) {
            return dataManager.getProgress(player.getUniqueId(), conditionType, key);
        }
        
        return 0;
    }
    
    /**
     * Check if a sub-condition matches the type and target
     * 
     * @param subCondition The condition to check
     * @param conditionType The condition type to match
     * @param target The target (material, entity, etc.) to match
     * @return true if this condition matches the type and target
     */
    private boolean matchesSubCondition(ProgressCondition subCondition, String conditionType, String target) {
        switch (conditionType.toLowerCase()) {
            case "collect":
                if (subCondition instanceof CollectCondition) {
                    return ((CollectCondition) subCondition).getMaterialType().toString().toLowerCase().equals(target.toLowerCase());
                }
                break;
            case "break":
                if (subCondition instanceof BreakCondition) {
                    return ((BreakCondition) subCondition).getMaterialType().toString().toLowerCase().equals(target.toLowerCase());
                }
                break;
            case "kills":
                if (subCondition instanceof KillsCondition) {
                    return ((KillsCondition) subCondition).getEntityType().toString().toLowerCase().equals(target.toLowerCase());
                }
                break;
            case "placeholder":
                if (subCondition instanceof PlaceholderCondition) {
                    return ((PlaceholderCondition) subCondition).getPlaceholder().equals(target);
                }
                break;
            case "prerequisite":
                if (subCondition instanceof PrerequisiteCondition) {
                    return ((PrerequisiteCondition) subCondition).getPrerequisiteItem().toString().toLowerCase().equals(target.toLowerCase());
                }
                break;
        }
        return false;
    }
} 