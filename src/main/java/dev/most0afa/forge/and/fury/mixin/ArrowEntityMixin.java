package dev.most0afa.forge.and.fury.mixin;

import dev.most0afa.forge.and.fury.Items.StormCallerBow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class ArrowEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if ((Object) this instanceof Arrow arrow) {
            StormCallerBow.handleArrowTick(arrow);
        }
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"))
    private void onHitBlock(BlockHitResult result, CallbackInfo ci) {
        if ((Object) this instanceof Arrow arrow) {
            StormCallerBow.handleArrowImpact(arrow, result.getBlockPos());
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void onHitEntity(EntityHitResult result, CallbackInfo ci) {
        if ((Object) this instanceof Arrow arrow) {
            StormCallerBow.handleArrowImpact(arrow, result.getEntity().blockPosition());
        }
    }
}
