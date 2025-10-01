package dev.most0afa.forge.and.fury.Items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class HealingStaff extends Item {
    private static final int COOLDOWN_TICKS = 30;
    private static final float HEAL_AMOUNT = 3.0F;

    public HealingStaff(Settings settings) {
        super(settings.maxDamage(250)
                .component(DataComponentTypes.ATTRIBUTE_MODIFIERS, createAttributeModifiers()));
    }

    private static AttributeModifiersComponent createAttributeModifiers() {
        return AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier(
                                Identifier.of("healing_staff", "attack_damage"),
                                2.0,
                                EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                .build();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            LivingEntity target = getTargetEntity(world, user);

            if (target != null && target != user) {
                healEntity(target, world);
                user.sendMessage(Text.of("Healed " + target.getName().getString()), true);

                if (world instanceof ServerWorld serverWorld) {
                    spawnHealingParticles(serverWorld, target);
                }
            } else {
                healEntity(user, world);
                user.sendMessage(Text.of("Self-heal activated"), true);

                if (world instanceof ServerWorld serverWorld) {
                    spawnHealingParticles(serverWorld, user);
                }
            }

            stack.damage(1, user, EquipmentSlot.MAINHAND);
            user.getItemCooldownManager().set(this, COOLDOWN_TICKS);

            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS, 1.0F, 1.5F);
        }

        return TypedActionResult.success(stack);
    }

    private void healEntity(LivingEntity entity, World world) {
        entity.heal(HEAL_AMOUNT);

        if (world.random.nextFloat() < 0.3f) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0));
        }

        if (entity.isOnFire()) {
            entity.extinguish();
        }

        removeBadStatusEffects(entity);
    }

    private void removeBadStatusEffects(LivingEntity entity) {
        List<RegistryEntry<StatusEffect>> effectsToRemove = new ArrayList<>();

        for (StatusEffectInstance effectInstance : entity.getStatusEffects()) {
            RegistryEntry<StatusEffect> effectType = effectInstance.getEffectType();

            if (!effectType.value().isBeneficial()) {
                effectsToRemove.add(effectType);
            }
        }

        for (RegistryEntry<StatusEffect> effectType : effectsToRemove) {
            entity.removeStatusEffect(effectType);
        }
    }

    private void spawnHealingParticles(ServerWorld world, LivingEntity entity) {
        Vec3d pos = entity.getPos().add(0, entity.getHeight() / 2, 0);

        for (int i = 0; i < 10; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 2.0;
            double offsetY = (world.random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (world.random.nextDouble() - 0.5) * 2.0;

            world.spawnParticles(ParticleTypes.HEART,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    1, 0, 0.1, 0, 0.05);
        }
    }

    private LivingEntity getTargetEntity(World world, PlayerEntity player) {
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        double range = 5.0;
        LivingEntity closestEntity = null;
        double closestDistance = range;

        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(range), e -> e != player)) {

            Vec3d toEntity = entity.getPos().subtract(start);
            Vec3d normalizedDirection = direction.normalize();
            double dot = toEntity.normalize().dotProduct(normalizedDirection);

            if (dot > 0.8) {
                double distance = start.distanceTo(entity.getPos());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }
        }

        return closestEntity;
    }
}