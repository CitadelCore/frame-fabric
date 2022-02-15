package net.moddingplayground.frame.mixin.rendering.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.SignType;
import net.moddingplayground.frame.api.rendering.v0.SignTextureProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(EntityModelLayers.class)
public abstract class EntityModelLayersMixin {
    @Shadow @Final private static String MAIN;

    @Inject(method = "createSign", at = @At("HEAD"), cancellable = true)
    private static void onCreateSign(SignType type, CallbackInfoReturnable<EntityModelLayer> cir) {
        if (type instanceof SignTextureProvider provider) cir.setReturnValue(provider.getEntityModelLayer(type, MAIN));
    }
}
