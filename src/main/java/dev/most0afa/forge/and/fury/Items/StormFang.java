package dev.most0afa.forge.and.fury.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class StormFang extends Item {

    public StormFang(ToolMaterial material, Item.Properties properties) {
        super(properties.sword(material, 7.0F, -2.4F).durability(1200).enchantable(18));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof Player) {
            ServerLevel level = (ServerLevel) attacker.level();

            if (level.getRandom().nextFloat() < 0.3F) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));

                for (int i = 0; i < 8; i++) {
                    double offsetX = level.getRandom().nextGaussian() * 0.5;
                    double offsetY = level.getRandom().nextDouble() * 1.5;
                    double offsetZ = level.getRandom().nextGaussian() * 0.5;

                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            target.getX() + offsetX,
                            target.getY() + offsetY,
                            target.getZ() + offsetZ,
                            1, 0, 0, 0, 0.1);
                }

                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 1.5F);
            }

            if (level.getRandom().nextFloat() < 0.15F) {
                Vec3 targetPos = target.position();
                BlockPos lightningPos = BlockPos.containing(targetPos.x, targetPos.y, targetPos.z);

                level.getServer().execute(() -> {
                    LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                    if (lightning != null) {
                        lightning.snapTo(lightningPos.getX() + 0.5, lightningPos.getY(), lightningPos.getZ() + 0.5);
                        level.addFreshEntity(lightning);
                    }
                });
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!level.isClientSide() && user.isShiftKeyDown()) {
            ServerLevel serverLevel = (ServerLevel) level;

            if (user.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.PASS;
            }

            user.getCooldowns().addCooldown(stack, 100);

            Vec3 playerPos = user.position();
            List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(playerPos.subtract(4, 2, 4), playerPos.add(4, 2, 4)),
                    entity -> entity != user && entity.isAlive());

            int struck = 0;
            for (LivingEntity entity : nearbyEntities) {
                if (struck >= 3) break;

                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));

                Vec3 entityPos = entity.position();
                BlockPos strikePos = BlockPos.containing(entityPos);

                for (int i = 0; i < 12; i++) {
                    double offsetX = serverLevel.getRandom().nextGaussian() * 1.0;
                    double offsetY = serverLevel.getRandom().nextDouble() * 2.0;
                    double offsetZ = serverLevel.getRandom().nextGaussian() * 1.0;

                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            strikePos.getX() + 0.5 + offsetX,
                            strikePos.getY() + offsetY,
                            strikePos.getZ() + 0.5 + offsetZ,
                            1, 0, 0, 0, 0.15);
                }

                serverLevel.getServer().execute(() -> {
                    LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
                    if (lightning != null) {
                        lightning.snapTo(strikePos.getX() + 0.5, strikePos.getY(), strikePos.getZ() + 0.5);
                        serverLevel.addFreshEntity(lightning);
                    }
                });

                struck++;
            }

            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 0.8F);

            user.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0));
            user.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0));

            stack.hurtAndBreak(5, user, EquipmentSlot.MAINHAND);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
