package com.tacz.legacy.api.client.other;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;

/**
 * Third-person arm pose when holding a gun. Port of upstream TACZ IThirdPersonAnimation for 1.12.2.
 */
public interface IThirdPersonAnimation {
    void animateGunHold(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head);

    void animateGunAim(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head, float aimProgress);
}
