package dev.most0afa.forge.and.fury.Items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.util.Identifier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

import java.util.Arrays;
import java.util.List;

public class FireAxe extends ToolItem {
    private static final List<String> ABILITIES = Arrays.asList("fireball", "fire_rain", "flaming_swing");
    private static final int FIREBALL_COOLDOWN = 60;
    private static final int FIRE_RAIN_COOLDOWN = 200;
    private static final int FLAMING_SWING_COOLDOWN = 140;
    private static final int WITHER_IMMUNITY_DURATION = 300;

    public FireAxe(ToolMaterial material, Settings settings) {
        super(material, settings.component(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                                new EntityAttributeModifier(Identifier.of("base_attack_damage"),
                                        8.0 + material.getAttackDamage(), EntityAttributeModifier.Operation.ADD_VALUE),
                                AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.GENERIC_ATTACK_SPEED,
                                new EntityAttributeModifier(Identifier.of("base_attack_speed"),
                                        -3.0, EntityAttributeModifier.Operation.ADD_VALUE),
                                AttributeModifierSlot.MAINHAND)
                        .build()));
    }

    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound tag = nbtComponent.getNbt().copy();

        if (!world.isClient) {
            if (player.isSneaking()) {
                int currentAbilityIndex = tag.getInt("SelectedAbility");
                currentAbilityIndex = (currentAbilityIndex + 1) % ABILITIES.size();
                tag.putInt("SelectedAbility", currentAbilityIndex);
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));

                String abilityName = switch (ABILITIES.get(currentAbilityIndex)) {
                    case "fireball" -> "Fireball";
                    case "fire_rain" -> "Fire Rain";
                    case "flaming_swing" -> "Flaming Swing";
                    default -> "Unknown";
                };
                player.sendMessage(Text.literal("Selected ability: " + abilityName), true);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.5F, 1.2F);
            } else {
                int currentAbilityIndex = tag.getInt("SelectedAbility");
                String selectedAbility = ABILITIES.get(currentAbilityIndex);
                String cooldownKey = selectedAbility + "_LastUsed";

                long lastUsed = tag.getLong(cooldownKey);
                long currentTime = world.getTime();
                int requiredCooldown = getAbilityCooldown(selectedAbility);

                if (currentTime - lastUsed >= requiredCooldown) {
                    performAbility(world, player, tag);
                    tag.putLong(cooldownKey, currentTime);
                    stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
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
                    player.sendMessage(Text.literal(abilityName + " on cooldown: " + String.format("%.1f", remainingSeconds) + " seconds remaining"), true);
                }
            }
        }
        return TypedActionResult.success(stack);
    }

    private int getAbilityCooldown(String ability) {
        return switch (ability) {
            case "fireball" -> FIREBALL_COOLDOWN;
            case "fire_rain" -> FIRE_RAIN_COOLDOWN;
            case "flaming_swing" -> FLAMING_SWING_COOLDOWN;
            default -> FIREBALL_COOLDOWN;
        };
    }

    private void grantWitherImmunity(PlayerEntity player) {
        player.removeStatusEffect(StatusEffects.WITHER);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, WITHER_IMMUNITY_DURATION, 0, false, true));
    }

    private void performAbility(World world, PlayerEntity player, NbtCompound tag) {
        int currentAbilityIndex = tag.getInt("SelectedAbility");
        String selectedAbility = ABILITIES.get(currentAbilityIndex);

        String abilityMessage = switch (selectedAbility) {
            case "fireball" -> "Casting Fireball!";
            case "fire_rain" -> "Summoning Fire Rain!";
            case "flaming_swing" -> "Executing Flaming Swing!";
            default -> "Activating ability!";
        };
        player.sendMessage(Text.literal(abilityMessage), true);

        switch (selectedAbility) {
            case "fireball":
                castFireball(world, player);
                break;
            case "fire_rain":
                castFireRain(world, player);
                break;
            case "flaming_swing":
                castFlamingSwing(world, player);
                break;
        }
    }

    private void castFireball(World world, PlayerEntity player) {
        if (world instanceof ServerWorld serverWorld) {
            SmallFireballEntity fireball = new SmallFireballEntity(world, player,
                    player.getRotationVector().multiply(3.5));
            fireball.setPosition(player.getEyePos());

            world.spawnEntity(fireball);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);

            serverWorld.spawnParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1, player.getZ(),
                    10, 0.5, 0.5, 0.5, 0.1);

            List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class,
                    Box.of(player.getPos(), 20, 20, 20),
                    entity -> entity != player && entity.squaredDistanceTo(player) <= 400);

            for (LivingEntity entity : nearbyEntities) {
                Vec3d direction = entity.getPos().subtract(player.getPos()).normalize();
                Vec3d fireballDirection = player.getRotationVector().normalize();

                if (direction.dotProduct(fireballDirection) > 0.8) {
                    entity.damage(world.getDamageSources().playerAttack(player), 12.0f);
                    entity.setOnFireFor(8);
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
                }
            }
        }
    }

    private void castFireRain(World world, PlayerEntity player) {
        if (world instanceof ServerWorld serverWorld) {
            Vec3d eyePos = player.getEyePos();
            Vec3d lookDirection = player.getRotationVector();
            Vec3d targetPos = eyePos.add(lookDirection.multiply(20));

            BlockHitResult hitResult = world.raycast(new RaycastContext(
                    eyePos, targetPos, RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE, player));

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                targetPos = hitResult.getPos();
            }

            final Vec3d finalTargetPos = targetPos;

            spawnMeteorWave(serverWorld, player, finalTargetPos, 8);

            serverWorld.getServer().execute(() -> spawnMeteorWave(serverWorld, player, finalTargetPos, 8));
            serverWorld.getServer().execute(() -> spawnMeteorWave(serverWorld, player, finalTargetPos, 8));
            serverWorld.getServer().execute(() -> spawnMeteorWave(serverWorld, player, finalTargetPos, 8));
            serverWorld.getServer().execute(() -> spawnMeteorWave(serverWorld, player, finalTargetPos, 8));

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.5F, 0.6F);
        }
    }

    private void castFlamingSwing(World world, PlayerEntity player) {
        if (world instanceof ServerWorld serverWorld) {
            AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, player.getX(), player.getY(), player.getZ());
            cloud.setRadius(6.0F);
            cloud.setDuration(80);
            cloud.setParticleType(ParticleTypes.FLAME);
            cloud.setOwner(player);

            world.spawnEntity(cloud);

            List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class,
                    Box.of(player.getPos(), 12, 8, 12),
                    entity -> entity != player && entity.squaredDistanceTo(player) <= 36);

            for (LivingEntity entity : nearbyEntities) {
                entity.damage(world.getDamageSources().playerAttack(player), 15.0f);
                entity.setOnFireFor(10);
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 2));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));

                Vec3d knockback = entity.getPos().subtract(player.getPos()).normalize().multiply(1.5);
                entity.addVelocity(knockback.x, 0.5, knockback.z);
                entity.velocityModified = true;
            }

            player.removeStatusEffect(StatusEffects.WITHER);

            BlockPos playerPos = player.getBlockPos();
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    for (int y = -1; y <= 3; y++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        if (world.getBlockState(pos).isAir() &&
                                world.getBlockState(pos.down()).isSolidBlock(world, pos.down()) &&
                                world.random.nextFloat() < 0.4f) {
                            world.setBlockState(pos, Blocks.FIRE.getDefaultState());
                        }
                    }
                }
            }

            serverWorld.spawnParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1, player.getZ(),
                    50, 3.0, 1.5, 3.0, 0.3);

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1.5F, 0.7F);
        }
    }

    private void spawnMeteorWave(ServerWorld world, PlayerEntity player, Vec3d targetPos, int meteorCount) {
        for (int i = 0; i < meteorCount; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 16;
            double offsetZ = (world.random.nextDouble() - 0.5) * 16;
            double x = targetPos.x + offsetX;
            double z = targetPos.z + offsetZ;
            double y = targetPos.y + 20 + (world.random.nextDouble() * 10);

            SmallFireballEntity fireball = new SmallFireballEntity(world, player,
                    new Vec3d(0, -2.5, 0));
            fireball.setPosition(x, y, z);
            world.spawnEntity(fireball);

            world.spawnParticles(ParticleTypes.FLAME, x, y, z, 5, 0.2, 0.2, 0.2, 0.1);
        }

        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class,
                Box.of(targetPos, 16, 8, 16),
                entity -> entity != player);

        for (LivingEntity entity : nearbyEntities) {
            if (entity.squaredDistanceTo(targetPos) <= 64) {
                entity.damage(world.getDamageSources().playerAttack(player), 6.0f);
                entity.setOnFireFor(4);
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0));
            }
        }
    }
}
