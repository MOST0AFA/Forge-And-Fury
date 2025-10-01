package dev.most0afa.forge.and.fury.Items;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

public class TitansHammer extends AxeItem {
    private static final int EARTHQUAKE_COOLDOWN = 200;
    private static final int SHOCKWAVE_RADIUS = 4;
    private static final int EARTHQUAKE_RADIUS = 6;

    public TitansHammer(ToolMaterial material, Settings settings) {
        super(material, settings.attributeModifiers(AxeItem.createAttributeModifiers(material, 9.0F, -3.4F)));
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        World world = attacker.getWorld();
        if (!world.isClient && attacker instanceof PlayerEntity) {
            BlockPos targetPos = target.getBlockPos();
            shockwaveAttack(world, targetPos, attacker);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1));
            target.takeKnockback(2.5F, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            createShockwaveParticles((ServerWorld) world, targetPos);
            world.playSound(null, targetPos.getX(), targetPos.getY(), targetPos.getZ(), SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 1.0F, 1.0F);
            stack.damage(1, attacker, EquipmentSlot.MAINHAND);
            if (world.random.nextFloat() < 0.15F) {
                createLightningStrike((ServerWorld) world, targetPos, attacker);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient) {
            if (player.getItemCooldownManager().isCoolingDown(this)) {
                return TypedActionResult.fail(stack);
            }
            BlockPos playerPos = player.getBlockPos();
            earthquakeSlam(world, playerPos, player);
            createEarthquakeParticles((ServerWorld) world, playerPos);
            world.playSound(null, playerPos.getX(), playerPos.getY(), playerPos.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5F, 0.8F);
            world.playSound(null, playerPos.getX(), playerPos.getY(), playerPos.getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0F, 0.5F);
            player.getItemCooldownManager().set(this, EARTHQUAKE_COOLDOWN);
            stack.damage(5, player, EquipmentSlot.MAINHAND);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 0));
        }
        return TypedActionResult.success(stack);
    }

    private void shockwaveAttack(World world, BlockPos pos, LivingEntity attacker) {
        if (world instanceof ServerWorld serverWorld) {
            Box searchBox = Box.of(Vec3d.ofCenter(pos), SHOCKWAVE_RADIUS * 2, 4, SHOCKWAVE_RADIUS * 2);
            List<LivingEntity> entities = serverWorld.getEntitiesByClass(LivingEntity.class, searchBox, entity -> entity != attacker && entity.squaredDistanceTo(attacker) <= SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS);
            for (LivingEntity entity : entities) {
                double distance = entity.distanceTo(attacker);
                float damageMultiplier = (float) (1.0 - (distance / SHOCKWAVE_RADIUS) * 0.5);
                entity.damage(attacker.getDamageSources().mobAttack(attacker), 6.0F * damageMultiplier);
                entity.takeKnockback(2.5F * damageMultiplier, attacker.getX() - entity.getX(), attacker.getZ() - entity.getZ());
            }
        }
    }

    private void earthquakeSlam(World world, BlockPos pos, PlayerEntity player) {
        if (world instanceof ServerWorld serverWorld) {
            Box searchBox = Box.of(Vec3d.ofCenter(pos), EARTHQUAKE_RADIUS * 2, 4, EARTHQUAKE_RADIUS * 2);
            List<LivingEntity> entities = serverWorld.getEntitiesByClass(LivingEntity.class, searchBox, entity -> entity != player && entity.squaredDistanceTo(player) <= EARTHQUAKE_RADIUS * EARTHQUAKE_RADIUS);
            for (LivingEntity entity : entities) {
                double distance = entity.distanceTo(player);
                float damageMultiplier = (float) (1.0 - (distance / EARTHQUAKE_RADIUS) * 0.3);
                entity.damage(player.getDamageSources().fall(), 8.0F * damageMultiplier);
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0));
                entity.takeKnockback(3.5F * damageMultiplier, player.getX() - entity.getX(), player.getZ() - entity.getZ());
            }
        }
    }

    private void createShockwaveParticles(ServerWorld world, BlockPos pos) {
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * 2 * Math.PI;
            double radius = 2.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.EXPLOSION, x, pos.getY() + 0.1, z, 1, 0, 0, 0, 0);
            world.spawnParticles(ParticleTypes.SMOKE, x, pos.getY() + 0.1, z, 3, 0.2, 0.1, 0.2, 0.02);
        }
    }

    private void createEarthquakeParticles(ServerWorld world, BlockPos pos) {
        BlockState groundState = world.getBlockState(pos.down());
        for (int ring = 1; ring <= 3; ring++) {
            for (int i = 0; i < 20 * ring; i++) {
                double angle = (i / (20.0 * ring)) * 2 * Math.PI;
                double radius = ring * 1.5;
                double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
                world.spawnParticles(ParticleTypes.EXPLOSION, x, pos.getY() + 0.1, z, 1, 0, 0, 0, 0);
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, pos.getY() + 0.1, z, 2, 0.3, 0.1, 0.3, 0.05);
                world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, groundState), x, pos.getY() + 0.1, z, 5, 0.2, 0.1, 0.2, 0.1);
            }
        }
    }

    private void createLightningStrike(ServerWorld world, BlockPos pos, LivingEntity attacker) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 20, 0.3, 1.0, 0.3, 0.1);
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5F, 1.5F);
        Box searchBox = Box.of(Vec3d.ofCenter(pos), 6, 4, 6);
        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class, searchBox, entity -> entity != attacker && entity.squaredDistanceTo(Vec3d.ofCenter(pos)) <= 9);
        for (int i = 0; i < Math.min(3, nearbyEntities.size()); i++) {
            LivingEntity target = nearbyEntities.get(i);
            target.damage(attacker.getDamageSources().mobAttack(attacker), 4.0F);
            Vec3d start = Vec3d.ofCenter(pos).add(0, 1, 0);
            Vec3d end = target.getPos().add(0, target.getHeight() / 2, 0);
            createLightningChain(world, start, end);
        }
    }

    private void createLightningChain(ServerWorld world, Vec3d start, Vec3d end) {
        Vec3d direction = end.subtract(start);
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            Vec3d pos = start.add(direction.multiply(i / (double) steps));
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}