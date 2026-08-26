package dev.most0afa.forge.and.fury.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TitansHammer extends AxeItem {
    private static final int EARTHQUAKE_COOLDOWN = 200;
    private static final int SHOCKWAVE_RADIUS = 4;
    private static final int EARTHQUAKE_RADIUS = 6;

    public TitansHammer(ToolMaterial material, Item.Properties properties) {
        super(material, 9.0F, -3.4F, properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        if (!level.isClientSide() && attacker instanceof Player) {
            BlockPos targetPos = target.blockPosition();
            shockwaveAttack(level, targetPos, attacker);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
            target.knockback(2.5F, attacker.getX() - target.getX(), attacker.getZ() - target.getZ(),
                    attacker.damageSources().mobAttack(attacker), 0.0F);
            createShockwaveParticles((ServerLevel) level, targetPos);
            level.playSound(null, targetPos.getX(), targetPos.getY(), targetPos.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (level.getRandom().nextFloat() < 0.15F) {
                createLightningStrike((ServerLevel) level, targetPos, attacker);
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (player.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.FAIL;
            }
            BlockPos playerPos = player.blockPosition();
            earthquakeSlam(level, playerPos, player);
            createEarthquakeParticles((ServerLevel) level, playerPos);
            level.playSound(null, playerPos.getX(), playerPos.getY(), playerPos.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.8F);
            level.playSound(null, playerPos.getX(), playerPos.getY(), playerPos.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.5F);
            player.getCooldowns().addCooldown(stack, EARTHQUAKE_COOLDOWN);
            stack.hurtAndBreak(5, player, EquipmentSlot.MAINHAND);
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 0));
        }
        return InteractionResult.SUCCESS;
    }

    private void shockwaveAttack(Level level, BlockPos pos, LivingEntity attacker) {
        if (level instanceof ServerLevel serverLevel) {
            AABB searchBox = AABB.ofSize(Vec3.atCenterOf(pos), SHOCKWAVE_RADIUS * 2, 4, SHOCKWAVE_RADIUS * 2);
            List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> entity != attacker && entity.distanceToSqr(attacker) <= SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS);
            for (LivingEntity entity : entities) {
                double distance = entity.distanceTo(attacker);
                float damageMultiplier = (float) (1.0 - (distance / SHOCKWAVE_RADIUS) * 0.5);
                entity.hurtServer(serverLevel, attacker.damageSources().mobAttack(attacker), 6.0F * damageMultiplier);
                entity.knockback(2.5F * damageMultiplier, attacker.getX() - entity.getX(), attacker.getZ() - entity.getZ(),
                        attacker.damageSources().mobAttack(attacker), 0.0F);
            }
        }
    }

    private void earthquakeSlam(Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            AABB searchBox = AABB.ofSize(Vec3.atCenterOf(pos), EARTHQUAKE_RADIUS * 2, 4, EARTHQUAKE_RADIUS * 2);
            List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> entity != player && entity.distanceToSqr(player) <= EARTHQUAKE_RADIUS * EARTHQUAKE_RADIUS);
            for (LivingEntity entity : entities) {
                double distance = entity.distanceTo(player);
                float damageMultiplier = (float) (1.0 - (distance / EARTHQUAKE_RADIUS) * 0.3);
                entity.hurtServer(serverLevel, player.damageSources().fall(), 8.0F * damageMultiplier);
                entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                entity.knockback(3.5F * damageMultiplier, player.getX() - entity.getX(), player.getZ() - entity.getZ(),
                        player.damageSources().fall(), 0.0F);
            }
        }
    }

    private void createShockwaveParticles(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * 2 * Math.PI;
            double radius = 2.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.EXPLOSION, x, pos.getY() + 0.1, z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SMOKE, x, pos.getY() + 0.1, z, 3, 0.2, 0.1, 0.2, 0.02);
        }
    }

    private void createEarthquakeParticles(ServerLevel level, BlockPos pos) {
        BlockState groundState = level.getBlockState(pos.below());
        for (int ring = 1; ring <= 3; ring++) {
            for (int i = 0; i < 20 * ring; i++) {
                double angle = (i / (20.0 * ring)) * 2 * Math.PI;
                double radius = ring * 1.5;
                double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
                level.sendParticles(ParticleTypes.EXPLOSION, x, pos.getY() + 0.1, z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, pos.getY() + 0.1, z, 2, 0.3, 0.1, 0.3, 0.05);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState), x, pos.getY() + 0.1, z, 5, 0.2, 0.1, 0.2, 0.1);
            }
        }
    }

    private void createLightningStrike(ServerLevel level, BlockPos pos, LivingEntity attacker) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 20, 0.3, 1.0, 0.3, 0.1);
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.5F);
        AABB searchBox = AABB.ofSize(Vec3.atCenterOf(pos), 6, 4, 6);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> entity != attacker && entity.distanceToSqr(Vec3.atCenterOf(pos)) <= 9);
        for (int i = 0; i < Math.min(3, nearbyEntities.size()); i++) {
            LivingEntity target = nearbyEntities.get(i);
            target.hurtServer(level, attacker.damageSources().mobAttack(attacker), 4.0F);
            Vec3 start = Vec3.atCenterOf(pos).add(0, 1, 0);
            Vec3 end = target.position().add(0, target.getBbHeight() / 2, 0);
            createLightningChain(level, start, end);
        }
    }

    private void createLightningChain(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = start.add(direction.scale(i / (double) steps));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
