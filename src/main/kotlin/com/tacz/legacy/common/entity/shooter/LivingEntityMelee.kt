package com.tacz.legacy.common.entity.shooter

import com.tacz.legacy.api.event.GunMeleeEvent
import com.tacz.legacy.api.item.IGun
import com.tacz.legacy.common.network.TACZNetworkHandler
import com.tacz.legacy.common.network.message.event.ServerMessageMelee
import com.tacz.legacy.common.resource.GunDataAccessor
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.DamageSource
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.relauncher.Side

/**
 * 服务端近战逻辑。与上游 TACZ LivingEntityMelee 行为一致。
 */
public class LivingEntityMelee(
    private val shooter: EntityLivingBase,
    private val data: ShooterDataHolder,
    private val draw: LivingEntityDrawGun,
) {
    /**
     * 发起近战攻击。
     */
    public fun melee() {
        if (shooter.world.isRemote) return
        val held = data.heldGun() ?: return
        val currentGunItem = held.stack
        val meleeData = held.gunData.meleeData ?: return

        // 冷却检查
        val meleeCoolDown = getMeleeCoolDown()
        if (meleeCoolDown > 0) return
        // 切枪中不可近战
        if (draw.getDrawCoolDown() != 0L) return
        // 换弹中不可近战
        if (data.reloadStateType.isReloading()) return

        // 发布服务端 Forge 事件
        val meleeEvent = GunMeleeEvent(shooter, currentGunItem, Side.SERVER)
        if (MinecraftForge.EVENT_BUS.post(meleeEvent)) return

        data.meleeTimestamp = System.currentTimeMillis()
        data.meleePrepTickCount = 0

        // 广播近战开始
        TACZNetworkHandler.sendToTrackingEntity(ServerMessageMelee(shooter.entityId, currentGunItem), shooter)

        // 如果有 prep time，延迟伤害；否则立即执行
        val defaultMeleeData = meleeData.defaultMeleeData
        if (defaultMeleeData != null && defaultMeleeData.prepTime <= 0f) {
            executeDefaultMeleeDamage(defaultMeleeData)
        }
    }

    /**
     * 每 tick 检查近战 prep 阶段是否需要执行伤害。
     */
    public fun tickMelee() {
        if (data.meleePrepTickCount < 0) return
        // 切枪后不再持有带近战数据的枪：取消待执行的近战，避免计数器永远停留
        val defaultMeleeData = data.heldGun()?.gunData?.meleeData?.defaultMeleeData
        if (defaultMeleeData == null) {
            data.meleePrepTickCount = -1
            return
        }

        if (defaultMeleeData.prepTime > 0f) {
            data.meleePrepTickCount++
            val elapsed = data.meleePrepTickCount * 50L // 50ms/tick
            if (elapsed >= (defaultMeleeData.prepTime * 1000).toLong()) {
                executeDefaultMeleeDamage(defaultMeleeData)
                data.meleePrepTickCount = -1
            }
        } else {
            data.meleePrepTickCount = -1
        }
    }

    public fun getMeleeCoolDown(): Long {
        val currentGunItem = data.currentGunItem?.get() ?: return 0
        val iGun = currentGunItem.item as? IGun ?: return 0
        val gunData = GunDataAccessor.getGunData(iGun.getGunId(currentGunItem)) ?: return -1

        val meleeData = gunData.meleeData ?: return 0
        if (data.meleeTimestamp < 0) return 0

        val elapsed = System.currentTimeMillis() - data.meleeTimestamp
        val coolDown = (meleeData.cooldown * 1000).toLong() - elapsed
        return if (coolDown < 0) 0 else coolDown
    }

    private fun executeDefaultMeleeDamage(mData: com.tacz.legacy.common.resource.GunDefaultMeleeCombatData) {
        val look = shooter.lookVec
        val eyePos = Vec3d(shooter.posX, shooter.posY + shooter.eyeHeight, shooter.posZ)
        val end = eyePos.add(look.scale(mData.distance.toDouble()))

        // 扇形范围内搜索目标
        val expand = mData.distance.toDouble() * 0.5
        val searchBox = AxisAlignedBB(eyePos.x, eyePos.y, eyePos.z, end.x, end.y, end.z)
            .grow(expand, expand, expand)

        val candidates = shooter.world.getEntitiesWithinAABBExcludingEntity(shooter, searchBox)
        val halfAngleRad = Math.toRadians(mData.rangeAngle.toDouble())
        // 非玩家射手（NPC/怪物）也应造成近战伤害，原实现对非玩家直接跳过
        val damageSource = if (shooter is EntityPlayer) {
            DamageSource.causePlayerDamage(shooter)
        } else {
            DamageSource.causeMobDamage(shooter)
        }

        for (entity in candidates) {
            if (entity !is EntityLivingBase) continue
            val toEntity = Vec3d(
                entity.posX - shooter.posX,
                entity.posY + entity.eyeHeight * 0.5 - (shooter.posY + shooter.eyeHeight),
                entity.posZ - shooter.posZ,
            )
            val dist = toEntity.length()
            if (dist > mData.distance) continue
            val angle = Math.acos(look.dotProduct(toEntity.normalize()).coerceIn(-1.0, 1.0))
            if (angle <= halfAngleRad) {
                entity.attackEntityFrom(damageSource, mData.damage)
            }
        }
    }
}
