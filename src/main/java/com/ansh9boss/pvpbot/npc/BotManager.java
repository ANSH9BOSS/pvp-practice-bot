package com.ansh9boss.pvpbot.npc;

import com.ansh9boss.pvpbot.PvPPracticeBot;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BotManager {

    private final PvPPracticeBot plugin;
    private final Map<UUID, List<NPC>> activeBots = new HashMap<>();
    private final Map<UUID, PlayerSessionStats> playerStats = new HashMap<>();
    private final Map<UUID, Long> spawnCooldowns = new HashMap<>();

    public BotManager(PvPPracticeBot plugin) {
        this.plugin = plugin;
    }

    public boolean spawnBot(Player player) {
        if (!plugin.isCitizensEnabled()) {
            player.sendMessage(ChatColor.RED + "Error: Citizens plugin is not enabled. Cannot spawn bots.");
            return false;
        }

        UUID uuid = player.getUniqueId();

        // Cooldown check
        int cooldownSec = plugin.getConfigManager().getSpawnCooldown();
        if (cooldownSec > 0 && spawnCooldowns.containsKey(uuid)) {
            long timeLeft = (spawnCooldowns.get(uuid) + (cooldownSec * 1000L)) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Spawn is on cooldown! Wait " + (timeLeft / 1000 + 1) + "s.");
                return false;
            }
        }

        // Active bot count check
        List<NPC> list = activeBots.computeIfAbsent(uuid, k -> new ArrayList<>());
        int maxAllowed = plugin.getConfigManager().getMaxBots(uuid);
        if (list.size() >= maxAllowed) {
            player.sendMessage(ChatColor.RED + "You have reached your limit of " + maxAllowed + " active bots! Remove one first.");
            return false;
        }

        // Spawn position (2 blocks in front of the player, facing the player)
        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2.0));
        // Rotate NPC to face player
        Location dirLoc = player.getLocation().subtract(spawnLoc);
        spawnLoc.setDirection(dirLoc.toVector());

        // Create Citizens NPC
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName() + " Bot");

        // Skin Mirroring
        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        skinTrait.setSkinName(player.getName(), true);

        // Add Custom AI Trait
        PracticeBotTrait botTrait = npc.getOrAddTrait(PracticeBotTrait.class);
        botTrait.setOwner(uuid);
        botTrait.setDifficulty(plugin.getConfigManager().getDefaultDifficulty());
        botTrait.setAttackMode(false); // Default to peace
        botTrait.setGamemodePreset("sword"); // Default to sword

        // Spawn
        npc.spawn(spawnLoc);

        // Mirror kit initially
        mirrorKit(npc, player);

        // Track NPC
        list.add(npc);

        // Apply cooldown
        spawnCooldowns.put(uuid, System.currentTimeMillis());

        player.sendMessage(ChatColor.GREEN + "Spawned PvP Practice Bot in Peace mode.");
        return true;
    }

    public void mirrorKit(NPC npc, Player player) {
        if (npc == null || !npc.isSpawned()) return;

        Equipment equipTrait = npc.getOrAddTrait(Equipment.class);

        // Mirror Armor
        equipTrait.set(Equipment.EquipmentSlot.HELMET, player.getInventory().getHelmet());
        equipTrait.set(Equipment.EquipmentSlot.CHESTPLATE, player.getInventory().getChestplate());
        equipTrait.set(Equipment.EquipmentSlot.LEGGINGS, player.getInventory().getLeggings());
        equipTrait.set(Equipment.EquipmentSlot.BOOTS, player.getInventory().getBoots());

        // Mirror Hands
        equipTrait.set(Equipment.EquipmentSlot.HAND, player.getInventory().getItemInMainHand());
        equipTrait.set(Equipment.EquipmentSlot.OFF_HAND, player.getInventory().getItemInOffHand());
    }

    public boolean removeLastBot(Player player) {
        UUID uuid = player.getUniqueId();
        List<NPC> list = activeBots.get(uuid);
        if (list == null || list.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not have any active bots.");
            return false;
        }

        NPC npc = list.remove(list.size() - 1);
        if (npc != null) {
            npc.destroy();
        }
        player.sendMessage(ChatColor.GREEN + "Removed your active bot.");
        return true;
    }

    public void removePlayerBots(UUID playerUuid) {
        List<NPC> list = activeBots.remove(playerUuid);
        if (list != null) {
            for (NPC npc : list) {
                if (npc != null) {
                    npc.destroy();
                }
            }
        }
    }

    public void removeAllBots() {
        if (!plugin.isCitizensEnabled()) return;

        for (List<NPC> list : activeBots.values()) {
            for (NPC npc : list) {
                if (npc != null) {
                    npc.destroy();
                }
            }
        }
        activeBots.clear();
    }

    public PlayerSessionStats getStats(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, k -> new PlayerSessionStats());
    }

    public List<NPC> getActiveBots(UUID uuid) {
        return activeBots.getOrDefault(uuid, Collections.emptyList());
    }

    public boolean hasActiveBots(UUID uuid) {
        List<NPC> list = activeBots.get(uuid);
        return list != null && !list.isEmpty();
    }
}
