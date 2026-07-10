package com.ansh9boss.pvpbot.listeners;

import com.ansh9boss.pvpbot.PvPPracticeBot;
import com.ansh9boss.pvpbot.npc.PracticeBotTrait;
import com.ansh9boss.pvpbot.npc.PlayerSessionStats;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.UUID;

public class BotListener implements Listener {

    private final PvPPracticeBot plugin;

    public BotListener(PvPPracticeBot plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 1. Check if the bot was damaged
        NPC targetNpc = getNPCEntity(event.getEntity());
        if (targetNpc != null && targetNpc.hasTrait(PracticeBotTrait.class)) {
            PracticeBotTrait trait = targetNpc.getOrAddTrait(PracticeBotTrait.class);
            Player damager = getPlayerDamager(event);

            if (damager != null && damager.getUniqueId().equals(trait.getOwnerUuid())) {
                PlayerSessionStats stats = plugin.getBotManager().getStats(damager.getUniqueId());
                
                // Track hit landed on bot
                stats.incrementHitsLanded();

                // Immortal behavior
                if (trait.isImmortal()) {
                    event.setCancelled(true);
                    if (targetNpc.getEntity() instanceof LivingEntity) {
                        LivingEntity d = (LivingEntity) targetNpc.getEntity();
                        double maxHealth = d.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                        d.setHealth(maxHealth);
                    }
                    return;
                }

                // Sumo behavior
                if ("sumo".equalsIgnoreCase(trait.getGamemodePreset())) {
                    event.setCancelled(true);
                    // Apply knockback manually since event is cancelled
                    if (targetNpc.isSpawned()) {
                        org.bukkit.entity.Entity npcEnt = targetNpc.getEntity();
                        Vector velocity = npcEnt.getLocation().toVector().subtract(damager.getLocation().toVector());
                        velocity.setY(0.0);
                        if (velocity.lengthSquared() > 0) {
                            velocity.normalize();
                        } else {
                            velocity = new Vector(1, 0, 0);
                        }
                        velocity.multiply(0.48);
                        velocity.setY(0.35);
                        npcEnt.setVelocity(velocity);
                    }
                }
            }
            return;
        }

        // 2. Check if the player was damaged by their bot
        if (event.getEntity() instanceof Player) {
            Player damagedPlayer = (Player) event.getEntity();
            NPC damagerNpc = getNPCDamager(event);

            if (damagerNpc != null && damagerNpc.hasTrait(PracticeBotTrait.class)) {
                PracticeBotTrait trait = damagerNpc.getOrAddTrait(PracticeBotTrait.class);

                if (damagedPlayer.getUniqueId().equals(trait.getOwnerUuid())) {
                    PlayerSessionStats stats = plugin.getBotManager().getStats(damagedPlayer.getUniqueId());
                    
                    // Track hit taken from bot
                    stats.incrementHitsTaken();

                    // Sumo behavior
                    if ("sumo".equalsIgnoreCase(trait.getGamemodePreset())) {
                        event.setCancelled(true);
                        // Apply knockback manually since event is cancelled
                        if (damagerNpc.isSpawned()) {
                            org.bukkit.entity.Entity npcEnt = damagerNpc.getEntity();
                            Vector velocity = damagedPlayer.getLocation().toVector().subtract(npcEnt.getLocation().toVector());
                            velocity.setY(0.0);
                            if (velocity.lengthSquared() > 0) {
                                velocity.normalize();
                            } else {
                                velocity = new Vector(1, 0, 0);
                            }
                            velocity.multiply(0.48);
                            velocity.setY(0.35);
                            damagedPlayer.setVelocity(velocity);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Destroy all bots belonging to the quitting player
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getBotManager().removePlayerBots(uuid);
        // Clear session stats on quit
        plugin.getBotManager().getStats(uuid).reset();
    }

    private Player getPlayerDamager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                return (Player) proj.getShooter();
            }
        }
        return null;
    }

    private NPC getNPCDamager(EntityDamageByEntityEvent event) {
        org.bukkit.entity.Entity damager = event.getDamager();
        if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof org.bukkit.entity.Entity) {
                damager = (org.bukkit.entity.Entity) proj.getShooter();
            }
        }
        return getNPCEntity(damager);
    }

    private NPC getNPCEntity(org.bukkit.entity.Entity entity) {
        if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(entity)) {
            return CitizensAPI.getNPCRegistry().getNPC(entity);
        }
        return null;
    }
}
