package com.thefallersgames.progression.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import com.thefallersgames.progression.data.PlayerDataManager;

/**
 * Listener for player events to handle loading and saving player data.
 */
public class PlayerListener implements Listener {
    
    private final PlayerDataManager dataManager;
    private final InventoryListener inventoryListener;
    private final Plugin plugin;
    
    /**
     * Create a new PlayerListener
     * 
     * @param dataManager The player data manager
     * @param inventoryListener The inventory listener for checking locked items
     * @param plugin The plugin instance for scheduling tasks
     */
    public PlayerListener(PlayerDataManager dataManager, InventoryListener inventoryListener, Plugin plugin) {
        this.dataManager = dataManager;
        this.inventoryListener = inventoryListener;
        this.plugin = plugin;
    }
    
    /**
     * Handle player join events
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player progression data
        dataManager.loadPlayerData(player.getUniqueId());
        
        // Schedule a task to check inventory for locked items after data is loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                inventoryListener.dropLockedItems(player);
            }
        }.runTaskLater(plugin, 10L); // Slightly longer delay to ensure data is loaded
    }
    
    /**
     * Handle player quit events
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save player progression data
        dataManager.savePlayerData(player.getUniqueId());
    }
} 