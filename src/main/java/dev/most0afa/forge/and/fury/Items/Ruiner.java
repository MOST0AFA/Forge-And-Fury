package dev.most0afa.forge.and.fury.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class Ruiner extends Item {
    private static final int MAX_HARDNESS = 50;

    public Ruiner(ToolMaterial material, Item.Properties properties) {
        super(properties
                .tool(material, BlockTags.MINEABLE_WITH_PICKAXE, 0.0F, 0.0F, 0.0F)
                .durability(2048)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(Identifier.fromNamespaceAndPath("forgeandfury", "ruiner_attack_damage"), 6.0,
                                        AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED,
                                new AttributeModifier(Identifier.fromNamespaceAndPath("forgeandfury", "ruiner_attack_speed"), -2.8,
                                        AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build()));
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return 9.0f;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && miner instanceof Player player) {
            List<BlockPos> blocksToBreak = new ArrayList<>();
            int totalHardness = 0;

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos targetPos = pos.offset(x, y, z);
                        BlockState targetState = level.getBlockState(targetPos);
                        float hardness = targetState.getDestroySpeed(level, targetPos);

                        if (hardness >= 0 && hardness <= MAX_HARDNESS && !targetState.isAir()) {
                            blocksToBreak.add(targetPos);
                            totalHardness += Math.max(1, (int) hardness);
                        }
                    }
                }
            }

            if (!blocksToBreak.isEmpty()) {
                createDestructionEffect(level, pos, blocksToBreak.size());

                for (BlockPos targetPos : blocksToBreak) {
                    level.destroyBlock(targetPos, true, miner, 512);

                    if (level instanceof ServerLevel serverLevel) {
                        spawnBreakParticles(serverLevel, targetPos);
                    }
                }

                int damageAmount = Math.max(1, totalHardness / 10);
                stack.hurtAndBreak(damageAmount, player, EquipmentSlot.MAINHAND);

                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    private void createDestructionEffect(Level level, BlockPos center, int blockCount) {
        if (level instanceof ServerLevel serverLevel) {
            int particleCount = Math.min(50, blockCount * 3);

            for (int i = 0; i < particleCount; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 4.0;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 4.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 4.0;

                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        center.getX() + 0.5 + offsetX,
                        center.getY() + 0.5 + offsetY,
                        center.getZ() + 0.5 + offsetZ,
                        1, 0, 0, 0, 0);
            }

            for (int i = 0; i < blockCount; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetY = level.getRandom().nextDouble() * 2.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 3.0;

                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        center.getX() + 0.5 + offsetX,
                        center.getY() + 0.5 + offsetY,
                        center.getZ() + 0.5 + offsetZ,
                        2, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    private void spawnBreakParticles(ServerLevel level, BlockPos pos) {
        if (level.getRandom().nextFloat() < 0.4f) {
            level.sendParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
