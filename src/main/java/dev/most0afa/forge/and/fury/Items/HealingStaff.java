package dev.most0afa.forge.and.fury.Items;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class HealingStaff extends Item {
    private static final int COOLDOWN_TICKS = 30;
    private static final float HEAL_AMOUNT = 3.0F;

    public HealingStaff(Item.Properties properties) {
        super(properties.durability(250)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(
                                        Identifier.fromNamespaceAndPath("forgeandfury", "healing_staff_attack_damage"),
                                        2.0,
                                        AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build()));
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            LivingEntity target = getTargetEntity(level, user);

            if (target != null && target != user) {
                healEntity(target, level);
                user.sendOverlayMessage(Component.literal("Healed " + target.getName().getString()));

                if (level instanceof ServerLevel serverLevel) {
                    spawnHealingParticles(serverLevel, target);
                }
            } else {
                healEntity(user, level);
                user.sendOverlayMessage(Component.literal("Self-heal activated"));

                if (level instanceof ServerLevel serverLevel) {
                    spawnHealingParticles(serverLevel, user);
                }
            }

            stack.hurtAndBreak(1, user, EquipmentSlot.MAINHAND);
            user.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);

            level.playSound(null, user.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 1.0F, 1.5F);
        }

        return InteractionResult.SUCCESS;
    }

    private void healEntity(LivingEntity entity, Level level) {
        entity.heal(HEAL_AMOUNT);

        if (level.getRandom().nextFloat() < 0.3f) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
        }

        if (entity.isOnFire()) {
            entity.clearFire();
        }

        removeBadStatusEffects(entity);
    }

    private void removeBadStatusEffects(LivingEntity entity) {
        List<Holder<MobEffect>> effectsToRemove = new ArrayList<>();

        for (MobEffectInstance effectInstance : entity.getActiveEffects()) {
            Holder<MobEffect> effectType = effectInstance.getEffect();

            if (!effectType.value().isBeneficial()) {
                effectsToRemove.add(effectType);
            }
        }

        for (Holder<MobEffect> effectType : effectsToRemove) {
            entity.removeEffect(effectType);
        }
    }

    private void spawnHealingParticles(ServerLevel level, LivingEntity entity) {
        Vec3 pos = entity.position().add(0, entity.getBbHeight() / 2, 0);

        for (int i = 0; i < 10; i++) {
            double offsetX = (level.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetY = (level.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetZ = (level.getRandom().nextDouble() - 0.5) * 2.0;

            level.sendParticles(ParticleTypes.HEART,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    1, 0, 0.1, 0, 0.05);
        }
    }

    private LivingEntity getTargetEntity(Level level, Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        double range = 5.0;
        LivingEntity closestEntity = null;
        double closestDistance = range;

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), e -> e != player)) {

            Vec3 toEntity = entity.position().subtract(start);
            Vec3 normalizedDirection = direction.normalize();
            double dot = toEntity.normalize().dot(normalizedDirection);

            if (dot > 0.8) {
                double distance = start.distanceTo(entity.position());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }
        }

        return closestEntity;
    }
}
