package com.thefallersgames.progression.adapter;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import com.thefallersgames.progression.api.adapter.PlaceholderAdapter;

import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Concrete adapter implementation for PlaceholderAPI.
 * This class bridges between our plugin and PlaceholderAPI.
 */
public class PlaceholderAPIAdapter implements PlaceholderAdapter {
    
    private final boolean available;
    
    public PlaceholderAPIAdapter() {
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        this.available = placeholderAPI != null && placeholderAPI.isEnabled();
    }
    
    @Override
    public String setPlaceholders(OfflinePlayer player, String text) {
        if (!available) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }
    
    @Override
    public String setPlaceholders(String text) {
        if (!available) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(null, text);
    }
    
    @Override
    public boolean isAvailable() {
        return available;
    }
} 