package dev.most0afa.forge.and.fury.Items;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;

import java.util.ArrayList;
import java.util.List;

public class Ruiner extends ToolItem {
    private static final int MAX_HARDNESS = 50;

    public Ruiner(ToolMaterials material, Settings settings) {
        super(material, settings.maxDamage(2048).attributeModifiers(
                AttributeModifiersComponent.builder()
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                                new EntityAttributeModifier(ToolItem.BASE_ATTACK_DAMAGE_MODIFIER_ID, 6.0, EntityAttributeModifier.Operation.ADD_VALUE),
                                AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.GENERIC_ATTACK_SPEED,
                                new EntityAttributeModifier(ToolItem.BASE_ATTACK_SPEED_MODIFIER_ID, -2.8, EntityAttributeModifier.Operation.ADD_VALUE),
                                AttributeModifierSlot.MAINHAND)
                        .build()
        ));
    }

    public boolean isSuitableFor(BlockState state) {
        return state.isIn(BlockTags.PICKAXE_MINEABLE);
    }

    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        if (state.isIn(BlockTags.PICKAXE_MINEABLE)) {
            return 9.0f;
        }
        return super.getMiningSpeed(stack, state);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && miner instanceof PlayerEntity player) {
            List<BlockPos> blocksToBreak = new ArrayList<>();
            int totalHardness = 0;

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos targetPos = pos.add(x, y, z);
                        BlockState targetState = world.getBlockState(targetPos);
                        float hardness = targetState.getHardness(world, targetPos);

                        if (hardness >= 0 && hardness <= MAX_HARDNESS && !targetState.isAir()) {
                            blocksToBreak.add(targetPos);
                            totalHardness += Math.max(1, (int)hardness);
                        }
                    }
                }
            }

            if (!blocksToBreak.isEmpty()) {
                createDestructionEffect(world, pos, blocksToBreak.size());

                for (BlockPos targetPos : blocksToBreak) {
                    world.breakBlock(targetPos, true, miner);

                    if (world instanceof ServerWorld serverWorld) {
                        spawnBreakParticles(serverWorld, targetPos);
                    }
                }

                int damageAmount = Math.max(1, totalHardness / 10);
                stack.damage(damageAmount, player, EquipmentSlot.MAINHAND);

                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.8F, 1.2F);
            }
        }
        return super.postMine(stack, world, state, pos, miner);
    }

    private void createDestructionEffect(World world, BlockPos center, int blockCount) {
        if (world instanceof ServerWorld serverWorld) {
            int particleCount = Math.min(50, blockCount * 3);

            for (int i = 0; i < particleCount; i++) {
                double offsetX = (world.getRandom().nextDouble() - 0.5) * 4.0;
                double offsetY = (world.getRandom().nextDouble() - 0.5) * 4.0;
                double offsetZ = (world.getRandom().nextDouble() - 0.5) * 4.0;

                serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
                        center.getX() + 0.5 + offsetX,
                        center.getY() + 0.5 + offsetY,
                        center.getZ() + 0.5 + offsetZ,
                        1, 0, 0, 0, 0);
            }

            for (int i = 0; i < blockCount; i++) {
                double offsetX = (world.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetY = world.getRandom().nextDouble() * 2.0;
                double offsetZ = (world.getRandom().nextDouble() - 0.5) * 3.0;

                serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
                        center.getX() + 0.5 + offsetX,
                        center.getY() + 0.5 + offsetY,
                        center.getZ() + 0.5 + offsetZ,
                        2, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    private void spawnBreakParticles(ServerWorld world, BlockPos pos) {
        if (world.getRandom().nextFloat() < 0.4f) {
            world.spawnParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ToolMaterials.NETHERITE.getRepairIngredient().test(ingredient);
    }
}