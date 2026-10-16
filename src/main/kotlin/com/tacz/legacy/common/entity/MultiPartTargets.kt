package com.tacz.legacy.common.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.MultiPartEntityPart

/**
 * 多部件实体（原版末影龙、寄生虫模组等 Boss）的命中目标解析。
 *
 * 这类实体由本体 + 多个部件实体组成，部件被攻击时会把伤害转发给本体。部件之间的碰撞箱互相重叠，
 * 一发子弹/一次爆炸很容易同时命中多个部件，如果逐个结算伤害，本体会受到数倍伤害。
 * 因此结算前先把部件映射回本体，同一本体只结算一次。
 */
internal object MultiPartTargets {
    private const val OWNER_SEARCH_RANGE: Double = 16.0

    /**
     * 返回伤害的实际承受者：部件 → 本体，普通实体 → 自身。
     * 先识别原版 [MultiPartEntityPart]，再在 [candidates] 与附近实体中查找 getParts() 包含该实体的本体，
     * 以兼容自定义部件类的模组。
     */
    fun rootOf(entity: Entity, candidates: Collection<Entity> = emptyList()): Entity {
        ((entity as? MultiPartEntityPart)?.parent as? Entity)?.let { return it }
        candidates.firstOrNull { owns(it, entity) }?.let { return it }
        // 普通生物不会是部件：跳过开销较大的周边搜索
        if (entity is EntityLivingBase || entity.world == null) {
            return entity
        }
        return entity.world
            .getEntitiesWithinAABBExcludingEntity(entity, entity.entityBoundingBox.grow(OWNER_SEARCH_RANGE))
            .firstOrNull { owns(it, entity) }
            ?: entity
    }

    private fun owns(owner: Entity, part: Entity): Boolean {
        if (owner === part) {
            return false
        }
        val parts = owner.parts ?: return false
        return parts.any { it === part }
    }
}
