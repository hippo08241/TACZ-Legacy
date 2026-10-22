package com.tacz.legacy.common.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.MultiPartEntityPart
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * 多部件实体（原版末影龙、寄生虫模组等 Boss）的命中目标解析。
 *
 * 这类实体由本体 + 多个部件实体组成，部件被攻击时会把伤害转发给本体。部件之间的碰撞箱互相重叠，
 * 一发子弹/一次爆炸很容易同时命中多个部件，如果逐个结算伤害，本体会受到数倍伤害。
 * 因此结算前先把部件映射回本体，同一本体只结算一次。
 */
internal object MultiPartTargets {
    private const val OWNER_SEARCH_RANGE: Double = 16.0
    private const val MAX_PARENT_DEPTH: Int = 4

    /** 每个部件类的 “parent” 字段（没有则为 empty），反射查找结果按类缓存 */
    private val parentFields: MutableMap<Class<*>, Optional<Field>> = ConcurrentHashMap()

    /**
     * 返回伤害的实际承受者：部件 → 本体，普通实体 → 自身。依次识别：
     * 1. 原版 [MultiPartEntityPart]（末影龙、寄生虫模组的 EntityHitbox）；
     * 2. 持有 parent 字段的自定义部件（寄生虫模组 Beckon 等使用的 EntityBody 直接继承 Entity）；
     * 3. 在 [candidates] 与附近实体中查找 getParts() 包含该实体的本体。
     */
    fun rootOf(entity: Entity, candidates: Collection<Entity> = emptyList()): Entity {
        var current = entity
        repeat(MAX_PARENT_DEPTH) {
            val parent = directParentOf(current, candidates) ?: return current
            if (parent === current) {
                return current
            }
            current = parent
        }
        return current
    }

    private fun directParentOf(entity: Entity, candidates: Collection<Entity>): Entity? {
        ((entity as? MultiPartEntityPart)?.parent as? Entity)?.let { return it }
        // 普通生物不会是部件：跳过反射与开销较大的周边搜索
        if (entity is EntityLivingBase) {
            return null
        }
        reflectiveParent(entity)?.let { return it }
        candidates.firstOrNull { owns(it, entity) }?.let { return it }
        val world = entity.world ?: return null
        return world
            .getEntitiesWithinAABBExcludingEntity(entity, entity.entityBoundingBox.grow(OWNER_SEARCH_RANGE))
            .firstOrNull { owns(it, entity) }
    }

    private fun reflectiveParent(entity: Entity): Entity? {
        val field = parentFields.computeIfAbsent(entity.javaClass) {
            Optional.ofNullable(findParentField(it, Entity::class.java))
        }.orElse(null) ?: return null
        return try {
            field.get(entity) as? Entity
        } catch (e: ReflectiveOperationException) {
            null
        } catch (e: RuntimeException) {
            null
        }
    }

    private fun owns(owner: Entity, part: Entity): Boolean {
        if (owner === part) {
            return false
        }
        val parts = owner.parts ?: return false
        return parts.any { it === part }
    }

    /**
     * 在 [cls] 及其父类（不含 [stopAt]）中查找名称含 “parent” 且类型为 [stopAt] 子类的实例字段。
     * 模组字段名在运行时不会被混淆，因此按名称识别是可靠的。
     */
    internal fun findParentField(cls: Class<*>, stopAt: Class<*>): Field? {
        var current: Class<*>? = cls
        while (current != null && current != stopAt && current != Any::class.java) {
            for (field in current.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) continue
                if (!stopAt.isAssignableFrom(field.type)) continue
                if (!field.name.contains("parent", ignoreCase = true)) continue
                return try {
                    field.isAccessible = true
                    field
                } catch (e: RuntimeException) {
                    null
                }
            }
            current = current.superclass
        }
        return null
    }
}
