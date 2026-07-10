package com.ansh9boss.pvpbot.commands;

import com.ansh9boss.pvpbot.PvPPracticeBot;
import com.ansh9boss.pvpbot.npc.PracticeBotTrait;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminCommandExecutor implements CommandExecutor, TabCompleter {

    private final PvPPracticeBot plugin;

    public AdminCommandExecutor(PvPPracticeBot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // General admin command permission check (except for update, which has a custom node)
        if (args.length > 0 && "update".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("pvpbot.admin.update")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to execute updates.");
                return true;
            }
        } else {
            if (!sender.hasPermission("pvpbot.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use admin commands.");
                return true;
            }
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                sender.sendMessage(ChatColor.YELLOW + "Hot-reloading PvPPracticeBot plugin...");
                plugin.getLogger().info("Initiating hot-reload of PvPPracticeBot...");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.getPluginManager().disablePlugin(plugin);
                    Bukkit.getPluginManager().enablePlugin(plugin);
                });
                break;

            case "maxbots":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /pvpbotadmin maxbots <player> <amount>");
                    return true;
                }
                setMaxBots(sender, args[1], args[2]);
                break;

            case "cooldown":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /pvpbotadmin cooldown <seconds>");
                    return true;
                }
                setCooldown(sender, args[1]);
                break;

            case "forceremove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /pvpbotadmin forceremove <player>");
                    return true;
                }
                forceRemove(sender, args[1]);
                break;

            case "immortal":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /pvpbotadmin immortal <player> <true|false>");
                    return true;
                }
                overrideImmortal(sender, args[1], args[2]);
                break;

            case "update":
                boolean confirm = args.length > 1 && "confirm".equalsIgnoreCase(args[1]);
                if (plugin.getBotManager().hasActiveBots(UUID.randomUUID())) { // Stub check
                    // Simply access updater
                }
                plugin.getUpdater().checkForUpdate(sender, confirm);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown admin subcommand.");
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void setMaxBots(CommandSender sender, String playerName, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            if (amount < 1) {
                sender.sendMessage(ChatColor.RED + "Amount must be at least 1.");
                return;
            }

            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            if (target.getUniqueId() == null) {
                sender.sendMessage(ChatColor.RED + "Player " + playerName + " not found.");
                return;
            }

            plugin.getConfigManager().setMaxBots(target.getUniqueId(), amount);
            sender.sendMessage(ChatColor.GREEN + "Set max bots limit for " + ChatColor.YELLOW + playerName + ChatColor.GREEN + " to " + ChatColor.YELLOW + amount);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format for amount.");
        }
    }

    private void setCooldown(CommandSender sender, String secondsStr) {
        try {
            int seconds = Integer.parseInt(secondsStr);
            if (seconds < 0) {
                sender.sendMessage(ChatColor.RED + "Cooldown cannot be negative.");
                return;
            }

            plugin.getConfigManager().setSpawnCooldown(seconds);
            sender.sendMessage(ChatColor.GREEN + "Set global spawn cooldown to " + ChatColor.YELLOW + seconds + " seconds.");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format for cooldown.");
        }
    }

    private void forceRemove(CommandSender sender, String playerName) {
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target.getUniqueId() == null) {
            sender.sendMessage(ChatColor.RED + "Player " + playerName + " not found.");
            return;
        }

        if (!plugin.getBotManager().hasActiveBots(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "Player " + playerName + " does not have any active bots.");
            return;
        }

        plugin.getBotManager().removePlayerBots(target.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + "Removed all active bots belonging to " + ChatColor.YELLOW + playerName + ChatColor.GREEN + ".");
        
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage(ChatColor.RED + "An administrator has removed your active practice bots.");
        }
    }

    private void overrideImmortal(CommandSender sender, String playerName, String valStr) {
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target.getUniqueId() == null) {
            sender.sendMessage(ChatColor.RED + "Player " + playerName + " not found.");
            return;
        }

        List<NPC> bots = plugin.getBotManager().getActiveBots(target.getUniqueId());
        if (bots.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Player " + playerName + " has no active bots.");
            return;
        }

        boolean val = Boolean.parseBoolean(valStr);
        NPC npc = bots.get(bots.size() - 1);
        PracticeBotTrait trait = npc.getOrAddTrait(PracticeBotTrait.class);
        trait.setImmortal(val);

        sender.sendMessage(ChatColor.GREEN + "Overrode immortal status for " + ChatColor.YELLOW + playerName + "'s bot to: " + ChatColor.YELLOW + val);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
        sender.sendMessage(ChatColor.GOLD + "   PvPPracticeBot Admin Help    ");
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
        sender.sendMessage(ChatColor.YELLOW + "/pvpbotadmin reload " + ChatColor.GRAY + "- Reload config.yml");
        sender.sendMessage(ChatColor.YELLOW + "/pvpbotadmin maxbots <player> <amount> " + ChatColor.GRAY + "- Set player bot limit");
        sender.sendMessage(ChatColor.YELLOW + "/pvpbotadmin cooldown <seconds> " + ChatColor.GRAY + "- Set spawn cooldown");
        sender.sendMessage(ChatColor.YELLOW + "/pvpbotadmin forceremove <player> " + ChatColor.GRAY + "- Force remove player's bot");
        sender.sendMessage(ChatColor.YELLOW + "/pvpbotadmin immortal <player> <true|false> " + ChatColor.GRAY + "- Override bot immortal status");
        sender.sendMessage(ChatColor.YELLOW + "/pvpbotadmin update " + ChatColor.GRAY + "- Check/apply updates");
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "maxbots", "cooldown", "forceremove", "immortal", "update").stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "maxbots":
                case "forceremove":
                case "immortal":
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "cooldown":
                    return Arrays.asList("0", "5", "10", "15", "30").stream()
                            .filter(c -> c.startsWith(args[1]))
                            .collect(Collectors.toList());
                case "update":
                    return Arrays.asList("confirm").stream()
                            .filter(c -> c.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "maxbots":
                    return Arrays.asList("1", "2", "3", "5").stream()
                            .filter(a -> a.startsWith(args[2]))
                            .collect(Collectors.toList());
                case "immortal":
                    return Arrays.asList("true", "false").stream()
                            .filter(i -> i.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
