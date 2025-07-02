package com.thefallersgames.progression.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Event fired when a player unlocks an item.
 * This follows the Event-Driven pattern recommended in the design principles.
 */
public class ItemUnlockEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final ItemStack item;
    private final String conditionType;
    
    /**
     * Create a new ItemUnlockEvent
     * 
     * @param player The player who unlocked the item
     * @param item The item that was unlocked
     * @param conditionType The type of condition that was met
     */
    public ItemUnlockEvent(Player player, ItemStack item, String conditionType) {
        this.player = player;
        this.item = item;
        this.conditionType = conditionType;
    }
    
    /**
     * Get the player who unlocked the item
     * 
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Get the item that was unlocked
     * 
     * @return The item
     */
    public ItemStack getItem() {
        return item;
    }
    
    /**
     * Get the type of condition that was met
     * 
     * @return The condition type
     */
    public String getConditionType() {
        return conditionType;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
} 