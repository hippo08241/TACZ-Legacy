package com.tacz.legacy.mixin.minecraft.client;

import com.tacz.legacy.api.item.IGun;
import com.tacz.legacy.client.animation.third.ThirdPersonGunPose;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public abstract class ModelBipedGunPoseMixin {

    @Shadow
    public ModelBiped.ArmPose rightArmPose;

    @Inject(method = "setRotationAngles", at = @At("HEAD"))
    private void tacz$forceGunArmPose(
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, float scaleFactor,
            Entity entityIn, CallbackInfo ci
    ) {
        if (!(entityIn instanceof EntityLivingBase)) {
            return;
        }
        ItemStack mainHand = ((EntityLivingBase) entityIn).getHeldItemMainhand();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof IGun) {
            this.rightArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
        }
    }

    /**
     * 原版姿势计算完成后，按枪包指定的第三人称动画（default / minigun）覆盖手臂与身体角度。
     * ModelPlayer 在调用 super.setRotationAngles 之后才把角度复制给外层皮肤，因此在这里修改即可同步到袖子。
     */
    @Inject(method = "setRotationAngles", at = @At("RETURN"))
    private void tacz$applyThirdPersonGunPose(
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, float scaleFactor,
            Entity entityIn, CallbackInfo ci
    ) {
        if (entityIn instanceof EntityLivingBase) {
            ThirdPersonGunPose.apply((ModelBiped) (Object) this, (EntityLivingBase) entityIn);
        }
    }
}