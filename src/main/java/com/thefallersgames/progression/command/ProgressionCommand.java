package com.thefallersgames.progression.command;

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

import java.util.UUID;

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
        if (args.length == 0) {
            // Show help message
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
            sender.sendMessage(ChatColor.RED + "You don't have permission to unlock progressions");
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /prog unlock <player/all> <conditionType> <target> [amount]");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog unlock JohnDoe kills zombie 50");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog unlock all collect apple 100");
            return true;
        }
        
        String playerArg = args[1];
        String conditionType = args[2].toLowerCase();
        String target = args[3].toLowerCase();
        
        // Default amount is large enough to satisfy any condition
        int amount = 100000;
        if (args.length > 4) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[4]);
                return true;
            }
        }
        
        if (playerArg.equalsIgnoreCase("all")) {
            // Unlock for all online players
            int playerCount = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerDataManager.setProgress(player.getUniqueId(), conditionType, target, amount);
                playerCount++;
            }
            
            sender.sendMessage(ChatColor.GREEN + "Set " + conditionType + "." + target + " = " + amount + 
                               " for " + playerCount + " online players");
        } else {
            // Unlock for a specific player
            Player player = Bukkit.getPlayerExact(playerArg);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + playerArg);
                return true;
            }
            
            playerDataManager.setProgress(player.getUniqueId(), conditionType, target, amount);
            sender.sendMessage(ChatColor.GREEN + "Set " + conditionType + "." + target + " = " + amount + 
                              " for player " + player.getName());
            
            // Notify the player
            player.sendMessage(ChatColor.GREEN + "You've unlocked " + target + " in the " + conditionType + " category!");
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
            sender.sendMessage(ChatColor.RED + "You don't have permission to lock progressions");
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /prog lock <player/all> <conditionType> <target>");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog lock JohnDoe kills zombie");
            sender.sendMessage(ChatColor.YELLOW + "Example: /prog lock all collect apple");
            return true;
        }
        
        String playerArg = args[1];
        String conditionType = args[2].toLowerCase();
        String target = args[3].toLowerCase();
        
        // Set progress to 0
        int amount = 0;
        
        if (playerArg.equalsIgnoreCase("all")) {
            // Lock for all online players
            int playerCount = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerDataManager.setProgress(player.getUniqueId(), conditionType, target, amount);
                playerCount++;
            }
            
            sender.sendMessage(ChatColor.GREEN + "Set " + conditionType + "." + target + " = " + amount + 
                               " for " + playerCount + " online players");
        } else {
            // Lock for a specific player
            Player player = Bukkit.getPlayerExact(playerArg);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + playerArg);
                return true;
            }
            
            playerDataManager.setProgress(player.getUniqueId(), conditionType, target, amount);
            sender.sendMessage(ChatColor.GREEN + "Set " + conditionType + "." + target + " = " + amount + 
                              " for player " + player.getName());
            
            // Notify the player
            player.sendMessage(ChatColor.RED + "Your progress for " + target + " in the " + conditionType + 
                              " category has been reset!");
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
     * Show the help message
     * 
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Progression Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/prog status " + ChatColor.WHITE + "- Check the status of the held item");
        
        if (sender.hasPermission("progression.reload")) {
            sender.sendMessage(ChatColor.YELLOW + "/prog reload " + ChatColor.WHITE + "- Reload the plugin configuration");
        }
        
        if (sender.hasPermission("progression.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/prog unlock <player/all> <type> <target> [amount] " + 
                              ChatColor.WHITE + "- Unlock progression for player(s)");
            sender.sendMessage(ChatColor.YELLOW + "/prog lock <player/all> <type> <target> " + 
                              ChatColor.WHITE + "- Lock progression for player(s)");
            sender.sendMessage(ChatColor.YELLOW + "/prog reset <player/all> " + 
                              ChatColor.WHITE + "- Reset all progression for player(s)");
        }
    }
} 