package com.ansh9boss.pvpbot.commands;

import com.ansh9boss.pvpbot.PvPPracticeBot;
import com.ansh9boss.pvpbot.npc.PracticeBotTrait;
import com.ansh9boss.pvpbot.npc.PlayerSessionStats;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
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
import java.util.stream.Collectors;

public class PlayerCommandExecutor implements CommandExecutor, TabCompleter {

    private final PvPPracticeBot plugin;

    public PlayerCommandExecutor(PvPPracticeBot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvpbot.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Check if Citizens is enabled for bot-dependent subcommands
        if (!plugin.isCitizensEnabled() && !subCommand.equals("stats") && !subCommand.equals("help")) {
            player.sendMessage(ChatColor.RED + "Citizens is not enabled on this server. Bot commands are disabled.");
            return true;
        }

        switch (subCommand) {
            case "spawn":
                plugin.getBotManager().spawnBot(player);
                break;

            case "remove":
                plugin.getBotManager().removeLastBot(player);
                break;

            case "mode":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pvpbot mode <peace|attack>");
                    return true;
                }
                setBotMode(player, args[1]);
                break;

            case "gamemode":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pvpbot gamemode <sword|boxing|sumo|bow|nodebuff>");
                    return true;
                }
                setBotGamemode(player, args[1]);
                break;

            case "kitmirror":
                syncBotKit(player);
                break;

            case "difficulty":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pvpbot difficulty <1-5>");
                    return true;
                }
                setBotDifficulty(player, args[1]);
                break;

            case "immortal":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pvpbot immortal <true|false>");
                    return true;
                }
                setBotImmortal(player, args[1]);
                break;

            case "stats":
                showStats(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /pvpbot help for commands.");
                break;
        }

        return true;
    }

    private void setBotMode(Player player, String modeStr) {
        List<NPC> bots = plugin.getBotManager().getActiveBots(player.getUniqueId());
        if (bots.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not have any active bots.");
            return;
        }

        boolean attackMode;
        if ("attack".equalsIgnoreCase(modeStr)) {
            attackMode = true;
        } else if ("peace".equalsIgnoreCase(modeStr)) {
            attackMode = false;
        } else {
            player.sendMessage(ChatColor.RED + "Invalid mode! Choose 'peace' or 'attack'.");
            return;
        }

        NPC npc = bots.get(bots.size() - 1);
        PracticeBotTrait trait = npc.getOrAddTrait(PracticeBotTrait.class);
        trait.setAttackMode(attackMode);

        player.sendMessage(ChatColor.GREEN + "Set your bot's mode to: " + ChatColor.YELLOW + (attackMode ? "Attack" : "Peace"));
    }

    private void setBotGamemode(Player player, String preset) {
        List<NPC> bots = plugin.getBotManager().getActiveBots(player.getUniqueId());
        if (bots.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not have any active bots.");
            return;
        }

        List<String> allowedPresets = Arrays.asList("sword", "boxing", "sumo", "bow", "nodebuff");
        if (!allowedPresets.contains(preset.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "Invalid preset! Choose: sword, boxing, sumo, bow, nodebuff");
            return;
        }

        NPC npc = bots.get(bots.size() - 1);
        PracticeBotTrait trait = npc.getOrAddTrait(PracticeBotTrait.class);
        trait.setGamemodePreset(preset.toLowerCase());

        player.sendMessage(ChatColor.GREEN + "Applied preset gamemode: " + ChatColor.YELLOW + preset.toUpperCase());
    }

    private void syncBotKit(Player player) {
        List<NPC> bots = plugin.getBotManager().getActiveBots(player.getUniqueId());
        if (bots.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not have any active bots.");
            return;
        }

        NPC npc = bots.get(bots.size() - 1);
        plugin.getBotManager().mirrorKit(npc, player);
        player.sendMessage(ChatColor.GREEN + "Mirrored your inventory equipment onto your bot.");
    }

    private void setBotDifficulty(Player player, String diffStr) {
        List<NPC> bots = plugin.getBotManager().getActiveBots(player.getUniqueId());
        if (bots.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not have any active bots.");
            return;
        }

        try {
            int diff = Integer.parseInt(diffStr);
            if (diff < 1 || diff > 5) {
                player.sendMessage(ChatColor.RED + "Difficulty must be between 1 and 5.");
                return;
            }

            NPC npc = bots.get(bots.size() - 1);
            PracticeBotTrait trait = npc.getOrAddTrait(PracticeBotTrait.class);
            trait.setDifficulty(diff);

            player.sendMessage(ChatColor.GREEN + "Set your bot's difficulty to: " + ChatColor.YELLOW + diff);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid difficulty number.");
        }
    }

    private void setBotImmortal(Player player, String immortalStr) {
        List<NPC> bots = plugin.getBotManager().getActiveBots(player.getUniqueId());
        if (bots.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not have any active bots.");
            return;
        }

        boolean immortal = Boolean.parseBoolean(immortalStr);
        NPC npc = bots.get(bots.size() - 1);
        PracticeBotTrait trait = npc.getOrAddTrait(PracticeBotTrait.class);
        trait.setImmortal(immortal);

        player.sendMessage(ChatColor.GREEN + "Set your bot's immortality to: " + ChatColor.YELLOW + immortal);
    }

    private void showStats(Player player) {
        PlayerSessionStats stats = plugin.getBotManager().getStats(player.getUniqueId());
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
        player.sendMessage(ChatColor.GOLD + "      PvPPracticeBot Stats      ");
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
        player.sendMessage(ChatColor.GRAY + "Hits Landed on Bot: " + ChatColor.GREEN + stats.getHitsLanded());
        player.sendMessage(ChatColor.GRAY + "Hits Taken from Bot: " + ChatColor.RED + stats.getHitsTaken());
        player.sendMessage(ChatColor.GRAY + "Current Hit Streak: " + ChatColor.YELLOW + stats.getCurrentStreak());
        player.sendMessage(ChatColor.GRAY + "Longest Hit Streak: " + ChatColor.GOLD + stats.getLongestStreak());
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
        player.sendMessage(ChatColor.GOLD + "     PvPPracticeBot Help        ");
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
        player.sendMessage(ChatColor.YELLOW + "/pvpbot spawn " + ChatColor.GRAY + "- Spawn bot near you");
        player.sendMessage(ChatColor.YELLOW + "/pvpbot remove " + ChatColor.GRAY + "- Remove your active bot");
        player.sendMessage(ChatColor.YELLOW + "/pvpbot mode <peace|attack> " + ChatColor.GRAY + "- Set behavior mode");
        player.sendMessage(ChatColor.YELLOW + "/pvpbot gamemode <preset> " + ChatColor.GRAY + "- Change preset");
        player.sendMessage(ChatColor.YELLOW + "/pvpbot kitmirror " + ChatColor.GRAY + "- Re-sync bot gear to yours");
        player.sendMessage(ChatColor.YELLOW + "/pvpbot difficulty <1-5> " + ChatColor.GRAY + "- Set difficulty scaling");
        player.sendMessage(ChatColor.YELLOW + "/pvpbot immortal <true|false> " + ChatColor.GRAY + "- Toggle immortal bot");
        player.sendMessage(ChatColor.YELLOW + "/pvpbot stats " + ChatColor.GRAY + "- Display your current session stats");
        player.sendMessage(ChatColor.YELLOW + "/pvpbot help " + ChatColor.GRAY + "- View this menu");
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("spawn", "remove", "mode", "gamemode", "kitmirror", "difficulty", "immortal", "stats", "help").stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "mode":
                    return Arrays.asList("peace", "attack").stream()
                            .filter(m -> m.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "gamemode":
                    return Arrays.asList("sword", "boxing", "sumo", "bow", "nodebuff").stream()
                            .filter(g -> g.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "difficulty":
                    return Arrays.asList("1", "2", "3", "4", "5").stream()
                            .filter(d -> d.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "immortal":
                    return Arrays.asList("true", "false").stream()
                            .filter(i -> i.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
