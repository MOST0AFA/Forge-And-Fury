package dev.most0afa.forge.and.fury.Items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.math.Box;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public class GravityInverter extends Item {
    private static final int LEVITATION_DURATION = 100;
    private static final int COOLDOWN_TICKS = 40;
    private static final double RAYCAST_DISTANCE = 8.0;
    private static final int SLOW_FALLING_BUFFER = 60;

    public GravityInverter(Settings settings) {
        super(settings.maxCount(1).fireproof().component(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                                new EntityAttributeModifier(Identifier.of("base_attack_damage"),
                                        0.0, EntityAttributeModifier.Operation.ADD_VALUE),
                                AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.GENERIC_ATTACK_SPEED,
                                new EntityAttributeModifier(Identifier.of("base_attack_speed"),
                                        -1.5, EntityAttributeModifier.Operation.ADD_VALUE),
                                AttributeModifierSlot.MAINHAND)
                        .build()));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        try {
            boolean success;

            if (player.isSneaking()) {
                success = handleTargetMode(world, player);
            } else {
                success = handleSelfMode(world, player);
            }

            if (success) {
                player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
                return TypedActionResult.success(stack);
            } else {
                return TypedActionResult.fail(stack);
            }

        } catch (Exception e) {
            System.err.println("GravityInverter error for player " + player.getName().getString() + ": " + e.getMessage());
            e.printStackTrace();

            player.sendMessage(Text.literal("§cGravity Inverter malfunctioned!"), true);
            return TypedActionResult.fail(stack);
        }
    }

    private boolean handleSelfMode(World world, PlayerEntity player) {
        if (applyLevitationEffects(player)) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.2F);

            spawnLevitationParticles(world, player);
            player.sendMessage(Text.literal("§bLevitating Self!"), true);
            return true;
        } else {
            player.sendMessage(Text.literal("§cFailed to levitate!"), true);
            return false;
        }
    }

    private boolean handleTargetMode(World world, PlayerEntity player) {
        LivingEntity target = getTargetEntity(world, player);

        if (target == null) {
            player.sendMessage(Text.literal("§cNo target found within " + (int)RAYCAST_DISTANCE + " blocks!"), true);
            return false;
        }

        if (applyLevitationEffects(target)) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.5F);

            spawnLevitationParticles(world, target);
            player.sendMessage(Text.literal("§bLevitating Entity: §e" + target.getName().getString()), true);
            return true;
        } else {
            player.sendMessage(Text.literal("§cTarget is immune to levitation!"), true);
            return false;
        }
    }

    private boolean applyLevitationEffects(LivingEntity target) {
        if (target == null || target.isRemoved() || !target.isAlive()) {
            return false;
        }

        if (target.isInvulnerable() || target.hasStatusEffect(StatusEffects.LEVITATION)) {
            return false;
        }

        try {
            target.removeStatusEffect(StatusEffects.LEVITATION);
            target.removeStatusEffect(StatusEffects.SLOW_FALLING);

            StatusEffectInstance levitation = new StatusEffectInstance(
                    StatusEffects.LEVITATION,
                    LEVITATION_DURATION,
                    1,
                    false,
                    true,
                    true
            );

            StatusEffectInstance slowFalling = new StatusEffectInstance(
                    StatusEffects.SLOW_FALLING,
                    LEVITATION_DURATION + SLOW_FALLING_BUFFER,
                    0,
                    false,
                    true,
                    true
            );

            boolean levitationApplied = target.addStatusEffect(levitation);
            target.addStatusEffect(slowFalling);

            return levitationApplied;

        } catch (Exception e) {
            System.err.println("Failed to apply levitation effects: " + e.getMessage());
            return false;
        }
    }

    private void spawnLevitationParticles(World world, LivingEntity entity) {
        if (world instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 20; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 2.0;
                double offsetY = world.random.nextDouble() * 2.0;
                double offsetZ = (world.random.nextDouble() - 0.5) * 2.0;

                serverWorld.spawnParticles(
                        ParticleTypes.END_ROD,
                        entity.getX() + offsetX,
                        entity.getY() + offsetY,
                        entity.getZ() + offsetZ,
                        1, 0.0, 0.1, 0.0, 0.02
                );
            }
        }
    }

    private LivingEntity getTargetEntity(World world, PlayerEntity player) {
        if (player == null || world == null) return null;

        try {
            Vec3d start = player.getEyePos();
            Vec3d direction = player.getRotationVector();
            Vec3d end = start.add(direction.multiply(RAYCAST_DISTANCE));

            Box searchBox = Box.from(start).union(Box.from(end)).expand(1.0);

            EntityHitResult entityHit = ProjectileUtil.raycast(
                    player,
                    start,
                    end,
                    searchBox,
                    entity -> {
                        return entity instanceof LivingEntity &&
                                entity != player &&
                                !entity.isRemoved() &&
                                entity.isAlive() &&
                                !entity.isInvulnerable();
                    },
                    RAYCAST_DISTANCE * RAYCAST_DISTANCE
            );

            if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
                return target;
            }
        } catch (Exception e) {
            System.err.println("Error in getTargetEntity: " + e.getMessage());
        }

        return null;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}