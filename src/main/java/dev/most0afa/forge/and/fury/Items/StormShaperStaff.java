package dev.most0afa.forge.and.fury.Items;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.text.Text;
import net.minecraft.block.Blocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StormShaperStaff extends Item {
    private static final int COOLDOWN_TICKS = 80;
    private static final int STORM_TRIGGER_USES = 5;
    private static final int STORM_WINDOW_TICKS = 1200;
    private static final Map<PlayerEntity, List<Long>> useHistory = new HashMap<>();

    public StormShaperStaff(Settings settings) {
        super(settings.maxCount(1).maxDamage(384));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            if (user.getItemCooldownManager().isCoolingDown(this)) {
                return TypedActionResult.fail(stack);
            }

            HitResult hitResult = user.raycast(50.0, 1.0f, false);
            BlockPos targetPos;

            if (hitResult instanceof BlockHitResult blockHit) {
                targetPos = blockHit.getBlockPos().up();
            } else {
                Vec3d lookVec = user.getRotationVec(1.0f);
                Vec3d targetVec = user.getEyePos().add(lookVec.multiply(20.0));
                targetPos = BlockPos.ofFloored(targetVec);
            }

            createCastingEffects(world, user);
            summonLightning((ServerWorld) world, targetPos);
            summonAdditionalLightning((ServerWorld) world, targetPos, 4);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0F, 1.5F);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8F, 0.8F);

            stack.damage(1, user, EquipmentSlot.MAINHAND);
            user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            trackUsage(user, (ServerWorld) world);
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    private void createCastingEffects(World world, PlayerEntity user) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        for (int i = 0; i < 15; i++) {
            double angle = (i / 15.0) * 2 * Math.PI;
            double radius = 2.0 + serverWorld.random.nextDouble() * 0.5;
            double x = user.getX() + Math.cos(angle) * radius;
            double z = user.getZ() + Math.sin(angle) * radius;
            double y = user.getY() + 1.5 + serverWorld.random.nextDouble() * 0.5;

            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
        }

        for (int i = 0; i < 20; i++) {
            double t = i / 20.0 * 4 * Math.PI;
            double radius = 0.8;
            double x = user.getX() + Math.cos(t) * radius * Math.sin(t * 0.5);
            double y = user.getY() + 1.0 + (t / (4 * Math.PI)) * 1.5;
            double z = user.getZ() + Math.sin(t) * radius * Math.sin(t * 0.5);

            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    x, y, z, 1, 0, 0, 0, 0.1);
        }

        for (int i = 0; i < 8; i++) {
            double offsetX = serverWorld.random.nextGaussian() * 0.3;
            double offsetY = serverWorld.random.nextDouble() * 0.8 + 0.5;
            double offsetZ = serverWorld.random.nextGaussian() * 0.3;

            serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                    user.getX() + offsetX,
                    user.getY() + offsetY,
                    user.getZ() + offsetZ,
                    1, 0, 0, 0, 0.2);
        }
    }

    private void summonLightning(ServerWorld serverWorld, BlockPos pos) {
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, serverWorld);
        lightning.refreshPositionAfterTeleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        serverWorld.spawnEntity(lightning);

        createEnvironmentalEffects(serverWorld, pos);
        createChainLightning(serverWorld, pos);
        createBlockEffects(serverWorld, pos);
    }

    private void summonAdditionalLightning(ServerWorld serverWorld, BlockPos centerPos, int count) {
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * 2 * Math.PI;
            double radius = 8.0 + serverWorld.random.nextDouble() * 4.0;
            int offsetX = (int) Math.round(Math.cos(angle) * radius);
            int offsetZ = (int) Math.round(Math.sin(angle) * radius);
            BlockPos strikePos = centerPos.add(offsetX, 0, offsetZ);

            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, serverWorld);
            lightning.refreshPositionAfterTeleport(strikePos.getX() + 0.5, strikePos.getY(), strikePos.getZ() + 0.5);
            serverWorld.spawnEntity(lightning);
        }
    }

    private void createEnvironmentalEffects(ServerWorld serverWorld, BlockPos pos) {
        for (int i = 0; i < 50; i++) {
            double offsetX = serverWorld.random.nextGaussian() * 3.0;
            double offsetY = serverWorld.random.nextDouble() * 5.0;
            double offsetZ = serverWorld.random.nextGaussian() * 3.0;

            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0.2);
        }

        for (int i = 0; i < 20; i++) {
            double offsetX = serverWorld.random.nextGaussian() * 2.0;
            double offsetY = serverWorld.random.nextDouble() * 2.0;
            double offsetZ = serverWorld.random.nextGaussian() * 2.0;

            serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0);
        }

        serverWorld.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.BLOCKS, 1.2F, 1.0F);

        serverWorld.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.8F, 1.5F);
    }

    private void createChainLightning(ServerWorld serverWorld, BlockPos pos) {
        List<LivingEntity> nearbyEntities = serverWorld.getEntitiesByClass(LivingEntity.class,
                new Box(pos).expand(6.0), entity -> entity.isAlive());

        int affectedCount = 0;
        for (LivingEntity entity : nearbyEntities) {
            if (affectedCount >= 5) break;

            createChainLightningVisual(serverWorld, pos, entity.getBlockPos());

            float damage = 6.0F - (affectedCount * 1.0F);
            entity.damage(serverWorld.getDamageSources().lightningBolt(), damage);

            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 160, 0));

            for (int i = 0; i < 12; i++) {
                double offsetX = serverWorld.random.nextGaussian() * 0.8;
                double offsetY = serverWorld.random.nextDouble() * 2.0;
                double offsetZ = serverWorld.random.nextGaussian() * 0.8;

                serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        entity.getX() + offsetX,
                        entity.getY() + offsetY,
                        entity.getZ() + offsetZ,
                        1, 0, 0, 0, 0.15);
            }

            serverWorld.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 0.5F, 2.0F);

            affectedCount++;
        }
    }

    private void createChainLightningVisual(ServerWorld serverWorld, BlockPos start, BlockPos end) {
        Vec3d startVec = new Vec3d(start.getX() + 0.5, start.getY() + 1, start.getZ() + 0.5);
        Vec3d endVec = new Vec3d(end.getX() + 0.5, end.getY() + 1, end.getZ() + 0.5);

        int steps = (int) startVec.distanceTo(endVec) * 2;
        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            Vec3d current = startVec.lerp(endVec, progress);

            double randomX = serverWorld.random.nextGaussian() * 0.3;
            double randomY = serverWorld.random.nextGaussian() * 0.2;
            double randomZ = serverWorld.random.nextGaussian() * 0.3;

            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    current.x + randomX, current.y + randomY, current.z + randomZ,
                    1, 0, 0, 0, 0.1);
        }
    }

    private void createBlockEffects(ServerWorld serverWorld, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (serverWorld.random.nextFloat() < 0.3F) {
                        if (serverWorld.getBlockState(checkPos).isOf(Blocks.SAND)) {
                            serverWorld.setBlockState(checkPos, Blocks.GLASS.getDefaultState());
                        }
                        else if (serverWorld.getBlockState(checkPos).isOf(Blocks.DIRT)) {
                            serverWorld.setBlockState(checkPos, Blocks.COARSE_DIRT.getDefaultState());
                        }
                        else if (serverWorld.getBlockState(checkPos).isOf(Blocks.STONE)) {
                            serverWorld.setBlockState(checkPos, Blocks.CRACKED_STONE_BRICKS.getDefaultState());
                        }
                    }
                }
            }
        }

        if (serverWorld.random.nextFloat() < 0.5F) {
            BlockPos belowPos = pos.down();
            if (serverWorld.getBlockState(belowPos).getBlock().getBlastResistance() < 10.0F) {
                serverWorld.breakBlock(belowPos, true);
            }
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantability() {
        return 15;
    }

    private void trackUsage(PlayerEntity player, ServerWorld world) {
        long currentTime = world.getTime();
        useHistory.computeIfAbsent(player, k -> new java.util.ArrayList<>()).add(currentTime);

        List<Long> history = useHistory.get(player);
        history.removeIf(time -> currentTime - time > STORM_WINDOW_TICKS);

        if (history.size() >= STORM_TRIGGER_USES) {
            world.setWeather(0, 6000, true, true);

            HitResult hitResult = player.raycast(50.0, 1.0f, false);
            BlockPos centerPos;

            if (hitResult instanceof BlockHitResult blockHit) {
                centerPos = blockHit.getBlockPos().up();
            } else {
                Vec3d lookVec = player.getRotationVec(1.0f);
                Vec3d targetVec = player.getEyePos().add(lookVec.multiply(20.0));
                centerPos = BlockPos.ofFloored(targetVec);
            }

            summonAdditionalLightning(world, centerPos, 8);
            player.sendMessage(Text.of("⛈️ You sense a storm brewing in the distance..."), true);

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