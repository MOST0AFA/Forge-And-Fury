package dev.most0afa.forge.and.fury.mixin;

import dev.most0afa.forge.and.fury.Items.Duskrend;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(
            method = "hurtServer",
            at = @At("RETURN")
    )
    private void postDuskrendEffects(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (source == null || !(source.getEntity() instanceof Player player)) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof Duskrend)) return;

        BlockPos pos = player.blockPosition();

        float healAmount = amount * 0.25f;
        player.heal(healAmount);

        level.playSound(null, pos, SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                player.getX(), player.getY() + 1.0, player.getZ(),
                1, 0.0D, 0.1D, 0.0D, 0.0D);
    }
}
