package dev.most0afa.forge.and.fury.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public class FireAxe extends Item {
    private static final List<String> ABILITIES = Arrays.asList("fireball", "fire_rain", "flaming_swing");
    private static final int FIREBALL_COOLDOWN = 60;
    private static final int FIRE_RAIN_COOLDOWN = 200;
    private static final int FLAMING_SWING_COOLDOWN = 140;
    private static final int WITHER_IMMUNITY_DURATION = 300;

    public FireAxe(ToolMaterial material, Item.Properties properties) {
        super(properties.axe(material, 8.0F, -3.0F));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

            if (player.isShiftKeyDown()) {
                int currentAbilityIndex = tag.getIntOr("SelectedAbility", 0);
                currentAbilityIndex = (currentAbilityIndex + 1) % ABILITIES.size();
                tag.putInt("SelectedAbility", currentAbilityIndex);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                String abilityName = switch (ABILITIES.get(currentAbilityIndex)) {
                    case "fireball" -> "Fireball";
                    case "fire_rain" -> "Fire Rain";
                    case "flaming_swing" -> "Flaming Swing";
                    default -> "Unknown";
                };
                player.sendOverlayMessage(Component.literal("Selected ability: " + abilityName));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.2F);
            } else {
                int currentAbilityIndex = tag.getIntOr("SelectedAbility", 0);
                String selectedAbility = ABILITIES.get(currentAbilityIndex);
                String cooldownKey = selectedAbility + "_LastUsed";

                long lastUsed = tag.getLongOr(cooldownKey, 0L);
                long currentTime = level.getGameTime();
                int requiredCooldown = getAbilityCooldown(selectedAbility);

                if (currentTime - lastUsed >= requiredCooldown) {
                    performAbility(level, player, tag);
                    tag.putLong(cooldownKey, currentTime);
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    grantWitherImmunity(player);
                } else {
                    long remainingTicks = requiredCooldown - (currentTime - lastUsed);
                    float remainingSeconds = remainingTicks / 20.0f;
                    String abilityName = switch (selectedAbility) {
                        case "fireball" -> "Fireball";
                        case "fire_rain" -> "Fire Rain";
                        case "flaming_swing" -> "Flaming Swing";
                        default -> "Ability";
                    };
                    player.sendOverlayMessage(Component.literal(abilityName + " on cooldown: " + String.format("%.1f", remainingSeconds) + " seconds remaining"));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    private int getAbilityCooldown(String ability) {
        return switch (ability) {
            case "fireball" -> FIREBALL_COOLDOWN;
            case "fire_rain" -> FIRE_RAIN_COOLDOWN;
            case "flaming_swing" -> FLAMING_SWING_COOLDOWN;
            default -> FIREBALL_COOLDOWN;
        };
    }

    private void grantWitherImmunity(Player player) {
        player.removeEffect(MobEffects.WITHER);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, WITHER_IMMUNITY_DURATION, 0, false, true));
    }

    private void performAbility(Level level, Player player, CompoundTag tag) {
        int currentAbilityIndex = tag.getIntOr("SelectedAbility", 0);
        String selectedAbility = ABILITIES.get(currentAbilityIndex);

        String abilityMessage = switch (selectedAbility) {
            case "fireball" -> "Casting Fireball!";
            case "fire_rain" -> "Summoning Fire Rain!";
            case "flaming_swing" -> "Executing Flaming Swing!";
            default -> "Activating ability!";
        };
        player.sendOverlayMessage(Component.literal(abilityMessage));

        switch (selectedAbility) {
            case "fireball":
                castFireball(level, player);
                break;
            case "fire_rain":
                castFireRain(level, player);
                break;
            case "flaming_swing":
                castFlamingSwing(level, player);
                break;
        }
    }

    private void castFireball(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            SmallFireball fireball = new SmallFireball(level, player,
                    player.getLookAngle().scale(3.5));
            fireball.setPos(player.getEyePosition());

            level.addFreshEntity(fireball);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1, player.getZ(),
                    10, 0.5, 0.5, 0.5, 0.1);

            List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class,
                    AABB.ofSize(player.position(), 20, 20, 20),
                    entity -> entity != player && entity.distanceToSqr(player) <= 400);

            for (LivingEntity entity : nearbyEntities) {
                Vec3 direction = entity.position().subtract(player.position()).normalize();
                Vec3 fireballDirection = player.getLookAngle().normalize();

                if (direction.dot(fireballDirection) > 0.8) {
                    entity.hurtServer(serverLevel, level.damageSources().playerAttack(player), 12.0f);
                    entity.igniteForSeconds(8.0F);
                    entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                }
            }
        }
    }

    private void castFireRain(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookDirection = player.getLookAngle();
            Vec3 targetPos = eyePos.add(lookDirection.scale(20));

            BlockHitResult hitResult = level.clip(new ClipContext(
                    eyePos, targetPos, ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE, player));

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                targetPos = hitResult.getLocation();
            }

            final Vec3 finalTargetPos = targetPos;

            spawnMeteorWave(serverLevel, player, finalTargetPos, 8);

            serverLevel.getServer().execute(() -> spawnMeteorWave(serverLevel, player, finalTargetPos, 8));
            serverLevel.getServer().execute(() -> spawnMeteorWave(serverLevel, player, finalTargetPos, 8));
            serverLevel.getServer().execute(() -> spawnMeteorWave(serverLevel, player, finalTargetPos, 8));
            serverLevel.getServer().execute(() -> spawnMeteorWave(serverLevel, player, finalTargetPos, 8));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5F, 0.6F);
        }
    }

    private void castFlamingSwing(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            AreaEffectCloud cloud = new AreaEffectCloud(level, player.getX(), player.getY(), player.getZ());
            cloud.setRadius(6.0F);
            cloud.setDuration(80);
            cloud.setCustomParticle(ParticleTypes.FLAME);
            cloud.setOwner(player);

            level.addFreshEntity(cloud);

            List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class,
                    AABB.ofSize(player.position(), 12, 8, 12),
                    entity -> entity != player && entity.distanceToSqr(player) <= 36);

            for (LivingEntity entity : nearbyEntities) {
                entity.hurtServer(serverLevel, level.damageSources().playerAttack(player), 15.0f);
                entity.igniteForSeconds(10.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 2));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));

                Vec3 knockback = entity.position().subtract(player.position()).normalize().scale(1.5);
                entity.push(knockback.x, 0.5, knockback.z);
            }

            player.removeEffect(MobEffects.WITHER);

            BlockPos playerPos = player.blockPosition();
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    for (int y = -1; y <= 3; y++) {
                        BlockPos pos = playerPos.offset(x, y, z);
                        if (level.getBlockState(pos).isAir() &&
                                level.getBlockState(pos.below()).isSolid() &&
                                level.getRandom().nextFloat() < 0.4f) {
                            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                }
            }

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1, player.getZ(),
                    50, 3.0, 1.5, 3.0, 0.3);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 1.5F, 0.7F);
        }
    }

    private void spawnMeteorWave(ServerLevel level, Player player, Vec3 targetPos, int meteorCount) {
        for (int i = 0; i < meteorCount; i++) {
            double offsetX = (level.getRandom().nextDouble() - 0.5) * 16;
            double offsetZ = (level.getRandom().nextDouble() - 0.5) * 16;
            double x = targetPos.x + offsetX;
            double z = targetPos.z + offsetZ;
            double y = targetPos.y + 20 + (level.getRandom().nextDouble() * 10);

            SmallFireball fireball = new SmallFireball(level, player,
                    new Vec3(0, -2.5, 0));
            fireball.setPos(x, y, z);
            level.addFreshEntity(fireball);

            level.sendParticles(ParticleTypes.FLAME, x, y, z, 5, 0.2, 0.2, 0.2, 0.1);
        }

        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(targetPos, 16, 8, 16),
                entity -> entity != player);

        for (LivingEntity entity : nearbyEntities) {
            if (entity.distanceToSqr(targetPos) <= 64) {
                entity.hurtServer(level, level.damageSources().playerAttack(player), 6.0f);
                entity.igniteForSeconds(4.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            }
        }
    }
}
