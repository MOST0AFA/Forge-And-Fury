package dev.most0afa.forge.and.fury.Items;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;

public class InfernalBow extends BowItem {
    public InfernalBow() {
        super(new Settings().maxDamage(384));
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(Items.BLAZE_ROD) || super.canRepair(stack, ingredient);
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
        if (!world.isClient && user instanceof PlayerEntity player) {
            int useTicks = this.getMaxUseTime(stack) - remainingUseTicks;
            float pullProgress = BowItem.getPullProgress(useTicks);

            if (pullProgress >= 0.1F) {
                var registryManager = world.getRegistryManager();
                var enchantmentRegistry = registryManager.get(RegistryKeys.ENCHANTMENT);

                var powerEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "power"));
                var flameEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "flame"));
                var infinityEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "infinity"));
                var unbreakingEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "unbreaking"));
                var mendingEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "mending"));
                var multishotEntry = enchantmentRegistry.getEntry(Identifier.of("minecraft", "multishot"));

                int powerLevel = powerEntry.isPresent() ? EnchantmentHelper.getLevel(powerEntry.get(), stack) : 0;
                int flameLevel = flameEntry.isPresent() ? EnchantmentHelper.getLevel(flameEntry.get(), stack) : 0;
                boolean hasInfinity = infinityEntry.isPresent() && EnchantmentHelper.getLevel(infinityEntry.get(), stack) > 0;
                int unbreakingLevel = unbreakingEntry.isPresent() ? EnchantmentHelper.getLevel(unbreakingEntry.get(), stack) : 0;
                boolean hasMending = mendingEntry.isPresent() && EnchantmentHelper.getLevel(mendingEntry.get(), stack) > 0;
                boolean hasMultishot = multishotEntry.isPresent() && EnchantmentHelper.getLevel(multishotEntry.get(), stack) > 0;

                ItemStack arrowStack = player.getProjectileType(stack);
                if (arrowStack.isEmpty()) {
                    return;
                }

                boolean consumeArrow = !hasInfinity || !arrowStack.isOf(Items.ARROW) || !player.getAbilities().creativeMode;

                int arrowCount = hasMultishot ? 3 : 1;
                if (consumeArrow && arrowStack.getCount() < arrowCount) {
                    return;
                }

                float[] yawOffsets = hasMultishot ? new float[]{-10.0F, 0.0F, 10.0F} : new float[]{0.0F};

                for (int i = 0; i < arrowCount; i++) {
                    PersistentProjectileEntity projectile;
                    if (arrowStack.getItem() instanceof ArrowItem arrowItem) {
                        projectile = arrowItem.createArrow(world, arrowStack, player, stack);
                    } else {
                        projectile = new ArrowEntity(EntityType.ARROW, world);
                    }

                    projectile.setOwner(player);

                    if (flameLevel > 0) {
                        projectile.setFireTicks(100 + (flameLevel * 20));
                    } else {
                        projectile.setFireTicks(100);
                    }

                    projectile.setPosition(player.getEyePos());

                    float yawOffset = arrowCount > 1 ? yawOffsets[i] : 0.0F;
                    Vec3d velocity = player.getRotationVector().multiply(pullProgress * 3.0F);
                    if (arrowCount > 1) {
                        double yawRadians = Math.toRadians(yawOffset);
                        double cos = Math.cos(yawRadians);
                        double sin = Math.sin(yawRadians);
                        velocity = new Vec3d(
                                velocity.x * cos - velocity.z * sin,
                                velocity.y,
                                velocity.x * sin + velocity.z * cos
                        );
                    }
                    projectile.setVelocity(velocity.x, velocity.y, velocity.z);

                    double baseDamageMultiplier = 1.0 + (pullProgress * 0.5);
                    double powerDamageBonus = powerLevel * 0.5;
                    double totalDamageMultiplier = baseDamageMultiplier + powerDamageBonus;
                    projectile.setDamage(projectile.getDamage() * totalDamageMultiplier);

                    if (pullProgress >= 1.0F) {
                        projectile.setCritical(true);
                    }

                    world.spawnEntity(projectile);
                }

                if (consumeArrow) {
                    arrowStack.decrement(arrowCount);
                    if (arrowStack.isEmpty()) {
                        player.getInventory().removeOne(arrowStack);
                    }
                }

                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F);

                if (world instanceof ServerWorld serverWorld) {
                    spawnFireTrail(serverWorld, player, powerLevel);
                    createPhoenixWings(serverWorld, player, powerLevel);

                    if (pullProgress >= 1.0F) {
                        createFireExplosionAtTarget(serverWorld, player, powerLevel);
                        createFireTornado(serverWorld, player, powerLevel);
                    }
                }

                int damageAmount = Math.max(1, (int) (pullProgress * 2));

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

    private void spawnFireTrail(ServerWorld world, PlayerEntity player, int powerLevel) {
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();

        int trailLength = 20 + (powerLevel * 5);
        int particleCount = 2 + powerLevel;

        for (int i = 1; i <= trailLength; i++) {
            Vec3d pos = start.add(direction.multiply(i * 0.5));

            world.spawnParticles(ParticleTypes.FLAME,
                    pos.x, pos.y, pos.z,
                    particleCount, 0.1, 0.1, 0.1, 0.02);

            if (world.random.nextFloat() < 0.3f) {
                world.spawnParticles(ParticleTypes.SMOKE,
                        pos.x, pos.y, pos.z,
                        1 + (powerLevel / 2), 0.05, 0.05, 0.05, 0.01);
            }

            if (powerLevel >= 3 && world.random.nextFloat() < 0.4f) {
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        pos.x, pos.y, pos.z,
                        1, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }

    private void createPhoenixWings(ServerWorld world, PlayerEntity player, int powerLevel) {
        Vec3d playerPos = player.getPos();

        int wingParticles = 8 + (powerLevel * 2);
        double wingSpread = 1.5 + (powerLevel * 0.3);

        for (int wing = 0; wing < 2; wing++) {
            double side = wing == 0 ? -1.0 : 1.0;

            for (int i = 0; i < wingParticles; i++) {
                double wingX = playerPos.x + (side * wingSpread) + (world.random.nextGaussian() * 0.3);
                double wingY = playerPos.y + 1.0 + (i * 0.2) + (world.random.nextGaussian() * 0.2);
                double wingZ = playerPos.z - 1.0 + (world.random.nextGaussian() * 0.3);

                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        wingX, wingY, wingZ,
                        1, 0.05, 0.05, 0.05, 0.01);

                if (world.random.nextFloat() < 0.4f) {
                    world.spawnParticles(ParticleTypes.END_ROD,
                            wingX, wingY, wingZ,
                            1, 0.1, 0.1, 0.1, 0.0);
                }
            }
        }
    }

    private void createFireExplosionAtTarget(ServerWorld world, PlayerEntity player, int powerLevel) {
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(50.0));

        RaycastContext context = new RaycastContext(start, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player);

        HitResult hitResult = world.raycast(context);

        Vec3d explosionPos;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            explosionPos = hitResult.getPos();
        } else {
            explosionPos = end;
        }

        float baseExplosionPower = 2.0F;
        float powerBonus = powerLevel * 1.5F;
        float totalExplosionPower = baseExplosionPower + powerBonus;

        world.createExplosion(null, explosionPos.x, explosionPos.y, explosionPos.z,
                totalExplosionPower, World.ExplosionSourceType.TNT);

        createFireCircle(world, new BlockPos((int)explosionPos.x, (int)explosionPos.y, (int)explosionPos.z), powerLevel);
        createMeteorShower(world, new BlockPos((int)explosionPos.x, (int)explosionPos.y, (int)explosionPos.z), powerLevel);

        float volume = 1.5F + (powerLevel * 0.3F);
        world.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, volume, 0.8F);
    }

    private void createFireCircle(ServerWorld world, BlockPos center, int powerLevel) {
        int radius = 3 + powerLevel;
        float fireChance = 0.6f + (powerLevel * 0.1f);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius && world.random.nextFloat() < fireChance) {
                    BlockPos firePos = center.add(x, 0, z);
                    BlockPos below = firePos.down();

                    if (world.isAir(firePos) && world.getBlockState(below).isSolid()) {
                        world.setBlockState(firePos, net.minecraft.block.Blocks.FIRE.getDefaultState());
                    }
                }
            }
        }

        int smokeParticles = 30 + (powerLevel * 10);
        for (int i = 0; i < smokeParticles; i++) {
            double angle = (i / (double)smokeParticles) * 2 * Math.PI;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;

            world.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    x, center.getY() + 1, z,
                    3 + powerLevel, 0.2, 0.5, 0.2, 0.05);
        }
    }

    private void createFireTornado(ServerWorld world, PlayerEntity player, int powerLevel) {
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d tornadoCenter = start.add(direction.multiply(8.0));

        world.playSound(null, tornadoCenter.x, tornadoCenter.y, tornadoCenter.z,
                SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.7F + (powerLevel * 0.2F), 0.5F);

        int tornadoHeight = 12 + (powerLevel * 3);
        double baseRadius = 2.0 + (powerLevel * 0.5);

        for (int height = 0; height < tornadoHeight; height++) {
            double y = tornadoCenter.y + height * 0.5;
            double radius = baseRadius - (height * 0.1);
            int particlesAtHeight = (int)(16 - height + powerLevel * 2);

            for (int i = 0; i < particlesAtHeight; i++) {
                double angle = (i / (double)particlesAtHeight) * 2 * Math.PI + (height * 0.3);
                double x = tornadoCenter.x + Math.cos(angle) * radius;
                double z = tornadoCenter.z + Math.sin(angle) * radius;

                world.spawnParticles(ParticleTypes.FLAME,
                        x, y, z,
                        1 + (powerLevel / 2), 0.05, 0.05, 0.05, 0.05);

                if (height % 3 == 0) {
                    world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            x, y, z,
                            1, 0.1, 0.1, 0.1, 0.02);
                }

                if (powerLevel >= 4 && height % 2 == 0) {
                    world.spawnParticles(ParticleTypes.LAVA,
                            x, y, z,
                            1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }
    }

    private void createMeteorShower(ServerWorld world, BlockPos center, int powerLevel) {
        int meteorCount = 6 + (powerLevel * 2);
        double meteorSpread = 8.0 + (powerLevel * 2.0);

        for (int i = 0; i < meteorCount; i++) {
            double meteorX = center.getX() + (world.random.nextGaussian() * meteorSpread);
            double meteorY = center.getY() + 15 + (world.random.nextDouble() * 5) + (powerLevel * 2);
            double meteorZ = center.getZ() + (world.random.nextGaussian() * meteorSpread);

            world.spawnParticles(ParticleTypes.FIREWORK,
                    meteorX, meteorY, meteorZ,
                    1 + powerLevel, 0.0, 0.0, 0.0, 0.0);

            int trailLength = 20 + (powerLevel * 5);
            for (int trail = 0; trail < trailLength; trail++) {
                double trailY = meteorY - (trail * 0.7);
                if (trailY > center.getY()) {
                    world.spawnParticles(ParticleTypes.FLAME,
                            meteorX + (world.random.nextGaussian() * 0.3),
                            trailY,
                            meteorZ + (world.random.nextGaussian() * 0.3),
                            1 + (powerLevel / 3), 0.1, 0.1, 0.1, 0.02);
                }
            }
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }
}