package com.tacz.legacy.common.entity.shooter

import net.minecraft.entity.EntityLivingBase

/**
 * 客户端换弹被取消时的通知入口（通用代码不能直接引用客户端类，由 ClientProxy 注册监听）。
 * 客户端据此停止仍在播放的换弹动画。
 */
public object ReloadCancelNotifier {
    @Volatile
    public var clientListener: ((EntityLivingBase) -> Unit)? = null

    internal fun notifyCancelled(shooter: EntityLivingBase) {
        if (shooter.world.isRemote) {
            clientListener?.invoke(shooter)
        }
    }
}
