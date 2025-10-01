package dev.most0afa.forge.and.fury.mixin;

import dev.most0afa.forge.and.fury.Items.StormCallerBow;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public class ArrowEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if ((Object) this instanceof ArrowEntity arrow) {
            StormCallerBow.handleArrowTick(arrow);
        }
    }

    @Inject(method = "onBlockHit", at = @At("HEAD"))
    private void onBlockHit(BlockHitResult result, CallbackInfo ci) {
        if ((Object) this instanceof ArrowEntity arrow) {
            StormCallerBow.handleArrowImpact(arrow, result.getBlockPos());
        }
    }

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void onEntityHit(EntityHitResult result, CallbackInfo ci) {
        if ((Object) this instanceof ArrowEntity arrow) {
            StormCallerBow.handleArrowImpact(arrow, result.getEntity().getBlockPos());
        }
    }
}