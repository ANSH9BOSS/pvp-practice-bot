package com.ansh9boss.pvpbot;

import com.ansh9boss.pvpbot.commands.PlayerCommandExecutor;
import com.ansh9boss.pvpbot.commands.AdminCommandExecutor;
import com.ansh9boss.pvpbot.config.ConfigManager;
import com.ansh9boss.pvpbot.listeners.BotListener;
import com.ansh9boss.pvpbot.npc.BotManager;
import com.ansh9boss.pvpbot.updater.Updater;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PvPPracticeBot extends JavaPlugin {

    private static PvPPracticeBot instance;
    private ConfigManager configManager;
    private BotManager botManager;
    private Updater updater;
    private boolean citizensEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize Config
        this.saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        
        // Initialize Updater
        this.updater = new Updater(this);

        // Check for Citizens
        if (Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            this.citizensEnabled = true;
            this.getLogger().info("Citizens found and hooked successfully!");
        } else {
            this.getLogger().warning("Citizens was not found! PvP Bot commands related to spawning will be disabled.");
        }

        // Initialize Bot Manager (passes citizens status)
        this.botManager = new BotManager(this);

        // Register Event Listeners
        Bukkit.getPluginManager().registerEvents(new BotListener(this), this);

        // Register Command Executors
        if (this.getCommand("pvpbot") != null) {
            this.getCommand("pvpbot").setExecutor(new PlayerCommandExecutor(this));
            this.getCommand("pvpbot").setTabCompleter(new PlayerCommandExecutor(this));
        }
        if (this.getCommand("pvpbotadmin") != null) {
            this.getCommand("pvpbotadmin").setExecutor(new AdminCommandExecutor(this));
            this.getCommand("pvpbotadmin").setTabCompleter(new AdminCommandExecutor(this));
        }

        this.getLogger().info("PvPPracticeBot v" + this.getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        // Clean up all active bots to prevent orphan NPCs
        if (this.botManager != null) {
            this.botManager.removeAllBots();
        }
        this.getLogger().info("PvPPracticeBot has been disabled and all bots removed.");
    }

    public static PvPPracticeBot getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public boolean isCitizensEnabled() {
        return citizensEnabled;
    }

    public Updater getUpdater() {
        return updater;
    }
}
