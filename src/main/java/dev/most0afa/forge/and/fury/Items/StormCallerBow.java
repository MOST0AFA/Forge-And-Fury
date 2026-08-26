package dev.most0afa.forge.and.fury.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StormCallerBow extends BowItem {
    private static final Map<UUID, StormArrowData> stormArrows = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> recentLightningStrikes = new ConcurrentHashMap<>();
    private static final long LIGHTNING_COOLDOWN = 5000;

    public StormCallerBow(Item.Properties properties) {
        super(properties.stacksTo(1).durability(512).repairable(Items.PRISMARINE_CRYSTALS).enchantable(25));
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
        if (user instanceof Player player) {
            boolean hasAmmo = !player.getProjectile(stack).isEmpty();
            ItemStack arrowStack = hasAmmo ? player.getProjectile(stack) : new ItemStack(Items.ARROW);

            if (!level.isClientSide()) {
                int useTicks = this.getUseDuration(stack, user) - remainingUseTicks;
                float power = BowItem.getPowerForTime(useTicks);

                if (power >= 0.1F) {
                    var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

                    Holder<Enchantment> powerEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("power")).orElse(null);
                    Holder<Enchantment> flameEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("flame")).orElse(null);
                    Holder<Enchantment> infinityEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("infinity")).orElse(null);
                    Holder<Enchantment> unbreakingEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("unbreaking")).orElse(null);
                    Holder<Enchantment> mendingEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("mending")).orElse(null);
                    Holder<Enchantment> multishot = enchantmentRegistry.get(Identifier.withDefaultNamespace("multishot")).orElse(null);

                    int powerLevel = powerEntry != null ? EnchantmentHelper.getItemEnchantmentLevel(powerEntry, stack) : 0;
                    int flameLevel = flameEntry != null ? EnchantmentHelper.getItemEnchantmentLevel(flameEntry, stack) : 0;
                    boolean hasInfinity = infinityEntry != null && EnchantmentHelper.getItemEnchantmentLevel(infinityEntry, stack) > 0;
                    int unbreakingLevel = unbreakingEntry != null ? EnchantmentHelper.getItemEnchantmentLevel(unbreakingEntry, stack) : 0;
                    boolean hasMending = mendingEntry != null && EnchantmentHelper.getItemEnchantmentLevel(mendingEntry, stack) > 0;
                    boolean hasMultishot = multishot != null && EnchantmentHelper.getItemEnchantmentLevel(multishot, stack) > 0;

                    boolean consumeArrow = !hasInfinity || !player.getAbilities().instabuild;
                    if (!consumeArrow || hasAmmo || player.getAbilities().instabuild) {

                        int arrowCount = hasMultishot ? 3 : 1;
                        float[] yawOffsets = hasMultishot ? new float[]{-10.0F, 0.0F, 10.0F} : new float[]{0.0F};

                        for (int i = 0; i < arrowCount; i++) {
                            Arrow arrow = new Arrow(EntityTypes.ARROW, level);
                            arrow.setOwner(player);
                            arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());

                            float yawOffset = arrowCount > 1 ? yawOffsets[i] : 0.0F;
                            arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, 0.0F,
                                    power * 3.0F + (powerLevel * 0.5F), 1.0F);

                            if (power == 1.0F) {
                                arrow.setCritArrow(true);
                            }

                            double baseDamage = 4.0;
                            double powerBonus = powerLevel * 1.5;
                            arrow.setBaseDamage(baseDamage + powerBonus);

                            if (flameLevel > 0) {
                                arrow.igniteForSeconds((100 + flameLevel * 20) / 20.0F);
                            }

                            StormArrowData data = new StormArrowData(powerLevel, power, hasMultishot);
                            stormArrows.put(arrow.getUUID(), data);

                            if (power >= 0.8F && level.getRandom().nextFloat() < 0.2F) {
                                data.hasHomingEffect = true;
                            }

                            level.addFreshEntity(arrow);
                        }

                        if (level instanceof ServerLevel serverLevel) {
                            createShootingEffects(serverLevel, player, power, powerLevel, hasMultishot);
                        }

                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.3F + (powerLevel * 0.1F), 2.0F);

                        if (consumeArrow && hasAmmo) {
                            arrowStack.shrink(1);
                        }

                        int damageAmount = 1;
                        if (unbreakingLevel > 0) {
                            if (level.getRandom().nextInt(unbreakingLevel + 1) == 0) {
                                stack.hurtAndBreak(damageAmount, player, EquipmentSlot.MAINHAND);
                            }
                        } else {
                            stack.hurtAndBreak(damageAmount, player, EquipmentSlot.MAINHAND);
                        }

                        if (hasMending && player.experienceLevel > 0) {
                            int repairAmount = Math.min(stack.getDamageValue(), 2);
                            if (repairAmount > 0) {
                                stack.setDamageValue(stack.getDamageValue() - repairAmount);
                                player.giveExperienceLevels(-1);
                            }
                        }

                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void createShootingEffects(ServerLevel level, Player player, float power, int powerLevel, boolean multishot) {
        Vec3 playerPos = player.position();

        int sparkCount = Math.min(multishot ? 8 : 5, 5 + powerLevel);

        for (int i = 0; i < sparkCount; i++) {
            double offsetX = level.getRandom().nextGaussian() * 0.8;
            double offsetY = level.getRandom().nextDouble() * 1.0 + 0.5;
            double offsetZ = level.getRandom().nextGaussian() * 0.8;

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    playerPos.x + offsetX, playerPos.y + offsetY, playerPos.z + offsetZ,
                    1, 0, 0, 0, 0.05);
        }

        if (power >= 0.8F && level.isThundering() && level.getRandom().nextBoolean()) {
            int enchantParticles = Math.min(3 + powerLevel, 8);
            for (int i = 0; i < enchantParticles; i++) {
                level.sendParticles(ParticleTypes.ENCHANT,
                        playerPos.x, playerPos.y + 1, playerPos.z,
                        1, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    public static void handleArrowTick(Arrow arrow) {
        UUID arrowId = arrow.getUUID();
        StormArrowData data = stormArrows.get(arrowId);

        if (data != null && arrow.level() instanceof ServerLevel serverLevel && !data.isGrounded) {
            if (arrow.tickCount % 4 != 0) return;

            Vec3 pos = arrow.position();

            if (serverLevel.getRandom().nextFloat() < 0.1F) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
            }

            if (data.hasHomingEffect && arrow.tickCount % 8 == 0) {
                updateHomingTrajectory(arrow, serverLevel);
            }
        }
    }

    public static void handleArrowImpact(Arrow arrow, BlockPos impactPos) {
        UUID arrowId = arrow.getUUID();
        StormArrowData data = stormArrows.get(arrowId);

        if (data != null && !data.hasLanded) {
            data.hasLanded = true;
            data.isGrounded = true;

            if (arrow.level() instanceof ServerLevel serverLevel) {
                long currentTime = System.currentTimeMillis();
                Long lastStrike = recentLightningStrikes.get(impactPos);
                if (lastStrike != null && currentTime - lastStrike < LIGHTNING_COOLDOWN) {
                    return;
                }
                recentLightningStrikes.put(impactPos, currentTime);

                cleanupOldStrikes(currentTime);

                createStormEffects(serverLevel, impactPos, data);

                if (data.drawPower >= 0.7F) {
                    createElectricField(serverLevel, impactPos, data);
                }

                summonLightningStrike(serverLevel, impactPos, data);
            }
        }
    }

    private static void cleanupOldStrikes(long currentTime) {
        recentLightningStrikes.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > LIGHTNING_COOLDOWN);
    }

    private static void createStormEffects(ServerLevel level, BlockPos pos, StormArrowData data) {
        int sparkCount = 8 + (data.lightningPower * 3);
        for (int i = 0; i < sparkCount; i++) {
            double offsetX = level.getRandom().nextGaussian() * 1.0;
            double offsetY = level.getRandom().nextDouble() * 1.5;
            double offsetZ = level.getRandom().nextGaussian() * 1.0;

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0.05);
        }

        if (level.getRandom().nextBoolean()) {
            int cloudCount = 4 + data.lightningPower * 2;
            for (int i = 0; i < cloudCount; i++) {
                double offsetX = level.getRandom().nextGaussian() * 0.8;
                double offsetZ = level.getRandom().nextGaussian() * 0.8;

                level.sendParticles(ParticleTypes.CLOUD,
                        pos.getX() + 0.5 + offsetX,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5 + offsetZ,
                        1, 0, 0.05, 0, 0.01);
            }
        }
    }

    private static void createElectricField(ServerLevel level, BlockPos center, StormArrowData data) {
        int fieldSize = 4 + data.lightningPower * 2;
        for (int i = 0; i < fieldSize; i++) {
            double angle = (i / (double) fieldSize) * 2 * Math.PI;
            double radius = 1.5 + (data.lightningPower * 0.3);
            double x = center.getX() + 0.5 + Math.cos(angle) * radius;
            double z = center.getZ() + 0.5 + Math.sin(angle) * radius;

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    x, center.getY() + 0.5, z, 1, 0.05, 0.05, 0.05, 0.01);
        }

        level.playSound(null, center.getX(), center.getY(), center.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS,
                0.3F + (data.lightningPower * 0.1F), 2.0F);
    }

    private static void summonLightningStrike(ServerLevel level, BlockPos pos, StormArrowData data) {
        LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (lightning != null) {
            lightning.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(lightning);
        }

        createElectromagneticPulse(level, pos, data);

        float chainChance = Math.min(0.15F + (data.lightningPower * 0.05F), 1.0F);
        if (data.lightningPower > 1 && level.getRandom().nextFloat() < chainChance) {
            int chainCount = data.lightningPower - 1;
            for (int i = 0; i < chainCount; i++) {
                int offsetX = level.getRandom().nextInt(5) - 2;
                int offsetZ = level.getRandom().nextInt(5) - 2;
                BlockPos chainPos = pos.offset(offsetX, 0, offsetZ);

                LightningBolt chainLightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (chainLightning != null) {
                    chainLightning.snapTo(chainPos.getX() + 0.5, chainPos.getY(), chainPos.getZ() + 0.5);
                    level.addFreshEntity(chainLightning);
                }
            }
        }

        if (data.drawPower >= 1.0F && data.lightningPower >= 3 && level.isThundering()) {
            createThunderClap(level, pos, data);
        }
    }

    private static void createThunderClap(ServerLevel level, BlockPos center, StormArrowData data) {
        float volume = 1.5F + (data.lightningPower * 0.3F);
        level.playSound(null, center.getX(), center.getY(), center.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, volume, 0.8F);

        int cloudCount = 10 + (data.lightningPower * 3);
        for (int i = 0; i < cloudCount; i++) {
            double angle = level.getRandom().nextDouble() * 2 * Math.PI;
            double distance = level.getRandom().nextDouble() * (2.0 + data.lightningPower * 0.5);
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;

            level.sendParticles(ParticleTypes.CLOUD,
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

    private static void updateHomingTrajectory(Arrow arrow, ServerLevel level) {
        Vec3 arrowPos = arrow.position();
        AABB searchBox = AABB.ofSize(arrowPos, 8, 8, 8);

        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != arrow.getOwner() && entity.distanceToSqr(arrowPos) <= 32.0);

        if (!nearbyEntities.isEmpty()) {
            LivingEntity target = nearbyEntities.get(0);
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            Vec3 direction = targetPos.subtract(arrowPos).normalize();
            Vec3 currentVelocity = arrow.getDeltaMovement();
            Vec3 newVelocity = currentVelocity.add(direction.scale(0.05));
            arrow.setDeltaMovement(newVelocity);
        }
    }

    private static void createElectromagneticPulse(ServerLevel level, BlockPos center, StormArrowData data) {
        double radius = 6.0 + (data.lightningPower * 1.5);
        float shockDamage = 1.5F + (data.lightningPower * 1.0F);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(Vec3.atCenterOf(center), radius * 2, radius * 2, radius * 2), entity -> true);

        int maxEntities = Math.min(entities.size(), 5 + data.lightningPower);
        for (int i = 0; i < maxEntities; i++) {
            LivingEntity entity = entities.get(i);
            double distance = entity.distanceToSqr(center.getX(), center.getY(), center.getZ());
            if (distance <= radius * radius) {
                entity.hurtServer(level, level.damageSources().lightningBolt(), shockDamage);
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));

                Vec3 knockback = entity.position().subtract(Vec3.atCenterOf(center)).normalize().scale(1.0);
                entity.push(knockback.x, 0.2, knockback.z);

                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                        2, 0.3, 0.3, 0.3, 0.1);
            }
        }

        int ringParticles = 8 + data.lightningPower * 2;
        for (int i = 0; i < ringParticles; i++) {
            double angle = (i / (double) ringParticles) * 2 * Math.PI;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;

            level.sendParticles(ParticleTypes.SONIC_BOOM,
                    x, center.getY() + 1, z, 1, 0, 0, 0, 0);
        }
    }
}
