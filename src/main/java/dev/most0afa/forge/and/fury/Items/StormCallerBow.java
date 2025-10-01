package dev.most0afa.forge.and.fury.Items;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Box;

public class StormCallerBow extends BowItem {
    private static final Map<UUID, StormArrowData> stormArrows = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> recentLightningStrikes = new ConcurrentHashMap<>();
    private static final long LIGHTNING_COOLDOWN = 5000;

    public StormCallerBow(Settings settings) {
        super(settings.maxCount(1).maxDamage(512));
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(Items.PRISMARINE_CRYSTALS) || super.canRepair(stack, ingredient);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantability() {
        return 25;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof PlayerEntity player) {
            boolean hasAmmo = !player.getProjectileType(stack).isEmpty();
            ItemStack arrowStack = hasAmmo ? player.getProjectileType(stack) : new ItemStack(Items.ARROW);

            if (!world.isClient) {
                int useTicks = this.getMaxUseTime(stack, user) - remainingUseTicks;
                float power = getPullProgress(useTicks);

                if (power >= 0.1F) {
                    var registryManager = world.getRegistryManager();
                    var enchantmentRegistry = registryManager.get(RegistryKeys.ENCHANTMENT);

                    var powerEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "power"));
                    var flameEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "flame"));
                    var infinityEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "infinity"));
                    var unbreakingEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "unbreaking"));
                    var mendingEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "mending"));
                    var multishot = enchantmentRegistry.getEntry(Identifier.of("minecraft", "multishot"));

                    int powerLevel = powerEntry.map(entry -> EnchantmentHelper.getLevel(entry, stack)).orElse(0);
                    int flameLevel = flameEntry.map(entry -> EnchantmentHelper.getLevel(entry, stack)).orElse(0);
                    boolean hasInfinity = infinityEntry.isPresent() && EnchantmentHelper.getLevel(infinityEntry.get(), stack) > 0;
                    int unbreakingLevel = unbreakingEntry.map(entry -> EnchantmentHelper.getLevel(entry, stack)).orElse(0);
                    boolean hasMending = mendingEntry.isPresent() && EnchantmentHelper.getLevel(mendingEntry.get(), stack) > 0;
                    boolean hasMultishot = multishot.isPresent() && EnchantmentHelper.getLevel(multishot.get(), stack) > 0;

                    boolean consumeArrow = !hasInfinity || !player.getAbilities().creativeMode;
                    if (!consumeArrow || hasAmmo || player.getAbilities().creativeMode) {

                        int arrowCount = hasMultishot ? 3 : 1;
                        float[] yawOffsets = hasMultishot ? new float[]{-10.0F, 0.0F, 10.0F} : new float[]{0.0F};

                        for (int i = 0; i < arrowCount; i++) {
                            ArrowEntity arrow = new ArrowEntity(EntityType.ARROW, world);
                            arrow.setOwner(player);
                            arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());

                            float yawOffset = arrowCount > 1 ? yawOffsets[i] : 0.0F;
                            arrow.setVelocity(player, player.getPitch(), player.getYaw() + yawOffset, 0.0F,
                                    power * 3.0F + (powerLevel * 0.5F), 1.0F);

                            if (power == 1.0F) {
                                arrow.setCritical(true);
                            }

                            double baseDamage = 4.0;
                            double powerBonus = powerLevel * 1.5;
                            arrow.setDamage(baseDamage + powerBonus);

                            if (flameLevel > 0) {
                                arrow.setOnFireFor(100 + (flameLevel * 20));
                            }

                            StormArrowData data = new StormArrowData(powerLevel, power, hasMultishot);
                            stormArrows.put(arrow.getUuid(), data);

                            if (power >= 0.8F && world.random.nextFloat() < 0.2F) {
                                data.hasHomingEffect = true;
                            }

                            world.spawnEntity(arrow);
                        }

                        if (world instanceof ServerWorld serverWorld) {
                            createShootingEffects(serverWorld, player, power, powerLevel, hasMultishot);
                        }

                        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F,
                                1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
                        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.3F + (powerLevel * 0.1F), 2.0F);

                        if (consumeArrow && hasAmmo) {
                            arrowStack.decrement(1);
                            if (arrowStack.isEmpty()) {
                                player.getInventory().removeOne(arrowStack);
                            }
                        }

                        int damageAmount = 1;
                        if (unbreakingLevel > 0) {
                            if (world.random.nextInt(unbreakingLevel + 1) == 0) {
                                stack.damage(damageAmount, player, EquipmentSlot.MAINHAND);
                            }
                        } else {
                            stack.damage(damageAmount, player, EquipmentSlot.MAINHAND);
                        }

                        if (hasMending && player.experienceLevel > 0) {
                            int repairAmount = Math.min(stack.getDamage(), 2);
                            if (repairAmount > 0) {
                                stack.setDamage(stack.getDamage() - repairAmount);
                                player.addExperience(-1);
                            }
                        }
                    }
                }
            }
        }
    }

    private void createShootingEffects(ServerWorld world, PlayerEntity player, float power, int powerLevel, boolean multishot) {
        Vec3d playerPos = player.getPos();

        int sparkCount = Math.min(multishot ? 8 : 5, 5 + powerLevel);

        for (int i = 0; i < sparkCount; i++) {
            double offsetX = world.random.nextGaussian() * 0.8;
            double offsetY = world.random.nextDouble() * 1.0 + 0.5;
            double offsetZ = world.random.nextGaussian() * 0.8;

            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    playerPos.x + offsetX, playerPos.y + offsetY, playerPos.z + offsetZ,
                    1, 0, 0, 0, 0.05);
        }

        if (power >= 0.8F && world.isThundering() && world.random.nextBoolean()) {
            int enchantParticles = Math.min(3 + powerLevel, 8);
            for (int i = 0; i < enchantParticles; i++) {
                world.spawnParticles(ParticleTypes.ENCHANT,
                        playerPos.x, playerPos.y + 1, playerPos.z,
                        1, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    public static void handleArrowTick(ArrowEntity arrow) {
        UUID arrowId = arrow.getUuid();
        StormArrowData data = stormArrows.get(arrowId);

        if (data != null && arrow.getWorld() instanceof ServerWorld serverWorld && !data.isGrounded) {
            if (arrow.age % 4 != 0) return;

            Vec3d pos = arrow.getPos();

            if (serverWorld.random.nextFloat() < 0.1F) {
                serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
            }

            if (data.hasHomingEffect && arrow.age % 8 == 0) {
                updateHomingTrajectory(arrow, serverWorld);
            }
        }
    }

    public static void handleArrowImpact(ArrowEntity arrow, BlockPos impactPos) {
        UUID arrowId = arrow.getUuid();
        StormArrowData data = stormArrows.get(arrowId);

        if (data != null && !data.hasLanded) {
            data.hasLanded = true;
            data.isGrounded = true;

            if (arrow.getWorld() instanceof ServerWorld serverWorld) {
                long currentTime = System.currentTimeMillis();
                Long lastStrike = recentLightningStrikes.get(impactPos);
                if (lastStrike != null && currentTime - lastStrike < LIGHTNING_COOLDOWN) {
                    return;
                }
                recentLightningStrikes.put(impactPos, currentTime);

                cleanupOldStrikes(currentTime);

                createStormEffects(serverWorld, impactPos, data);

                if (data.drawPower >= 0.7F) {
                    createElectricField(serverWorld, impactPos, data);
                }

                summonLightningStrike(serverWorld, impactPos, data);
            }
        }
    }

    private static void cleanupOldStrikes(long currentTime) {
        recentLightningStrikes.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > LIGHTNING_COOLDOWN);
    }

    private static void createStormEffects(ServerWorld world, BlockPos pos, StormArrowData data) {
        int sparkCount = Math.min(8 + (data.lightningPower * 2), 15);
        for (int i = 0; i < sparkCount; i++) {
            double offsetX = world.random.nextGaussian() * 1.0;
            double offsetY = world.random.nextDouble() * 1.5;
            double offsetZ = world.random.nextGaussian() * 1.0;

            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0.05);
        }

        if (world.random.nextBoolean()) {
            int cloudCount = Math.min(4 + data.lightningPower, 8);
            for (int i = 0; i < cloudCount; i++) {
                double offsetX = world.random.nextGaussian() * 0.8;
                double offsetZ = world.random.nextGaussian() * 0.8;

                world.spawnParticles(ParticleTypes.CLOUD,
                        pos.getX() + 0.5 + offsetX,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5 + offsetZ,
                        1, 0, 0.05, 0, 0.01);
            }
        }
    }

    private static void createElectricField(ServerWorld world, BlockPos center, StormArrowData data) {
        int fieldSize = Math.min(4 + data.lightningPower, 8);
        for (int i = 0; i < fieldSize; i++) {
            double angle = (i / (double)fieldSize) * 2 * Math.PI;
            double radius = 1.5 + (data.lightningPower * 0.2);
            double x = center.getX() + 0.5 + Math.cos(angle) * radius;
            double z = center.getZ() + 0.5 + Math.sin(angle) * radius;

            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    x, center.getY() + 0.5, z, 1, 0.05, 0.05, 0.05, 0.01);
        }

        world.playSound(null, center.getX(), center.getY(), center.getZ(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS,
                0.3F + (data.lightningPower * 0.1F), 2.0F);
    }

    private static void summonLightningStrike(ServerWorld world, BlockPos pos, StormArrowData data) {
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        lightning.refreshPositionAfterTeleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        world.spawnEntity(lightning);

        createElectromagneticPulse(world, pos, data);

        if (data.lightningPower > 1 && world.random.nextFloat() < 0.15F) {
            int chainCount = Math.min(data.lightningPower - 1, 2);
            for (int i = 0; i < chainCount; i++) {
                int offsetX = world.random.nextInt(3) - 1;
                int offsetZ = world.random.nextInt(3) - 1;
                BlockPos chainPos = pos.add(offsetX, 0, offsetZ);

                LightningEntity chainLightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
                chainLightning.refreshPositionAfterTeleport(chainPos.getX() + 0.5, chainPos.getY(), chainPos.getZ() + 0.5);
                world.spawnEntity(chainLightning);
            }
        }

        if (data.drawPower >= 1.0F && data.lightningPower >= 3 && world.isThundering()) {
            createThunderClap(world, pos, data);
        }
    }

    private static void createThunderClap(ServerWorld world, BlockPos center, StormArrowData data) {
        float volume = Math.min(1.5F + (data.lightningPower * 0.2F), 3.0F);
        world.playSound(null, center.getX(), center.getY(), center.getZ(),
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, volume, 0.8F);

        int cloudCount = Math.min(10 + (data.lightningPower * 2), 20);
        for (int i = 0; i < cloudCount; i++) {
            double angle = world.random.nextDouble() * 2 * Math.PI;
            double distance = world.random.nextDouble() * (2.0 + data.lightningPower * 0.5);
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;

            world.spawnParticles(ParticleTypes.CLOUD,
                    x, center.getY() + 1, z, 1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    public static void cleanupArrowData(UUID arrowId) {
        stormArrows.remove(arrowId);
    }

    private static class StormArrowData {
        public final int lightningPower;
        public final float drawPower;
        public final boolean multishot;
        public boolean hasLanded = false;
        public boolean isGrounded = false;
        public boolean hasHomingEffect = false;

        public StormArrowData(int lightningPower, float drawPower, boolean multishot) {
            this.lightningPower = lightningPower;
            this.drawPower = drawPower;
            this.multishot = multishot;
        }
    }

    private static void updateHomingTrajectory(ArrowEntity arrow, ServerWorld world) {
        Vec3d arrowPos = arrow.getPos();
        Box searchBox = Box.of(arrowPos, 8, 8, 8);

        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class, searchBox,
                entity -> entity != arrow.getOwner() && entity.squaredDistanceTo(arrowPos) <= 32.0);

        if (!nearbyEntities.isEmpty()) {
            LivingEntity target = nearbyEntities.get(0);
            Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2, 0);
            Vec3d direction = targetPos.subtract(arrowPos).normalize();
            Vec3d currentVelocity = arrow.getVelocity();
            Vec3d newVelocity = currentVelocity.add(direction.multiply(0.05));
            arrow.setVelocity(newVelocity);
        }
    }

    private static void createElectromagneticPulse(ServerWorld world, BlockPos center, StormArrowData data) {
        double radius = Math.min(6.0 + (data.lightningPower * 1.0), 12.0);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
                Box.of(Vec3d.ofCenter(center), radius * 2, radius * 2, radius * 2), entity -> true);

        int maxEntities = Math.min(entities.size(), 5);
        for (int i = 0; i < maxEntities; i++) {
            LivingEntity entity = entities.get(i);
            double distance = entity.squaredDistanceTo(center.getX(), center.getY(), center.getZ());
            if (distance <= radius * radius) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0));

                Vec3d knockback = entity.getPos().subtract(Vec3d.ofCenter(center)).normalize().multiply(1.0);
                entity.addVelocity(knockback.x, 0.2, knockback.z);
                entity.velocityModified = true;

                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ(),
                        2, 0.3, 0.3, 0.3, 0.1);
            }
        }

        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;

            world.spawnParticles(ParticleTypes.SONIC_BOOM,
                    x, center.getY() + 1, z, 1, 0, 0, 0, 0);
        }
    }
}