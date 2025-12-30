package com.treecapitator;

import org.bukkit.plugin.java.JavaPlugin;

public class TreeCapitator extends JavaPlugin {

    private static TreeCapitator instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new TreeBreakListener(this), this);
        getLogger().info("TreeCapitator has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TreeCapitator has been disabled!");
    }

    public static TreeCapitator getInstance() {
        return instance;
    }
}
