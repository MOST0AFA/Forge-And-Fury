package dev.most0afa.forge.and.fury.Items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;

import java.util.List;

public class StormFang extends SwordItem {

    public StormFang(ToolMaterial material, Settings settings) {
        super(material, settings.maxDamage(1200).component(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                                new EntityAttributeModifier(Identifier.of("base_attack_damage"),
                                        7.0 + material.getAttackDamage(), EntityAttributeModifier.Operation.ADD_VALUE),
                                AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.GENERIC_ATTACK_SPEED,
                                new EntityAttributeModifier(Identifier.of("base_attack_speed"),
                                        -2.4, EntityAttributeModifier.Operation.ADD_VALUE),
                                AttributeModifierSlot.MAINHAND)
                        .build()));
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient && attacker instanceof PlayerEntity) {
            ServerWorld world = (ServerWorld) attacker.getWorld();

            if (world.random.nextFloat() < 0.3F) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0));

                for (int i = 0; i < 8; i++) {
                    double offsetX = world.random.nextGaussian() * 0.5;
                    double offsetY = world.random.nextDouble() * 1.5;
                    double offsetZ = world.random.nextGaussian() * 0.5;

                    world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                            target.getX() + offsetX,
                            target.getY() + offsetY,
                            target.getZ() + offsetZ,
                            1, 0, 0, 0, 0.1);
                }

                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.5F, 1.5F);
            }

            if (world.random.nextFloat() < 0.15F) {
                Vec3d targetPos = target.getPos();
                BlockPos lightningPos = BlockPos.ofFloored(targetPos.x, targetPos.y, targetPos.z);

                world.getServer().execute(() -> {
                    LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
                    lightning.refreshPositionAfterTeleport(lightningPos.getX() + 0.5, lightningPos.getY(), lightningPos.getZ() + 0.5);
                    world.spawnEntity(lightning);
                });
            }
        }

        if (attacker instanceof PlayerEntity player) {
            stack.damage(1, player, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && user.isSneaking()) {
            ServerWorld serverWorld = (ServerWorld) world;

            if (user.getItemCooldownManager().isCoolingDown(this)) {
                return TypedActionResult.pass(stack);
            }

            user.getItemCooldownManager().set(this, 100);

            Vec3d playerPos = user.getPos();
            List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class,
                    new Box(playerPos.subtract(4, 2, 4), playerPos.add(4, 2, 4)),
                    entity -> entity != user && entity.isAlive());

            int struck = 0;
            for (LivingEntity entity : nearbyEntities) {
                if (struck >= 3) break;

                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));

                Vec3d entityPos = entity.getPos();
                BlockPos strikePos = BlockPos.ofFloored(entityPos);

                for (int i = 0; i < 12; i++) {
                    double offsetX = serverWorld.random.nextGaussian() * 1.0;
                    double offsetY = serverWorld.random.nextDouble() * 2.0;
                    double offsetZ = serverWorld.random.nextGaussian() * 1.0;

                    serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                            strikePos.getX() + 0.5 + offsetX,
                            strikePos.getY() + offsetY,
                            strikePos.getZ() + 0.5 + offsetZ,
                            1, 0, 0, 0, 0.15);
                }

                serverWorld.getServer().execute(() -> {
                    LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, serverWorld);
                    lightning.refreshPositionAfterTeleport(strikePos.getX() + 0.5, strikePos.getY(), strikePos.getZ() + 0.5);
                    serverWorld.spawnEntity(lightning);
                });

                struck++;
            }

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.8F, 0.8F);

            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 0));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0));

            stack.damage(5, user, EquipmentSlot.MAINHAND);

            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantability() {
        return 18;
    }
}