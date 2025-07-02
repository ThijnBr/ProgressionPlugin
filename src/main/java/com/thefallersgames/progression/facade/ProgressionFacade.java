package com.thefallersgames.progression.facade;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.thefallersgames.progression.api.condition.ProgressCondition;
import com.thefallersgames.progression.api.event.ItemUnlockEvent;
import com.thefallersgames.progression.api.facade.UnlockFacade;
import com.thefallersgames.progression.api.service.ProgressService;
import com.thefallersgames.progression.util.ItemUtil;

/**
 * Implementation of UnlockFacade that simplifies interaction with the progression system.
 */
public class ProgressionFacade implements UnlockFacade {
    
    private final ProgressService progressService;
    
    public ProgressionFacade(ProgressService progressService) {
        this.progressService = progressService;
    }
    
    @Override
    public boolean canUseItem(Player player, ItemStack item) {
        if (player.hasPermission("progression.bypass")) {
            return true;
        }
        
        if (!isTrackedItem(item)) {
            return true;
        }
        
        return !progressService.isItemLocked(player, item);
    }
    
    @Override
    public boolean handleItemUse(Player player, ItemStack item) {
        if (!isTrackedItem(item)) {
            return true;
        }
        
        if (canUseItem(player, item)) {
            // Player can use the item
            return true;
        } else {
            // Item is locked, send message
            player.sendMessage(ChatColor.RED + progressService.getItemLockMessage(player, item));
            
            // Cancel the action
            return false;
        }
    }
    
    @Override
    public boolean isTrackedItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        String itemId = ItemUtil.getItemId(item);
        return progressService.getItemCondition(item) != null;
    }
    
    @Override
    public String getItemStatusMessage(Player player, ItemStack item) {
        if (!isTrackedItem(item)) {
            return ChatColor.GREEN + "This item is not restricted.";
        }
        
        ProgressCondition condition = progressService.getItemCondition(item);
        
        if (condition == null) {
            return ChatColor.GREEN + "This item is not restricted.";
        }
        
        if (progressService.meetsCondition(player, condition)) {
            return ChatColor.GREEN + "You have unlocked this item! (" + 
                   condition.getCurrentProgress(player) + "/" + condition.getRequiredProgress() + ")";
        } else {
            return ChatColor.RED + progressService.getItemLockMessage(player, item);
        }
    }
} 