package com.tacz.legacy.client.animation.third

import com.tacz.legacy.api.entity.IGunOperator
import com.tacz.legacy.api.item.IGun
import com.tacz.legacy.client.resource.TACZClientAssetManager
import com.tacz.legacy.common.item.LegacyRuntimeTooltipSupport
import net.minecraft.client.model.ModelBiped
import net.minecraft.entity.EntityLivingBase
import kotlin.math.cos
import kotlin.math.sin

/**
 * 第三人称持枪姿势。移植自上游 TACZ ThirdPersonManager：
 * 枪包 display 文件中的 third_person_animation 选择姿势（default / minigun），未知名称使用 default。
 *
 * 枪模型的 thirdperson_hand 定位组是按对应姿势的手臂朝向制作的，姿势不对时枪会歪
 * （例如加特林按 default 的平举手臂渲染会变成竖着拿）。
 */
public object ThirdPersonGunPose {
    private const val MINIGUN = "minigun"

    /** 在 ModelBiped.setRotationAngles 计算完原版姿势之后调用 */
    @JvmStatic
    public fun apply(model: ModelBiped, entity: EntityLivingBase) {
        val stack = entity.heldItemMainhand
        val iGun = stack.item as? IGun ?: return
        // 睡觉、爬梯、鞘翅飞行时保持原版姿势（与上游一致）
        if (entity.isPlayerSleeping || entity.isOnLadder || entity.isElytraFlying) {
            return
        }
        val animation = LegacyRuntimeTooltipSupport.resolveGunDisplayId(stack, iGun)
            ?.let(TACZClientAssetManager::getGunDisplay)
            ?.thirdPersonAnimation
        if (animation == MINIGUN) {
            animateMinigun(model)
            return
        }
        val aimingProgress = IGunOperator.fromLivingEntity(entity).getSynAimingProgress()
        if (aimingProgress <= 0f) {
            animateDefaultHold(model)
        } else {
            animateDefaultAim(model, aimingProgress)
        }
    }

    private fun animateDefaultHold(model: ModelBiped) {
        val head = model.bipedHead
        model.bipedRightArm.rotateAngleY = -0.3f + head.rotateAngleY
        model.bipedLeftArm.rotateAngleY = 0.8f + head.rotateAngleY
        model.bipedRightArm.rotateAngleX = -1.4f + head.rotateAngleX
        model.bipedLeftArm.rotateAngleX = -1.4f + head.rotateAngleX
    }

    private fun animateDefaultAim(model: ModelBiped, aimProgress: Float) {
        val head = model.bipedHead
        val yaw = lerp(aimProgress, 0.3f, 0.35f)
        val pitch = lerp(aimProgress, 1.4f, 1.6f)
        model.bipedRightArm.rotateAngleY = -yaw + head.rotateAngleY
        model.bipedLeftArm.rotateAngleY = 0.8f + head.rotateAngleY
        model.bipedRightArm.rotateAngleX = -pitch + head.rotateAngleX
        model.bipedLeftArm.rotateAngleX = -pitch + head.rotateAngleX
    }

    /** 加特林：身体侧转，右手垂在腰间托住枪身，左手向前扶住 */
    private fun animateMinigun(model: ModelBiped) {
        val body = model.bipedBody
        val rightArm = model.bipedRightArm
        val leftArm = model.bipedLeftArm
        body.rotateAngleY = model.bipedHead.rotateAngleY + 0.8f

        val cosTheta = cos(-body.rotateAngleY)
        val sinTheta = sin(-body.rotateAngleY)

        val rightX = rightArm.rotationPointX
        rightArm.rotationPointX = rightX * cosTheta
        rightArm.rotationPointZ = rightX * sinTheta
        rightArm.rotateAngleY = -1.0f + body.rotateAngleY
        rightArm.rotateAngleX = -0.1f + body.rotateAngleX

        val leftX = leftArm.rotationPointX
        leftArm.rotationPointX = leftX * cosTheta
        leftArm.rotationPointZ = leftX * sinTheta
        leftArm.rotateAngleY = -0.1f + body.rotateAngleY
        leftArm.rotateAngleX = -1.0f + body.rotateAngleX
    }

    private fun lerp(t: Float, from: Float, to: Float): Float = from + (to - from) * t
}
