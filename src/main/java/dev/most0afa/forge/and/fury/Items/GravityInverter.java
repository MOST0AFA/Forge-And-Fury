package dev.most0afa.forge.and.fury.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class GravityInverter extends Item {
    private static final int LEVITATION_DURATION = 100;
    private static final int COOLDOWN_TICKS = 40;
    private static final double RAYCAST_DISTANCE = 8.0;
    private static final int SLOW_FALLING_BUFFER = 60;

    public GravityInverter(Item.Properties properties) {
        super(properties.stacksTo(1).fireResistant().attributes(
                ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(Identifier.fromNamespaceAndPath("forgeandfury", "base_attack_damage"),
                                        0.0, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED,
                                new AttributeModifier(Identifier.fromNamespaceAndPath("forgeandfury", "base_attack_speed"),
                                        -1.5, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        try {
            boolean success;

            if (player.isShiftKeyDown()) {
                success = handleTargetMode(level, player);
            } else {
                success = handleSelfMode(level, player);
            }

            if (success) {
                player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.FAIL;
            }

        } catch (Exception e) {
            player.sendOverlayMessage(Component.literal("§cGravity Inverter malfunctioned!"));
            return InteractionResult.FAIL;
        }
    }

    private boolean handleSelfMode(Level level, Player player) {
        if (applyLevitationEffects(player)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);

            spawnLevitationParticles(level, player);
            player.sendOverlayMessage(Component.literal("§bLevitating Self!"));
            return true;
        } else {
            player.sendOverlayMessage(Component.literal("§cFailed to levitate!"));
            return false;
        }
    }

    private boolean handleTargetMode(Level level, Player player) {
        LivingEntity target = getTargetEntity(level, player);

        if (target == null) {
            player.sendOverlayMessage(Component.literal("§cNo target found within " + (int) RAYCAST_DISTANCE + " blocks!"));
            return false;
        }

        if (applyLevitationEffects(target)) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.5F);

            spawnLevitationParticles(level, target);
            player.sendOverlayMessage(Component.literal("§bLevitating Entity: §e" + target.getName().getString()));
            return true;
        } else {
            player.sendOverlayMessage(Component.literal("§cTarget is immune to levitation!"));
            return false;
        }
    }

    private boolean applyLevitationEffects(LivingEntity target) {
        if (target == null || target.isRemoved() || !target.isAlive()) {
            return false;
        }

        if (target.isInvulnerable() || target.hasEffect(MobEffects.LEVITATION)) {
            return false;
        }

        try {
            target.removeEffect(MobEffects.LEVITATION);
            target.removeEffect(MobEffects.SLOW_FALLING);

            MobEffectInstance levitation = new MobEffectInstance(
                    MobEffects.LEVITATION,
                    LEVITATION_DURATION,
                    1,
                    false,
                    true,
                    true
            );

            MobEffectInstance slowFalling = new MobEffectInstance(
                    MobEffects.SLOW_FALLING,
                    LEVITATION_DURATION + SLOW_FALLING_BUFFER,
                    0,
                    false,
                    true,
                    true
            );

            boolean levitationApplied = target.addEffect(levitation);
            target.addEffect(slowFalling);

            return levitationApplied;

        } catch (Exception e) {
            return false;
        }
    }

    private void spawnLevitationParticles(Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 2.0;
                double offsetY = level.getRandom().nextDouble() * 2.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 2.0;

                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        entity.getX() + offsetX,
                        entity.getY() + offsetY,
                        entity.getZ() + offsetZ,
                        1, 0.0, 0.1, 0.0, 0.02
                );
            }
        }
    }

    private LivingEntity getTargetEntity(Level level, Player player) {
        if (player == null || level == null) return null;

        try {
            Vec3 start = player.getEyePosition();
            Vec3 direction = player.getLookAngle();
            Vec3 end = start.add(direction.scale(RAYCAST_DISTANCE));

            AABB searchBox = new AABB(start, end).inflate(1.0);

            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
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
        }

        return null;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
