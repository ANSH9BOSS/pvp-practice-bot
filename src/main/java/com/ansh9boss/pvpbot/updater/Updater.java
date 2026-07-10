package com.ansh9boss.pvpbot.updater;

import com.ansh9boss.pvpbot.PvPPracticeBot;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public class Updater {

    private final PvPPracticeBot plugin;
    private long pendingUpdateExpiry = 0;
    private String pendingDownloadUrl = null;
    private String pendingTagName = null;

    public Updater(PvPPracticeBot plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdate(CommandSender sender, boolean confirm) {
        // Run update logic asynchronously to avoid blocking the main server thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String repo = plugin.getConfigManager().getGithubRepo();
                String token = plugin.getConfigManager().getGithubToken();
                String currentVersion = plugin.getDescription().getVersion();

                plugin.getLogger().info("Checking for updates from repository: " + repo);
                sender.sendMessage(ChatColor.GRAY + "Checking GitHub for updates...");

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                String apiUrl = "https://api.github.com/repos/" + repo + "/releases/latest";
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("User-Agent", "PvPPracticeBot-Updater")
                        .GET();

                if (token != null && !token.trim().isEmpty()) {
                    reqBuilder.header("Authorization", "Bearer " + token.trim());
                }

                HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 404) {
                    plugin.getLogger().warning("No releases found or repository is private/invalid.");
                    sender.sendMessage(ChatColor.RED + "No updates found. Verify repo owner/name in config.yml.");
                    return;
                } else if (response.statusCode() != 200) {
                    plugin.getLogger().warning("GitHub API returned status code " + response.statusCode());
                    sender.sendMessage(ChatColor.RED + "Error checking update. GitHub API status: " + response.statusCode());
                    return;
                }

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String tagName = json.get("tag_name").getAsString();
                
                if (!isNewerVersion(currentVersion, tagName)) {
                    plugin.getLogger().info("Already running the latest version: " + currentVersion);
                    sender.sendMessage(ChatColor.GREEN + "Already running the latest version (v" + currentVersion + ").");
                    return;
                }

                // Look for the jar asset
                String jarUrl = null;
                if (json.has("assets")) {
                    JsonArray assets = json.getAsJsonArray("assets");
                    for (JsonElement element : assets) {
                        JsonObject asset = element.getAsJsonObject();
                        String name = asset.get("name").getAsString();
                        if (name.endsWith(".jar")) {
                            jarUrl = asset.get("browser_download_url").getAsString();
                            break;
                        }
                    }
                }

                if (jarUrl == null) {
                    plugin.getLogger().warning("No .jar assets found in the latest release v" + tagName);
                    sender.sendMessage(ChatColor.RED + "Found release v" + tagName + " but it contains no .jar files!");
                    return;
                }

                plugin.getLogger().info("Found newer version: " + tagName);

                if (!confirm) {
                    // Mark update as pending
                    pendingDownloadUrl = jarUrl;
                    pendingTagName = tagName;
                    pendingUpdateExpiry = System.currentTimeMillis() + 30000; // 30 seconds

                    sender.sendMessage(ChatColor.GOLD + "=============================================");
                    sender.sendMessage(ChatColor.YELLOW + "A newer version is available: " + ChatColor.GREEN + tagName);
                    sender.sendMessage(ChatColor.YELLOW + "Current running version: " + ChatColor.RED + currentVersion);
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "To download and restart the server, run within 30s:");
                    sender.sendMessage(ChatColor.AQUA + "/pvpbotadmin update confirm");
                    sender.sendMessage(ChatColor.GOLD + "=============================================");
                } else {
                    // Check if confirmation is valid
                    if (pendingDownloadUrl == null || System.currentTimeMillis() > pendingUpdateExpiry) {
                        sender.sendMessage(ChatColor.RED + "No pending update found or confirmation expired. Check again first.");
                        return;
                    }

                    // Apply update
                    applyUpdate(sender, pendingDownloadUrl, pendingTagName);
                    // Reset pending
                    pendingDownloadUrl = null;
                    pendingTagName = null;
                    pendingUpdateExpiry = 0;
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to check for updates: " + e.getMessage());
                sender.sendMessage(ChatColor.RED + "Updater encountered an exception. Check console.");
                e.printStackTrace();
            }
        });
    }

    private void applyUpdate(CommandSender sender, String downloadUrl, String tagName) {
        try {
            sender.sendMessage(ChatColor.YELLOW + "Downloading version " + tagName + "...");
            plugin.getLogger().info("Downloading update from: " + downloadUrl);

            File tempFile = File.createTempFile("pvpbot-update", ".jar");
            tempFile.deleteOnExit();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", "PvPPracticeBot-Updater")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                sender.sendMessage(ChatColor.RED + "Download failed. Server returned code " + response.statusCode());
                plugin.getLogger().warning("Download failed with HTTP status: " + response.statusCode());
                return;
            }

            try (InputStream in = new BufferedInputStream(response.body());
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0) {
                sender.sendMessage(ChatColor.RED + "Downloaded file was empty or corrupted.");
                plugin.getLogger().severe("Downloaded file length was zero or file does not exist.");
                return;
            }

            sender.sendMessage(ChatColor.GREEN + "Download complete. Installing update...");
            plugin.getLogger().info("Download verified (" + tempFile.length() + " bytes). Swapping jar...");

            // Get current plugin jar location safely
            File currentJar = new File(PvPPracticeBot.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            
            // Move file using atomic replacement
            Files.move(tempFile.toPath(), currentJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Successfully installed update to " + currentJar.getName());
            sender.sendMessage(ChatColor.GREEN + "Update installed successfully!");

            // Schedule server restart on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Restarting the server in 3 seconds...");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    restartServer();
                }, 60L); // 3 seconds delay
            });

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to apply update. Check console for details.");
            plugin.getLogger().severe("Error applying update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void restartServer() {
        plugin.getLogger().info("Initiating server restart...");
        
        // Try Spigot's built-in restart first
        try {
            Bukkit.spigot().restart();
        } catch (NoSuchMethodError | Exception e) {
            plugin.getLogger().warning("Bukkit.spigot().restart() not supported or failed. Falling back to shutdown.");
            
            // Broadcast warning, save world and shutdown
            Bukkit.broadcastMessage(ChatColor.RED + "[PvPPracticeBot] Server shutting down for updates!");
            
            Bukkit.savePlayers();
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                world.save();
            }
            
            /*
             * NOTE ON RELAUNCHING:
             * Bukkit.shutdown() stops the Minecraft server process.
             * Relaunching the JVM afterward requires the server hosting script or wrapper to
             * detect exit and loop/restart. Standard scripts use a 'while true' bash loop.
             */
            Bukkit.shutdown();
        }
    }

    public static boolean isNewerVersion(String currentStr, String releaseStr) {
        if (currentStr == null || releaseStr == null) return false;
        
        // Strip 'v' if present
        if (currentStr.startsWith("v")) currentStr = currentStr.substring(1);
        if (releaseStr.startsWith("v")) releaseStr = releaseStr.substring(1);

        String[] currentParts = currentStr.split("\\.");
        String[] releaseParts = releaseStr.split("\\.");

        int length = Math.max(currentParts.length, releaseParts.length);
        for (int i = 0; i < length; i++) {
            int currentVal = i < currentParts.length ? parseOrZero(currentParts[i]) : 0;
            int releaseVal = i < releaseParts.length ? parseOrZero(releaseParts[i]) : 0;

            if (releaseVal > currentVal) return true;
            if (currentVal > releaseVal) return false;
        }
        return false;
    }

    private static int parseOrZero(String str) {
        try {
            return Integer.parseInt(str.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
