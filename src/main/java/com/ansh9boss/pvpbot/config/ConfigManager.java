package com.ansh9boss.pvpbot.config;

import com.ansh9boss.pvpbot.PvPPracticeBot;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class ConfigManager {

    private final PvPPracticeBot plugin;
    private int defaultDifficulty;
    private int defaultMaxBots;
    private int spawnCooldown;
    private String githubRepo;
    private String githubToken;

    public ConfigManager(PvPPracticeBot plugin) {
        this.plugin = plugin;
        this.loadValues();
    }

    public void reloadConfig() {
        this.plugin.reloadConfig();
        this.loadValues();
    }

    private void loadValues() {
        FileConfiguration config = this.plugin.getConfig();
        this.defaultDifficulty = config.getInt("default-difficulty", 3);
        this.defaultMaxBots = config.getInt("max-bots-per-player", 1);
        this.spawnCooldown = config.getInt("spawn-cooldown", 10);
        this.githubRepo = config.getString("github-repo", "ANSH9BOSS/pvp-practice-bot");
        this.githubToken = config.getString("github-token", "");

        // Validate values
        if (this.defaultDifficulty < 1 || this.defaultDifficulty > 5) {
            this.defaultDifficulty = 3;
        }
        if (this.defaultMaxBots < 1) {
            this.defaultMaxBots = 1;
        }
        if (this.spawnCooldown < 0) {
            this.spawnCooldown = 0;
        }
    }

    public int getDefaultDifficulty() {
        return defaultDifficulty;
    }

    public int getSpawnCooldown() {
        return spawnCooldown;
    }

    public void setSpawnCooldown(int seconds) {
        this.spawnCooldown = Math.max(0, seconds);
        this.plugin.getConfig().set("spawn-cooldown", this.spawnCooldown);
        this.plugin.saveConfig();
    }

    public String getGithubRepo() {
        return githubRepo;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public int getMaxBots(UUID uuid) {
        FileConfiguration config = this.plugin.getConfig();
        String path = "player-max-bots." + uuid.toString();
        if (config.contains(path)) {
            return config.getInt(path);
        }
        return defaultMaxBots;
    }

    public void setMaxBots(UUID uuid, int amount) {
        int finalAmount = Math.max(1, amount);
        this.plugin.getConfig().set("player-max-bots." + uuid.toString(), finalAmount);
        this.plugin.saveConfig();
    }
}
