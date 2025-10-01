package dev.most0afa.forge.and.fury.Items;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FrozenFang extends SwordItem {
    public FrozenFang(ToolMaterial material, Settings settings) {
        super(material, settings.attributeModifiers(SwordItem.createAttributeModifiers(material, 3, -2.4F)));
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        World world = attacker.getEntityWorld();

        if (!world.isClient) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));

            stack.damage(1, attacker, EquipmentSlot.MAINHAND);

            world.playSound(null, BlockPos.ofFloored(target.getPos()),
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8F, 1.2F);
            world.playSound(null, BlockPos.ofFloored(attacker.getPos()),
                    SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.5F, 1.0F);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && (state.isOf(Blocks.ICE) || state.isOf(Blocks.PACKED_ICE) || state.isOf(Blocks.BLUE_ICE))) {
            stack.damage(1, miner, EquipmentSlot.MAINHAND);

            world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.0F, 1.2F);
            world.playSound(null, BlockPos.ofFloored(miner.getPos()),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.3F, 1.5F);
        }
        return super.postMine(stack, world, state, pos, miner);
    }
}