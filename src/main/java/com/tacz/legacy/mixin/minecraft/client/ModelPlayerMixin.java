package com.tacz.legacy.mixin.minecraft.client;

import com.tacz.legacy.client.animation.third.InnerThirdPersonManager;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPlayer.class)
public abstract class ModelPlayerMixin {
    @Shadow
    public ModelRenderer bipedRightArm;
    @Shadow
    public ModelRenderer bipedLeftArm;
    @Shadow
    public ModelRenderer bipedBody;
    @Shadow
    public ModelRenderer bipedHead;

    @Inject(method = "setRotationAngles", at = @At("RETURN"))
    private void tacz$applyThirdPersonGunPose(
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            float scaleFactor,
            Entity entityIn,
            CallbackInfo ci
    ) {
        if (!(entityIn instanceof EntityLivingBase)) {
            return;
        }
        InnerThirdPersonManager.setRotationAnglesHead(
                (EntityLivingBase) entityIn,
                bipedRightArm,
                bipedLeftArm,
                bipedBody,
                bipedHead,
                limbSwingAmount
        );
    }
}
