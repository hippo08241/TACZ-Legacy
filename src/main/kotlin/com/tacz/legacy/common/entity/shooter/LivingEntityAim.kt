package com.tacz.legacy.common.entity.shooter

import net.minecraft.entity.EntityLivingBase

/**
 * 服务端瞄准逻辑。与上游 TACZ LivingEntityAim 行为一致。
 */
public class LivingEntityAim(
    private val shooter: EntityLivingBase,
    private val data: ShooterDataHolder,
    private val draw: LivingEntityDrawGun,
) {
    /**
     * 切换瞄准状态。
     */
    public fun aim(isAiming: Boolean) {
        if (data.heldGun() == null) return

        // 切枪中不允许瞄准
        if (draw.getDrawCoolDown() != 0L) return
        // 在拉栓中不允许瞄准
        if (data.isBolting) return

        data.isAiming = isAiming
        data.aimingTimestamp = System.currentTimeMillis()
    }

    /**
     * 每 tick 更新瞄准进度（0.0 ~ 1.0）。
     */
    public fun tickAimingProgress() {
        val gunData = data.heldGun()?.gunData
        if (gunData == null) {
            data.aimingProgress = 0f
            return
        }

        val aimTimeMs = (gunData.aimTimeS * 1000).toLong()
        if (aimTimeMs <= 0) {
            data.aimingProgress = if (data.isAiming) 1f else 0f
            return
        }

        val tickDeltaMs = 50L // 50ms per tick
        val progressPerTick = tickDeltaMs.toFloat() / aimTimeMs
        if (data.isAiming) {
            data.aimingProgress = (data.aimingProgress + progressPerTick).coerceIn(0f, 1f)
        } else {
            data.aimingProgress = (data.aimingProgress - progressPerTick).coerceIn(0f, 1f)
        }
    }
}
