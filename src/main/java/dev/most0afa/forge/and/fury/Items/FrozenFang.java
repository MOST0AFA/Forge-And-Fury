package dev.most0afa.forge.and.fury.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FrozenFang extends Item {
    public FrozenFang(ToolMaterial material, Item.Properties properties) {
        super(properties.sword(material, 3.0F, -2.4F));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();

        if (!level.isClientSide()) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));

            level.playSound(null, BlockPos.containing(target.position()),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
            level.playSound(null, BlockPos.containing(attacker.position()),
                    SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE))) {
            stack.hurtAndBreak(1, miner, EquipmentSlot.MAINHAND);

            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.2F);
            level.playSound(null, BlockPos.containing(miner.position()),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 1.5F);
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }
}
