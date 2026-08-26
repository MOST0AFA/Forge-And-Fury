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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class InfernalBow extends BowItem {
    public InfernalBow(Item.Properties properties) {
        super(properties.durability(384).repairable(Items.BLAZE_ROD).enchantable(25));
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
        if (!level.isClientSide() && user instanceof Player player) {
            int useTicks = this.getUseDuration(stack, user) - remainingUseTicks;
            float pullProgress = BowItem.getPowerForTime(useTicks);

            if (pullProgress >= 0.1F) {
                var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

                Holder<Enchantment> powerEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("power")).orElse(null);
                Holder<Enchantment> flameEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("flame")).orElse(null);
                Holder<Enchantment> infinityEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("infinity")).orElse(null);
                Holder<Enchantment> unbreakingEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("unbreaking")).orElse(null);
                Holder<Enchantment> mendingEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("mending")).orElse(null);
                Holder<Enchantment> multishotEntry = enchantmentRegistry.get(Identifier.withDefaultNamespace("multishot")).orElse(null);

                int powerLevel = powerEntry != null ? EnchantmentHelper.getItemEnchantmentLevel(powerEntry, stack) : 0;
                int flameLevel = flameEntry != null ? EnchantmentHelper.getItemEnchantmentLevel(flameEntry, stack) : 0;
                boolean hasInfinity = infinityEntry != null && EnchantmentHelper.getItemEnchantmentLevel(infinityEntry, stack) > 0;
                int unbreakingLevel = unbreakingEntry != null ? EnchantmentHelper.getItemEnchantmentLevel(unbreakingEntry, stack) : 0;
                boolean hasMending = mendingEntry != null && EnchantmentHelper.getItemEnchantmentLevel(mendingEntry, stack) > 0;
                boolean hasMultishot = multishotEntry != null && EnchantmentHelper.getItemEnchantmentLevel(multishotEntry, stack) > 0;

                ItemStack arrowStack = player.getProjectile(stack);
                if (arrowStack.isEmpty()) {
                    return false;
                }

                boolean consumeArrow = !hasInfinity || !arrowStack.is(Items.ARROW) || !player.getAbilities().instabuild;

                int arrowCount = hasMultishot ? 3 : 1;
                if (consumeArrow && arrowStack.getCount() < arrowCount) {
                    return false;
                }

                float[] yawOffsets = hasMultishot ? new float[]{-10.0F, 0.0F, 10.0F} : new float[]{0.0F};

                for (int i = 0; i < arrowCount; i++) {
                    AbstractArrow projectile;
                    if (arrowStack.getItem() instanceof ArrowItem arrowItem) {
                        projectile = arrowItem.createArrow(level, arrowStack, player, stack);
                    } else {
                        projectile = new Arrow(level, player, arrowStack.copyWithCount(1), stack);
                    }

                    if (flameLevel > 0) {
                        projectile.igniteForSeconds((100 + flameLevel * 20) / 20.0F);
                    } else {
                        projectile.igniteForSeconds(5.0F);
                    }

                    projectile.setPos(player.getEyePosition());

                    float yawOffset = arrowCount > 1 ? yawOffsets[i] : 0.0F;
                    Vec3 velocity = player.getLookAngle().scale(pullProgress * 3.0F);
                    if (arrowCount > 1) {
                        double yawRadians = Math.toRadians(yawOffset);
                        double cos = Math.cos(yawRadians);
                        double sin = Math.sin(yawRadians);
                        velocity = new Vec3(
                                velocity.x * cos - velocity.z * sin,
                                velocity.y,
                                velocity.x * sin + velocity.z * cos
                        );
                    }
                    projectile.setDeltaMovement(velocity.x, velocity.y, velocity.z);

                    double baseDamageMultiplier = 1.0 + (pullProgress * 0.5);
                    double powerDamageBonus = powerLevel * 0.5;
                    double totalDamageMultiplier = baseDamageMultiplier + powerDamageBonus;
                    projectile.setBaseDamage(2.0 * totalDamageMultiplier);

                    if (pullProgress >= 1.0F) {
                        projectile.setCritArrow(true);
                    }

                    level.addFreshEntity(projectile);
                }

                if (consumeArrow) {
                    arrowStack.shrink(arrowCount);
                }

                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);

                if (level instanceof ServerLevel serverLevel) {
                    spawnFireTrail(serverLevel, player, powerLevel);
                    createPhoenixWings(serverLevel, player, powerLevel);

                    if (pullProgress >= 1.0F) {
                        createFireExplosionAtTarget(serverLevel, player, powerLevel);
                        createFireTornado(serverLevel, player, powerLevel);
                    }
                }

                int damageAmount = Math.max(1, (int) (pullProgress * 2));

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
        return false;
    }

    private void spawnFireTrail(ServerLevel level, Player player, int powerLevel) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();

        int trailLength = 20 + (powerLevel * 5);
        int particleCount = 2 + powerLevel;

        for (int i = 1; i <= trailLength; i++) {
            Vec3 pos = start.add(direction.scale(i * 0.5));

            level.sendParticles(ParticleTypes.FLAME,
                    pos.x, pos.y, pos.z,
                    particleCount, 0.1, 0.1, 0.1, 0.02);

            if (level.getRandom().nextFloat() < 0.3f) {
                level.sendParticles(ParticleTypes.SMOKE,
                        pos.x, pos.y, pos.z,
                        1 + (powerLevel / 2), 0.05, 0.05, 0.05, 0.01);
            }

            if (powerLevel >= 3 && level.getRandom().nextFloat() < 0.4f) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        pos.x, pos.y, pos.z,
                        1, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }

    private void createPhoenixWings(ServerLevel level, Player player, int powerLevel) {
        Vec3 playerPos = player.position();

        int wingParticles = 8 + (powerLevel * 2);
        double wingSpread = 1.5 + (powerLevel * 0.3);

        for (int wing = 0; wing < 2; wing++) {
            double side = wing == 0 ? -1.0 : 1.0;

            for (int i = 0; i < wingParticles; i++) {
                double wingX = playerPos.x + (side * wingSpread) + (level.getRandom().nextGaussian() * 0.3);
                double wingY = playerPos.y + 1.0 + (i * 0.2) + (level.getRandom().nextGaussian() * 0.2);
                double wingZ = playerPos.z - 1.0 + (level.getRandom().nextGaussian() * 0.3);

                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        wingX, wingY, wingZ,
                        1, 0.05, 0.05, 0.05, 0.01);

                if (level.getRandom().nextFloat() < 0.4f) {
                    level.sendParticles(ParticleTypes.END_ROD,
                            wingX, wingY, wingZ,
                            1, 0.1, 0.1, 0.1, 0.0);
                }
            }
        }
    }

    private void createFireExplosionAtTarget(ServerLevel level, Player player, int powerLevel) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(50.0));

        ClipContext context = new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);

        HitResult hitResult = level.clip(context);

        Vec3 explosionPos;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            explosionPos = hitResult.getLocation();
        } else {
            explosionPos = end;
        }

        float baseExplosionPower = 2.0F;
        float powerBonus = powerLevel * 1.5F;
        float totalExplosionPower = baseExplosionPower + powerBonus;

        level.explode(null, explosionPos.x, explosionPos.y, explosionPos.z,
                totalExplosionPower, Level.ExplosionInteraction.TNT);

        createFireCircle(level, BlockPos.containing(explosionPos), powerLevel);
        createMeteorShower(level, BlockPos.containing(explosionPos), powerLevel);

        float volume = 1.5F + (powerLevel * 0.3F);
        level.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, volume, 0.8F);
    }

    private void createFireCircle(ServerLevel level, BlockPos center, int powerLevel) {
        int radius = 3 + powerLevel;
        float fireChance = 0.6f + (powerLevel * 0.1f);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius && level.getRandom().nextFloat() < fireChance) {
                    BlockPos firePos = center.offset(x, 0, z);
                    BlockPos below = firePos.below();

                    if (level.isEmptyBlock(firePos) && level.getBlockState(below).isSolid()) {
                        level.setBlock(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }
        }

        int smokeParticles = 30 + (powerLevel * 10);
        for (int i = 0; i < smokeParticles; i++) {
            double angle = (i / (double) smokeParticles) * 2 * Math.PI;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;

            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    x, center.getY() + 1, z,
                    3 + powerLevel, 0.2, 0.5, 0.2, 0.05);
        }
    }

    private void createFireTornado(ServerLevel level, Player player, int powerLevel) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 tornadoCenter = start.add(direction.scale(8.0));

        level.playSound(null, tornadoCenter.x, tornadoCenter.y, tornadoCenter.z,
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.7F + (powerLevel * 0.2F), 0.5F);

        int tornadoHeight = 12 + (powerLevel * 3);
        double baseRadius = 2.0 + (powerLevel * 0.5);

        for (int height = 0; height < tornadoHeight; height++) {
            double y = tornadoCenter.y + height * 0.5;
            double radius = baseRadius - (height * 0.1);
            int particlesAtHeight = (int) (16 - height + powerLevel * 2);

            for (int i = 0; i < particlesAtHeight; i++) {
                double angle = (i / (double) particlesAtHeight) * 2 * Math.PI + (height * 0.3);
                double x = tornadoCenter.x + Math.cos(angle) * radius;
                double z = tornadoCenter.z + Math.sin(angle) * radius;

                level.sendParticles(ParticleTypes.FLAME,
                        x, y, z,
                        1 + (powerLevel / 2), 0.05, 0.05, 0.05, 0.05);

                if (height % 3 == 0) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            x, y, z,
                            1, 0.1, 0.1, 0.1, 0.02);
                }

                if (powerLevel >= 4 && height % 2 == 0) {
                    level.sendParticles(ParticleTypes.LAVA,
                            x, y, z,
                            1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }
    }

    private void createMeteorShower(ServerLevel level, BlockPos center, int powerLevel) {
        int meteorCount = 6 + (powerLevel * 2);
        double meteorSpread = 8.0 + (powerLevel * 2.0);

        for (int i = 0; i < meteorCount; i++) {
            double meteorX = center.getX() + (level.getRandom().nextGaussian() * meteorSpread);
            double meteorY = center.getY() + 15 + (level.getRandom().nextDouble() * 5) + (powerLevel * 2);
            double meteorZ = center.getZ() + (level.getRandom().nextGaussian() * meteorSpread);

            level.sendParticles(ParticleTypes.FIREWORK,
                    meteorX, meteorY, meteorZ,
                    1 + powerLevel, 0.0, 0.0, 0.0, 0.0);

            int trailLength = 20 + (powerLevel * 5);
            for (int trail = 0; trail < trailLength; trail++) {
                double trailY = meteorY - (trail * 0.7);
                if (trailY > center.getY()) {
                    level.sendParticles(ParticleTypes.FLAME,
                            meteorX + (level.getRandom().nextGaussian() * 0.3),
                            trailY,
                            meteorZ + (level.getRandom().nextGaussian() * 0.3),
                            1 + (powerLevel / 3), 0.1, 0.1, 0.1, 0.02);
                }
            }
        }
    }
}
