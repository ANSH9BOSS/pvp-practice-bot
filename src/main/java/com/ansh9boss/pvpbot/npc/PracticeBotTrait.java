package com.ansh9boss.pvpbot.npc;

import com.ansh9boss.pvpbot.PvPPracticeBot;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.trait.trait.Equipment;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

@TraitName("PracticeBotTrait")
public class PracticeBotTrait extends Trait {

    private UUID ownerUuid;
    private boolean attackMode = false; // false = peace, true = attack
    private String gamemodePreset = "sword"; // sword, boxing, sumo, bow, nodebuff
    private int difficulty = 3; // 1 to 5
    private boolean immortal = false;

    private int reactionTicksRemaining = -1;
    private int attackCooldownTicks = 0;
    private int arrowCooldownTicks = 0;
    private int critChainCount = 0;
    private final Random random = new Random();

    public PracticeBotTrait() {
        super("PracticeBotTrait");
    }

    public void setOwner(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public boolean isAttackMode() {
        return attackMode;
    }

    public void setAttackMode(boolean attackMode) {
        this.attackMode = attackMode;
        if (!attackMode) {
            this.reactionTicksRemaining = -1;
        }
    }

    public String getGamemodePreset() {
        return gamemodePreset;
    }

    public void setGamemodePreset(String gamemodePreset) {
        this.gamemodePreset = gamemodePreset;
        this.applyPresetKit();
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        if (difficulty >= 1 && difficulty <= 5) {
            this.difficulty = difficulty;
        }
    }

    public boolean isImmortal() {
        return immortal;
    }

    public void setImmortal(boolean immortal) {
        this.immortal = immortal;
        if (immortal && npc.isSpawned() && npc.getEntity() instanceof LivingEntity) {
            // Heal to full instantly
            LivingEntity d = (LivingEntity) npc.getEntity();
            double maxHealth = d.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            d.setHealth(maxHealth);
        }
    }

    @Override
    public void run() {
        if (!npc.isSpawned() || ownerUuid == null) {
            return;
        }

        Player player = Bukkit.getPlayer(ownerUuid);
        if (player == null || !player.isOnline()) {
            // Owner left, remove the NPC
            npc.destroy();
            return;
        }

        LivingEntity npcEntity = (LivingEntity) npc.getEntity();
        if (npcEntity == null) {
            return;
        }

        // 1. Rule execution: Potion effect stripping for "nodebuff"
        if ("nodebuff".equalsIgnoreCase(gamemodePreset)) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : npcEntity.getActivePotionEffects()) {
                npcEntity.removePotionEffect(effect.getType());
            }
        }

        // 2. Rule execution: Boxing speed effects
        if ("boxing".equalsIgnoreCase(gamemodePreset)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false));
            npcEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false));
        }

        // 3. Rule execution: Immortal health lock
        if (immortal) {
            double maxHealth = npcEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            if (npcEntity.getHealth() < maxHealth) {
                npcEntity.setHealth(maxHealth);
            }
        }

        // 4. Movement AI (always navigate to follow or attack)
        Location npcLoc = npcEntity.getLocation();
        Location playerLoc = player.getLocation();

        if (npcLoc.getWorld() != playerLoc.getWorld()) {
            // Different worlds, teleport NPC to player
            npcEntity.teleport(playerLoc.clone().add(1, 0, 1));
            return;
        }

        double distance = npcLoc.distance(playerLoc);

        // Pathfind towards player
        npc.getNavigator().setTarget((org.bukkit.entity.Entity) player, false);

        // 5. Combat action loop (Only in Attack mode)
        if (attackMode) {
            // Decrement attack cooldowns
            if (attackCooldownTicks > 0) {
                attackCooldownTicks--;
            }
            if (arrowCooldownTicks > 0) {
                arrowCooldownTicks--;
            }

            if ("bow".equalsIgnoreCase(gamemodePreset)) {
                // Bow mode shooting logic
                handleRangedAttack(player, npcEntity, distance);
            } else {
                // Melee mode attack logic (sword, boxing, sumo, nodebuff)
                handleMeleeAttack(player, npcEntity, distance);
            }
        }
    }

    private void handleMeleeAttack(Player player, LivingEntity npcEntity, double distance) {
        // Melee reach is typically 3.2 blocks
        if (distance <= 3.2) {
            if (attackCooldownTicks <= 0) {
                if (reactionTicksRemaining == -1) {
                    // Roll reaction delay based on difficulty
                    reactionTicksRemaining = getReactionDelayTicks();
                } else if (reactionTicksRemaining > 0) {
                    reactionTicksRemaining--;
                } else {
                    // reactionTicksRemaining is 0, execute attack
                    reactionTicksRemaining = -1;
                    executeMeleeHit(player, npcEntity);
                }
            }
        } else {
            // Reset reaction if player runs away out of range
            if (distance > 3.8) {
                reactionTicksRemaining = -1;
            }
        }
    }

    private void executeMeleeHit(Player player, LivingEntity npcEntity) {
        // Swing hand
        if (npcEntity instanceof Player) {
            ((Player) npcEntity).swingMainHand();
        }

        // Roll accuracy
        double accuracy = getAccuracyChance();
        if (random.nextDouble() <= accuracy) {
            // Hit landed!
            double damageAmount = getPresetMeleeDamage();

            if (!"boxing".equalsIgnoreCase(gamemodePreset) && !"sumo".equalsIgnoreCase(gamemodePreset) && damageAmount > 0.0) {
                // Roll critical hit chance (scales with difficulty, 20% to 40%)
                double critChance = 0.15 + (0.05 * difficulty);
                if (random.nextDouble() <= critChance) {
                    critChainCount++;
                    // Critical damage multiplier (starts at 1.5x, goes up with crit chains)
                    double multiplier = 1.5 + (0.15 * Math.min(critChainCount - 1, 3));
                    double finalDamage = damageAmount * multiplier;
                    
                    player.damage(finalDamage, npcEntity);
                    
                    // Spawn critical hit particles
                    player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0), 15, 0.2, 0.3, 0.2, 0.15);
                    
                    if (critChainCount >= 3) {
                        // Extra magic crit particles for high chains
                        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1.0, 0), 10, 0.2, 0.3, 0.2, 0.15);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
                    } else {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                    }
                } else {
                    critChainCount = 0;
                    player.damage(damageAmount, npcEntity);
                    // Spawn sweep particle for normal hits
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.0, 0), 1);
                }
            } else {
                critChainCount = 0;
                player.damage(damageAmount, npcEntity);
                if ("boxing".equalsIgnoreCase(gamemodePreset)) {
                    // Small hit visual/sound for boxing
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.0, 0), 1);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 1.0f);
                }
            }
        }

        // Set cooldown based on difficulty
        attackCooldownTicks = getAttackCooldownTicks();
    }

    private void handleRangedAttack(Player player, LivingEntity npcEntity, double distance) {
        // Only shoot if in ranged bounds
        if (distance >= 3.0 && distance <= 22.0) {
            if (arrowCooldownTicks <= 0) {
                arrowCooldownTicks = getArrowCooldownTicks();

                // Swing hand to simulate drawing bow
                if (npcEntity instanceof Player) {
                    ((Player) npcEntity).swingMainHand();
                }

                // Shoot arrow
                Location eyeLoc = npcEntity.getEyeLocation();
                Vector direction = player.getEyeLocation().toVector().subtract(eyeLoc.toVector());
                double dist = direction.length();
                
                // Add vertical arch offset to vector based on distance
                direction.setY(direction.getY() + (dist * 0.1));
                direction.normalize();

                // Roll accuracy: slightly randomize direction if accuracy check fails
                double accuracy = getAccuracyChance();
                if (random.nextDouble() > accuracy) {
                    // Add some spread/error to the arrow vector
                    direction.add(new Vector(
                            (random.nextDouble() - 0.5) * 0.2,
                            (random.nextDouble() - 0.5) * 0.2,
                            (random.nextDouble() - 0.5) * 0.2
                    )).normalize();
                }

                Arrow arrow = npcEntity.getWorld().spawnArrow(eyeLoc, direction, 1.6f, 0.0f);
                arrow.setShooter(npcEntity);

                // Play shoot sound
                npcEntity.getWorld().playSound(eyeLoc, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
            }
        }
    }

    // Helper functions for difficulty scaling
    private int getReactionDelayTicks() {
        return switch (difficulty) {
            case 1 -> 18 + random.nextInt(12); // 18-30 ticks (~1s - 1.5s)
            case 2 -> 12 + random.nextInt(8);  // 12-20 ticks
            case 3 -> 6 + random.nextInt(6);   // 6-12 ticks
            case 4 -> 2 + random.nextInt(4);   // 2-6 ticks
            case 5 -> random.nextInt(2);       // 0-2 ticks (extremely fast)
            default -> 8;
        };
    }

    private double getAccuracyChance() {
        return switch (difficulty) {
            case 1 -> 0.35;
            case 2 -> 0.55;
            case 3 -> 0.75;
            case 4 -> 0.88;
            case 5 -> 0.97;
            default -> 0.70;
        };
    }

    private int getAttackCooldownTicks() {
        return switch (difficulty) {
            case 1 -> 25; // Slow hits
            case 2 -> 20;
            case 3 -> 15;
            case 4 -> 12;
            case 5 -> 10; // Rapid hits (~0.5s)
            default -> 15;
        };
    }

    private int getArrowCooldownTicks() {
        return switch (difficulty) {
            case 1 -> 60; // 3 seconds
            case 2 -> 50; // 2.5 seconds
            case 3 -> 40; // 2.0 seconds
            case 4 -> 30; // 1.5 seconds
            case 5 -> 20; // 1.0 second
            default -> 40;
        };
    }

    private double getPresetMeleeDamage() {
        if ("boxing".equalsIgnoreCase(gamemodePreset) || "sumo".equalsIgnoreCase(gamemodePreset)) {
            return 0.0; // Handled specially or minimal
        }
        // Base damage scales significantly higher by difficulty
        return 1.5 + (1.5 * difficulty); // Diff 1: 3.0, Diff 3: 6.0, Diff 5: 9.0
    }

    // Apply the preset armor and weapons to the NPC
    public void applyPresetKit() {
        if (!npc.isSpawned()) return;
        Equipment equip = npc.getOrAddTrait(Equipment.class);

        // Clear existing
        for (int i = 0; i <= 5; i++) {
            equip.set(i, null);
        }

        switch (gamemodePreset.toLowerCase()) {
            case "boxing":
                // Leather armor, no weapons
                equip.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.LEATHER_HELMET));
                equip.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.LEATHER_CHESTPLATE));
                equip.set(Equipment.EquipmentSlot.LEGGINGS, new ItemStack(Material.LEATHER_LEGGINGS));
                equip.set(Equipment.EquipmentSlot.BOOTS, new ItemStack(Material.LEATHER_BOOTS));
                break;
            case "sumo":
                // No armor, no weapons
                break;
            case "bow":
                // Chainmail armor, bow
                equip.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.CHAINMAIL_HELMET));
                equip.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                equip.set(Equipment.EquipmentSlot.LEGGINGS, new ItemStack(Material.CHAINMAIL_LEGGINGS));
                equip.set(Equipment.EquipmentSlot.BOOTS, new ItemStack(Material.CHAINMAIL_BOOTS));
                equip.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.BOW));
                equip.set(Equipment.EquipmentSlot.OFF_HAND, new ItemStack(Material.ARROW));
                break;
            case "nodebuff":
            case "sword":
            default:
                // Diamond armor, diamond sword, shield
                equip.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.DIAMOND_HELMET));
                equip.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.DIAMOND_CHESTPLATE));
                equip.set(Equipment.EquipmentSlot.LEGGINGS, new ItemStack(Material.DIAMOND_LEGGINGS));
                equip.set(Equipment.EquipmentSlot.BOOTS, new ItemStack(Material.DIAMOND_BOOTS));
                equip.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.DIAMOND_SWORD));
                equip.set(Equipment.EquipmentSlot.OFF_HAND, new ItemStack(Material.SHIELD));
                break;
        }
    }
}
