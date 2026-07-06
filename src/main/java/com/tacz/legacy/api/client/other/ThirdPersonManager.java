package com.tacz.legacy.api.client.other;

import com.google.common.collect.Maps;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;

import java.util.Map;

/**
 * Simple third-person gun-hold animation registry. Port of upstream TACZ ThirdPersonManager for 1.12.2.
 */
public final class ThirdPersonManager {
    private static final Map<String, IThirdPersonAnimation> CACHE = Maps.newHashMap();
    private static final String RESERVED_DEFAULT_NAME = "default";

    private static final IThirdPersonAnimation DEFAULT = new IThirdPersonAnimation() {
        @Override
        public void animateGunHold(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head) {
            rightArm.rotateAngleY = -0.3F + head.rotateAngleY;
            leftArm.rotateAngleY = 0.8F + head.rotateAngleY;
            rightArm.rotateAngleX = -1.4F + head.rotateAngleX;
            leftArm.rotateAngleX = -1.4F + head.rotateAngleX;
        }

        @Override
        public void animateGunAim(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head, float aimProgress) {
            float lerp1 = 0.3f + (0.35f - 0.3f) * aimProgress;
            float lerp2 = 1.4f + (1.6f - 1.4f) * aimProgress;
            rightArm.rotateAngleY = -lerp1 + head.rotateAngleY;
            leftArm.rotateAngleY = 0.8F + head.rotateAngleY;
            rightArm.rotateAngleX = -lerp2 + head.rotateAngleX;
            leftArm.rotateAngleX = -lerp2 + head.rotateAngleX;
        }
    };

    private static final String MINI_GUN_NAME = "minigun";
    private static final IThirdPersonAnimation MINI_GUN = new IThirdPersonAnimation() {
        @Override
        public void animateGunHold(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head) {
            body.rotateAngleY = head.rotateAngleY + 0.8f;

            double cosTheta = Math.cos(-body.rotateAngleY);
            double sinTheta = Math.sin(-body.rotateAngleY);

            float x = rightArm.offsetX;
            rightArm.offsetX = (float) (x * cosTheta);
            rightArm.offsetZ = (float) (x * sinTheta);

            rightArm.rotateAngleY = -1.0F + body.rotateAngleY;
            rightArm.rotateAngleX = -0.1F + body.rotateAngleX;

            float x2 = leftArm.offsetX;
            leftArm.offsetX = (float) (x2 * cosTheta);
            leftArm.offsetZ = (float) (x2 * sinTheta);

            leftArm.rotateAngleY = -0.1F + body.rotateAngleY;
            leftArm.rotateAngleX = -1F + body.rotateAngleX;
        }

        @Override
        public void animateGunAim(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head, float aimProgress) {
            animateGunHold(entity, rightArm, leftArm, body, head);
        }
    };

    private ThirdPersonManager() {
    }

    public static void registerDefault() {
        CACHE.put(RESERVED_DEFAULT_NAME, DEFAULT);
        CACHE.put(MINI_GUN_NAME, MINI_GUN);
    }

    public static void register(String name, IThirdPersonAnimation animation) {
        if (RESERVED_DEFAULT_NAME.equals(name)) {
            return;
        }
        CACHE.put(name, animation);
    }

    public static IThirdPersonAnimation getAnimation(String name) {
        if (name == null || name.isEmpty()) {
            return DEFAULT;
        }
        IThirdPersonAnimation animation = CACHE.get(name);
        return animation != null ? animation : DEFAULT;
    }
}
