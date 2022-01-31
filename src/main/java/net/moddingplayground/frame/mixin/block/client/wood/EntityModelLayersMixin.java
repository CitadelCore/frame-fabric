package net.moddingplayground.frame.mixin.block.client.wood;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;
import net.minecraft.util.SignType;
import net.moddingplayground.frame.api.block.wood.FrameSignType;
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
        if (type instanceof FrameSignType t) {
            Identifier id = t.getId();
            cir.setReturnValue(new EntityModelLayer(new Identifier(id.getNamespace(), "sign/%s".formatted(id.getPath())), MAIN));
        }
    }
}
