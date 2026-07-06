package com.tacz.legacy.client.animation.third

import com.tacz.legacy.api.client.other.ThirdPersonManager
import com.tacz.legacy.api.entity.IGunOperator
import com.tacz.legacy.api.item.IGun
import com.tacz.legacy.client.resource.TACZClientAssetManager
import com.tacz.legacy.client.resource.pojo.display.gun.GunDisplay
import com.tacz.legacy.common.item.LegacyRuntimeTooltipSupport
import com.tacz.legacy.common.resource.TACZGunPackPresentation
import com.tacz.legacy.common.resource.TACZGunPackRuntimeRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.model.ModelRenderer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack

/**
 * Applies third-person arm poses for gun holding. Port of upstream InnerThirdPersonManager for 1.12.2.
 */
internal object InnerThirdPersonManager {
    @JvmStatic
    fun setRotationAnglesHead(
        entityIn: EntityLivingBase,
        rightArm: ModelRenderer,
        leftArm: ModelRenderer,
        body: ModelRenderer,
        head: ModelRenderer,
        @Suppress("UNUSED_PARAMETER") limbSwingAmount: Float,
    ) {
        if (Minecraft.getMinecraft().isGamePaused) {
            return
        }
        if (entityIn !is IGunOperator) {
            return
        }
        val mainHandItem: ItemStack = entityIn.heldItemMainhand
        val iGun = IGun.getIGunOrNull(mainHandItem) ?: return
        if (entityIn.isPlayerSleeping || entityIn.isOnLadder || entityIn.isInWater) {
            return
        }

        val display = resolveGunDisplay(mainHandItem, iGun) ?: return
        val aimingProgress = entityIn.getSynAimingProgress()
        val animationName = display.thirdPersonAnimation?.takeIf { it.isNotEmpty() } ?: "default"
        val animation = ThirdPersonManager.getAnimation(animationName)
        if (aimingProgress <= 0f) {
            animation.animateGunHold(entityIn, rightArm, leftArm, body, head)
        } else {
            animation.animateGunAim(entityIn, rightArm, leftArm, body, head, aimingProgress)
        }
    }

    private fun resolveGunDisplay(stack: ItemStack, iGun: IGun): GunDisplay? {
        val gunId = iGun.getGunId(stack)
        var displayId = LegacyRuntimeTooltipSupport.resolveGunDisplayId(stack, iGun)
        if (displayId == null) {
            displayId = TACZGunPackPresentation.resolveGunDisplayId(TACZGunPackRuntimeRegistry.getSnapshot(), gunId)
        }
        return displayId?.let { TACZClientAssetManager.getGunDisplay(it) }
    }
}
