package dev.most0afa.forge.and.fury.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StormShaperStaff extends Item {
    private static final int COOLDOWN_TICKS = 80;
    private static final int STORM_TRIGGER_USES = 5;
    private static final int STORM_WINDOW_TICKS = 1200;
    private static final Map<Player, List<Long>> useHistory = new HashMap<>();

    public StormShaperStaff(Item.Properties properties) {
        super(properties.durability(384).enchantable(15));
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (user.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.FAIL;
            }

            BlockPos targetPos = raycastTargetPos(level, user);

            createCastingEffects(level, user);
            summonLightning((ServerLevel) level, targetPos);
            summonAdditionalLightning((ServerLevel) level, targetPos, 4);

            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.5F);

            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 0.8F);

            stack.hurtAndBreak(1, user, EquipmentSlot.MAINHAND);
            user.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            trackUsage(user, (ServerLevel) level);
        }

        return InteractionResult.SUCCESS;
    }

    private BlockPos raycastTargetPos(Level level, Player user) {
        Vec3 eyePos = user.getEyePosition();
        Vec3 lookVec = user.getLookAngle();
        Vec3 endVec = eyePos.add(lookVec.scale(50.0));

        BlockHitResult hitResult = level.clip(new ClipContext(eyePos, endVec,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, user));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos().above();
        }
        return BlockPos.containing(endVec);
    }

    private void createCastingEffects(Level level, Player user) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        for (int i = 0; i < 15; i++) {
            double angle = (i / 15.0) * 2 * Math.PI;
            double radius = 2.0 + serverLevel.getRandom().nextDouble() * 0.5;
            double x = user.getX() + Math.cos(angle) * radius;
            double z = user.getZ() + Math.sin(angle) * radius;
            double y = user.getY() + 1.5 + serverLevel.getRandom().nextDouble() * 0.5;

            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
        }

        for (int i = 0; i < 20; i++) {
            double t = i / 20.0 * 4 * Math.PI;
            double radius = 0.8;
            double x = user.getX() + Math.cos(t) * radius * Math.sin(t * 0.5);
            double y = user.getY() + 1.0 + (t / (4 * Math.PI)) * 1.5;
            double z = user.getZ() + Math.sin(t) * radius * Math.sin(t * 0.5);

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    x, y, z, 1, 0, 0, 0, 0.1);
        }

        for (int i = 0; i < 8; i++) {
            double offsetX = serverLevel.getRandom().nextGaussian() * 0.3;
            double offsetY = serverLevel.getRandom().nextDouble() * 0.8 + 0.5;
            double offsetZ = serverLevel.getRandom().nextGaussian() * 0.3;

            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    user.getX() + offsetX,
                    user.getY() + offsetY,
                    user.getZ() + offsetZ,
                    1, 0, 0, 0, 0.2);
        }
    }

    private void summonLightning(ServerLevel serverLevel, BlockPos pos) {
        LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (lightning != null) {
            lightning.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            serverLevel.addFreshEntity(lightning);
        }

        createEnvironmentalEffects(serverLevel, pos);
        createChainLightning(serverLevel, pos);
        createBlockEffects(serverLevel, pos);
    }

    private void summonAdditionalLightning(ServerLevel serverLevel, BlockPos centerPos, int count) {
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * 2 * Math.PI;
            double radius = 8.0 + serverLevel.getRandom().nextDouble() * 4.0;
            int offsetX = (int) Math.round(Math.cos(angle) * radius);
            int offsetZ = (int) Math.round(Math.sin(angle) * radius);
            BlockPos strikePos = centerPos.offset(offsetX, 0, offsetZ);

            LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
            if (lightning != null) {
                lightning.snapTo(strikePos.getX() + 0.5, strikePos.getY(), strikePos.getZ() + 0.5);
                serverLevel.addFreshEntity(lightning);
            }
        }
    }

    private void createEnvironmentalEffects(ServerLevel serverLevel, BlockPos pos) {
        for (int i = 0; i < 50; i++) {
            double offsetX = serverLevel.getRandom().nextGaussian() * 3.0;
            double offsetY = serverLevel.getRandom().nextDouble() * 5.0;
            double offsetZ = serverLevel.getRandom().nextGaussian() * 3.0;

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0.2);
        }

        for (int i = 0; i < 20; i++) {
            double offsetX = serverLevel.getRandom().nextGaussian() * 2.0;
            double offsetY = serverLevel.getRandom().nextDouble() * 2.0;
            double offsetZ = serverLevel.getRandom().nextGaussian() * 2.0;

            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0);
        }

        serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 1.2F, 1.0F);

        serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.8F, 1.5F);
    }

    private void createChainLightning(ServerLevel serverLevel, BlockPos pos) {
        List<LivingEntity> nearbyEntities = serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(pos).inflate(6.0), LivingEntity::isAlive);

        int affectedCount = 0;
        for (LivingEntity entity : nearbyEntities) {
            if (affectedCount >= 5) break;

            createChainLightningVisual(serverLevel, pos, entity.blockPosition());

            float damage = 6.0F - (affectedCount * 1.0F);
            entity.hurtServer(serverLevel, serverLevel.damageSources().lightningBolt(), damage);

            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));

            for (int i = 0; i < 12; i++) {
                double offsetX = serverLevel.getRandom().nextGaussian() * 0.8;
                double offsetY = serverLevel.getRandom().nextDouble() * 2.0;
                double offsetZ = serverLevel.getRandom().nextGaussian() * 0.8;

                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        entity.getX() + offsetX,
                        entity.getY() + offsetY,
                        entity.getZ() + offsetZ,
                        1, 0, 0, 0, 0.15);
            }

            serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.5F, 2.0F);

            affectedCount++;
        }
    }

    private void createChainLightningVisual(ServerLevel serverLevel, BlockPos start, BlockPos end) {
        Vec3 startVec = new Vec3(start.getX() + 0.5, start.getY() + 1, start.getZ() + 0.5);
        Vec3 endVec = new Vec3(end.getX() + 0.5, end.getY() + 1, end.getZ() + 0.5);

        int steps = (int) startVec.distanceTo(endVec) * 2;
        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            Vec3 current = startVec.lerp(endVec, progress);

            double randomX = serverLevel.getRandom().nextGaussian() * 0.3;
            double randomY = serverLevel.getRandom().nextGaussian() * 0.2;
            double randomZ = serverLevel.getRandom().nextGaussian() * 0.3;

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    current.x + randomX, current.y + randomY, current.z + randomZ,
                    1, 0, 0, 0, 0.1);
        }
    }

    private void createBlockEffects(ServerLevel serverLevel, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (serverLevel.getRandom().nextFloat() < 0.3F) {
                        if (serverLevel.getBlockState(checkPos).is(Blocks.SAND)) {
                            serverLevel.setBlock(checkPos, Blocks.GLASS.defaultBlockState(), 3);
                        } else if (serverLevel.getBlockState(checkPos).is(Blocks.DIRT)) {
                            serverLevel.setBlock(checkPos, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                        } else if (serverLevel.getBlockState(checkPos).is(Blocks.STONE)) {
                            serverLevel.setBlock(checkPos, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        if (serverLevel.getRandom().nextFloat() < 0.5F) {
            BlockPos belowPos = pos.below();
            if (serverLevel.getBlockState(belowPos).getBlock().getExplosionResistance() < 10.0F) {
                serverLevel.destroyBlock(belowPos, true, null, 512);
            }
        }
    }

    private void trackUsage(Player player, ServerLevel level) {
        long currentTime = level.getGameTime();
        useHistory.computeIfAbsent(player, k -> new java.util.ArrayList<>()).add(currentTime);

        List<Long> history = useHistory.get(player);
        history.removeIf(time -> currentTime - time > STORM_WINDOW_TICKS);

        if (history.size() >= STORM_TRIGGER_USES) {
            level.getServer().setWeatherParameters(0, 6000, true, true);

            BlockPos centerPos = raycastTargetPos(level, player);

            summonAdditionalLightning(level, centerPos, 8);
            player.sendOverlayMessage(Component.literal("A storm brews in the distance..."));

            history.clear();
        }

        cleanupOldEntries();
    }

    private void cleanupOldEntries() {
        if (useHistory.size() > 100) {
            useHistory.entrySet().removeIf(entry ->
                    entry.getKey() == null || !entry.getKey().isAlive());
        }
    }
}
