package dev.most0afa.forge.and.fury.mixin;

import dev.most0afa.forge.and.fury.Items.Duskrend;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {


    @Inject(
            method = "damage",
            at = @At("RETURN")
    )
    private void postDuskrendEffects(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (source == null || !(source.getAttacker() instanceof PlayerEntity player)) return;

        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof Duskrend)) return;

        World world = player.getWorld();
        BlockPos pos = player.getBlockPos();

        float healAmount = amount * 0.25f;
        player.heal(healAmount);


        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    1, 0.0D, 0.1D, 0.0D, 0.0D);
        }
    }
}