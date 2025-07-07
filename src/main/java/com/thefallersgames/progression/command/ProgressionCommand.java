package com.thefallersgames.progression.command;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.thefallersgames.progression.Progression;
import com.thefallersgames.progression.api.facade.UnlockFacade;
import com.thefallersgames.progression.api.service.ProgressService;
import com.thefallersgames.progression.data.PlayerDataManager;
import com.thefallersgames.progression.util.ItemUtil;

/**
 * Command handler for the plugin's commands.
 */
public class ProgressionCommand implements CommandExecutor {
    
    private final Progression plugin;
    private final ProgressService progressService;
    private final UnlockFacade unlockFacade;
    private final PlayerDataManager playerDataManager;
    
    /**
     * Create a new ProgressionCommand
     * 
     * @param plugin The plugin instance
     * @param progressService The progress service
     * @param unlockFacade The unlock facade
     */
    public ProgressionCommand(Progression plugin, ProgressService progressService, UnlockFacade unlockFacade) {
        this.plugin = plugin;
        this.progressService = progressService;
        this.unlockFacade = unlockFacade;
        this.playerDataManager = plugin.getPlayerDataManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "status":
                return handleStatusCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            case "unlock":
                return handleUnlockCommand(sender, args);
            case "lock":
                return handleLockCommand(sender, args);
            case "reset":
                return handleResetCommand(sender, args);
            case "testitem":
                return handleTestItemCommand(sender, args);
            default:
                showHelp(sender);
                return true;
        }
    }
    
    /**
     * Handle the status command
     * 
     * @param sender The command sender
     * @param args Command arguments
     * @return true if handled, false otherwise
     */
    private boolean handleStatusCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("progression.view")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to check progression status");
            return true;
        }
        
        // Check status of held item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        
        if (heldItem == null || heldItem.getType().isAir()) {
            player.sendMessage(ChatColor.YELLOW + "Hold an item to check its status");
            return true;
        }
        
        String statusMessage = unlockFacade.getItemStatusMessage(player, heldItem);
        player.sendMessage(statusMessage);
        
        return true;
    }
    
    /**
     * Handle the reload command
     * 
     * @param sender The command sender
     * @return true if handled, false otherwise
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("progression.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin");
            return true;
        }
        
        // Reload plugin configuration
        plugin.reloadPluginConfig();
        
        sender.sendMessage(ChatColor.GREEN + "Progression configuration reloaded");
        return true;
    }
    
    /**
     * Handle the unlock command
     * 
     * @param sender The command sender
     * @param args Command arguments
     * @return true if handled, false otherwise
     */
    private boolean handleUnlockCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("progression.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to unlock items");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /prog unlock <player/all> <itemId>");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog unlock JohnDoe diamond_sword");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog unlock all wooden_pickaxe");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog unlock JohnDoe repairtoken:repair_token");
            return true;
        }
        
        String playerArg = args[1];
        String itemId = args[2].toLowerCase();
        
        // Get the condition for this item
        com.thefallersgames.progression.api.condition.ProgressCondition condition = null;
        
        try {
            // First try to get directly from item conditions map
            condition = progressService.getAllConditions().get(itemId);
            
            // If not found directly, try to create an item and check
            if (condition == null) {
                Material material = Material.valueOf(itemId.toUpperCase());
                ItemStack item = new ItemStack(material);
                condition = progressService.getItemCondition(item);
            }
        } catch (Exception e) {
            // Ignore, we'll check if condition is null below
        }
        
        if (condition == null) {
            sender.sendMessage(ChatColor.RED + "No progression requirements found for item: " + itemId);
            return true;
        }
        
        // Use the condition's required progress amount
        int amount = condition.getRequiredProgress();
        
        // Determine condition type and target based on the condition
        String conditionType = "";
        String target = "";
        
        if (condition instanceof com.thefallersgames.progression.condition.KillsCondition) {
            com.thefallersgames.progression.condition.KillsCondition killsCondition = 
                (com.thefallersgames.progression.condition.KillsCondition) condition;
            conditionType = "kills";
            target = killsCondition.getEntityName();
        } else if (condition instanceof com.thefallersgames.progression.condition.CollectCondition) {
            com.thefallersgames.progression.condition.CollectCondition collectCondition = 
                (com.thefallersgames.progression.condition.CollectCondition) condition;
            conditionType = "collect";
            target = collectCondition.getMaterialName();
        } else if (condition instanceof com.thefallersgames.progression.condition.BreakCondition) {
            com.thefallersgames.progression.condition.BreakCondition breakCondition = 
                (com.thefallersgames.progression.condition.BreakCondition) condition;
            conditionType = "break";
            target = breakCondition.getMaterialName();
        } else if (condition instanceof com.thefallersgames.progression.condition.PlaceholderCondition) {
            com.thefallersgames.progression.condition.PlaceholderCondition placeholderCondition = 
                (com.thefallersgames.progression.condition.PlaceholderCondition) condition;
            conditionType = "placeholder";
            target = placeholderCondition.getPlaceholder();
        } else if (condition instanceof com.thefallersgames.progression.condition.CompositeCondition) {
            // For composite conditions, find the main non-prerequisite condition
            com.thefallersgames.progression.condition.CompositeCondition composite = 
                (com.thefallersgames.progression.condition.CompositeCondition) condition;
            
            for (com.thefallersgames.progression.api.condition.ProgressCondition subCondition : composite.getConditions()) {
                if (!(subCondition instanceof com.thefallersgames.progression.condition.PrerequisiteCondition)) {
                    if (subCondition instanceof com.thefallersgames.progression.condition.KillsCondition) {
                        com.thefallersgames.progression.condition.KillsCondition killsCondition = 
                            (com.thefallersgames.progression.condition.KillsCondition) subCondition;
                        conditionType = "kills";
                        target = killsCondition.getEntityName();
                        break;
                    } else if (subCondition instanceof com.thefallersgames.progression.condition.CollectCondition) {
                        com.thefallersgames.progression.condition.CollectCondition collectCondition = 
                            (com.thefallersgames.progression.condition.CollectCondition) subCondition;
                        conditionType = "collect";
                        target = collectCondition.getMaterialName();
                        break;
                    } else if (subCondition instanceof com.thefallersgames.progression.condition.BreakCondition) {
                        com.thefallersgames.progression.condition.BreakCondition breakCondition = 
                            (com.thefallersgames.progression.condition.BreakCondition) subCondition;
                        conditionType = "break";
                        target = breakCondition.getMaterialName();
                        break;
                    }
                }
            }
        }
        
        if (conditionType.isEmpty() || target.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Could not determine condition type for item: " + itemId);
            return true;
        }
        
        // Now we can use the same logic as before but with the extracted condition type and target
        if (playerArg.equalsIgnoreCase("all")) {
            // Unlock for all online players
            int playerCount = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerDataManager.setProgress(player.getUniqueId(), conditionType, target, amount);
                playerCount++;
            }
            
            sender.sendMessage(ChatColor.GREEN + "Unlocked " + ChatColor.YELLOW + itemId + 
                               ChatColor.GREEN + " for " + playerCount + " online players");
        } else {
            // Unlock for a specific player
            Player player = Bukkit.getPlayerExact(playerArg);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + playerArg);
                return true;
            }
            
            playerDataManager.setProgress(player.getUniqueId(), conditionType, target, amount);
            sender.sendMessage(ChatColor.GREEN + "Unlocked " + ChatColor.YELLOW + itemId + 
                               ChatColor.GREEN + " for player " + player.getName());
            
            // Notify the player
            player.sendMessage(ChatColor.GREEN + "You've unlocked " + ChatColor.YELLOW + itemId + ChatColor.GREEN + "!");
        }
        
        return true;
    }
    
    /**
     * Handle the lock command
     * 
     * @param sender The command sender
     * @param args Command arguments
     * @return true if handled, false otherwise
     */
    private boolean handleLockCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("progression.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to lock items");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /prog lock <player/all> <itemId>");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog lock JohnDoe diamond_sword");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog lock all wooden_pickaxe");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog lock JohnDoe repairtoken:repair_token");
            return true;
        }
        
        String playerArg = args[1];
        String itemId = args[2].toLowerCase();
        
        // Get the condition for this item
        com.thefallersgames.progression.api.condition.ProgressCondition condition = null;
        
        try {
            // First try to get directly from item conditions map
            condition = progressService.getAllConditions().get(itemId);
            
            // If not found directly, try to create an item and check
            if (condition == null) {
                Material material = Material.valueOf(itemId.toUpperCase());
                ItemStack item = new ItemStack(material);
                condition = progressService.getItemCondition(item);
            }
        } catch (Exception e) {
            // Ignore, we'll check if condition is null below
        }
        
        if (condition == null) {
            sender.sendMessage(ChatColor.RED + "No progression requirements found for item: " + itemId);
            return true;
        }
        
        // Set progress to 0
        int amount = 0;
        
        // Determine condition type and target based on the condition
        String conditionType = "";
        String target = "";
        
        if (condition instanceof com.thefallersgames.progression.condition.KillsCondition) {
            com.thefallersgames.progression.condition.KillsCondition killsCondition = 
                (com.thefallersgames.progression.condition.KillsCondition) condition;
            conditionType = "kills";
            target = killsCondition.getEntityName();
        } else if (condition instanceof com.thefallersgames.progression.condition.CollectCondition) {
            com.thefallersgames.progression.condition.CollectCondition collectCondition = 
                (com.thefallersgames.progression.condition.CollectCondition) condition;
            conditionType = "collect";
            target = collectCondition.getMaterialName();
        } else if (condition instanceof com.thefallersgames.progression.condition.BreakCondition) {
            com.thefallersgames.progression.condition.BreakCondition breakCondition = 
                (com.thefallersgames.progression.condition.BreakCondition) condition;
            conditionType = "break";
            target = breakCondition.getMaterialName();
        } else if (condition instanceof com.thefallersgames.progression.condition.PlaceholderCondition) {
            com.thefallersgames.progression.condition.PlaceholderCondition placeholderCondition = 
                (com.thefallersgames.progression.condition.PlaceholderCondition) condition;
            conditionType = "placeholder";
            target = placeholderCondition.getPlaceholder();
        } else if (condition instanceof com.thefallersgames.progression.condition.CompositeCondition) {
            // For composite conditions, find the main non-prerequisite condition
            com.thefallersgames.progression.condition.CompositeCondition composite = 
                (com.thefallersgames.progression.condition.CompositeCondition) condition;
            
            for (com.thefallersgames.progression.api.condition.ProgressCondition subCondition : composite.getConditions()) {
                if (!(subCondition instanceof com.thefallersgames.progression.condition.PrerequisiteCondition)) {
                    if (subCondition instanceof com.thefallersgames.progression.condition.KillsCondition) {
                        com.thefallersgames.progression.condition.KillsCondition killsCondition = 
                            (com.thefallersgames.progression.condition.KillsCondition) subCondition;
                        conditionType = "kills";
                        target = killsCondition.getEntityName();
                        break;
                    } else if (subCondition instanceof com.thefallersgames.progression.condition.CollectCondition) {
                        com.thefallersgames.progression.condition.CollectCondition collectCondition = 
                            (com.thefallersgames.progression.condition.CollectCondition) subCondition;
                        conditionType = "collect";
                        target = collectCondition.getMaterialName();
                        break;
                    } else if (subCondition instanceof com.thefallersgames.progression.condition.BreakCondition) {
                        com.thefallersgames.progression.condition.BreakCondition breakCondition = 
                            (com.thefallersgames.progression.condition.BreakCondition) subCondition;
                        conditionType = "break";
                        target = breakCondition.getMaterialName();
                        break;
                    }
                }
            }
        }
        
        if (conditionType.isEmpty() || target.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Could not determine condition type for item: " + itemId);
            return true;
        }
        
        // Now we can use the same logic as before but with the extracted condition type and target
        if (playerArg.equalsIgnoreCase("all")) {
            // Lock for all online players
            int playerCount = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerDataManager.setProgress(player.getUniqueId(), conditionType, target, amount);
                playerCount++;
            }
            
            sender.sendMessage(ChatColor.GREEN + "Locked " + ChatColor.YELLOW + itemId + 
                               ChatColor.GREEN + " for " + playerCount + " online players");
        } else {
            // Lock for a specific player
            Player player = Bukkit.getPlayerExact(playerArg);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + playerArg);
                return true;
            }
            
            playerDataManager.setProgress(player.getUniqueId(), conditionType, target, amount);
            sender.sendMessage(ChatColor.GREEN + "Locked " + ChatColor.YELLOW + itemId + 
                               ChatColor.GREEN + " for player " + player.getName());
            
            // Notify the player
            player.sendMessage(ChatColor.RED + "Your item " + ChatColor.YELLOW + itemId + 
                              ChatColor.RED + " has been locked!");
        }
        
        return true;
    }
    
    /**
     * Handle the reset command
     * 
     * @param sender The command sender
     * @param args Command arguments
     * @return true if handled, false otherwise
     */
    private boolean handleResetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("progression.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reset progressions");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /prog reset <player/all>");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog reset JohnDoe");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog reset all");
            return true;
        }
        
        String playerArg = args[1];
        
        if (playerArg.equalsIgnoreCase("all")) {
            // Confirm this is a drastic action
            if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                sender.sendMessage(ChatColor.RED + "WARNING: This will reset ALL progression data for ALL players!");
                sender.sendMessage(ChatColor.RED + "To confirm, use: /prog reset all confirm");
                return true;
            }
            
            // Reset for all players
            for (Player player : Bukkit.getOnlinePlayers()) {
                resetPlayerData(player.getUniqueId());
                
                // Notify the player
                player.sendMessage(ChatColor.RED + "Your progression data has been completely reset by an admin!");
            }
            
            sender.sendMessage(ChatColor.GREEN + "Reset progression data for all online players");
        } else {
            // Reset for a specific player
            Player player = Bukkit.getPlayerExact(playerArg);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + playerArg);
                return true;
            }
            
            resetPlayerData(player.getUniqueId());
            
            sender.sendMessage(ChatColor.GREEN + "Reset progression data for player " + player.getName());
            
            // Notify the player
            player.sendMessage(ChatColor.RED + "Your progression data has been completely reset by an admin!");
        }
        
        return true;
    }
    
    /**
     * Reset all progression data for a player
     * 
     * @param playerId The player's UUID
     */
    private void resetPlayerData(UUID playerId) {
        // Remove the player from the data manager's memory cache
        playerDataManager.clearPlayerData(playerId);
        
        // Reload empty data
        playerDataManager.loadPlayerData(playerId);
    }
    
    /**
     * Handle the testitem command to test custom items with ItemModel
     * 
     * @param sender The command sender
     * @param args Command arguments
     * @return true if handled, false otherwise
     */
    private boolean handleTestItemCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("progression.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
            return true;
        }
        
        // Get the item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You need to hold an item to test");
            return true;
        }
        
        // Simplified output with only essential information
        sender.sendMessage(ChatColor.GOLD + "Item Detection Test:");
        
        String itemId = ItemUtil.getItemId(item);
        
        try {
            if (item.hasItemMeta() && item.getItemMeta().hasItemModel()) {
                sender.sendMessage(ChatColor.GREEN + "Detected custom item: " + itemId);
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Detected vanilla item: " + itemId);
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error detecting item type");
        }
        
        // Only show if this item has a progression requirement
        if (plugin.getProgressService().getItemCondition(item) != null) {
            sender.sendMessage(ChatColor.GREEN + "This item has progression requirements");
        }
        
        return true;
    }
    
    /**
     * Show the help message
     * 
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Progression Commands ===");
        
        // Basic commands available to all players with progression.view
        if (sender.hasPermission("progression.view")) {
            sender.sendMessage(ChatColor.YELLOW + "/prog status " + 
                              ChatColor.WHITE + "- Check the status of the item you're currently holding");
        }
        
        // Reload command for administrators
        if (sender.hasPermission("progression.reload")) {
            sender.sendMessage(ChatColor.YELLOW + "/prog reload " + 
                              ChatColor.WHITE + "- Reload the plugin configuration");
        }
        
        // Administrative commands
        if (sender.hasPermission("progression.admin")) {
            sender.sendMessage(ChatColor.GOLD + "=== Admin Commands ===");
            
            sender.sendMessage(ChatColor.YELLOW + "/prog unlock <player/all> <itemId> " + 
                              ChatColor.WHITE + "- Set progression for player(s)");
            
            sender.sendMessage(ChatColor.YELLOW + "/prog lock <player/all> <itemId> " + 
                              ChatColor.WHITE + "- Reset progression to 0 for player(s)");
            
            sender.sendMessage(ChatColor.YELLOW + "/prog reset <player/all> " + 
                              ChatColor.WHITE + "- Reset all progression data for player(s)");
            
            sender.sendMessage(ChatColor.YELLOW + "/prog testitem " + 
                              ChatColor.WHITE + "- Test custom item detection (1.21.5+ ItemModel API)");
        }
        
        // Add a hint about permissions if the player doesn't have admin access
        if (!sender.hasPermission("progression.admin") && sender instanceof Player) {
            sender.sendMessage(ChatColor.GRAY + "Additional commands are available to administrators.");
        }
    }
} 