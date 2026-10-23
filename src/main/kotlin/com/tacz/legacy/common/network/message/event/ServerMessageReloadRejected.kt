package com.tacz.legacy.common.network.message.event

import com.tacz.legacy.api.entity.IGunOperator
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * S2C: 服务端拒绝了玩家的换弹请求（客户端已经先行进入换弹状态并开始播放动画）。
 * 客户端收到后取消本地换弹状态并停止换弹动画，避免“动画播完了子弹却没有增加”。
 */
public class ServerMessageReloadRejected() : IMessage {
    override fun fromBytes(buf: ByteBuf) {}
    override fun toBytes(buf: ByteBuf) {}

    public class Handler : IMessageHandler<ServerMessageReloadRejected, IMessage?> {
        override fun onMessage(message: ServerMessageReloadRejected, ctx: MessageContext): IMessage? {
            val mc = Minecraft.getMinecraft()
            mc.addScheduledTask {
                val player = mc.player ?: return@addScheduledTask
                IGunOperator.fromLivingEntity(player).cancelReload()
            }
            return null
        }
    }
}
