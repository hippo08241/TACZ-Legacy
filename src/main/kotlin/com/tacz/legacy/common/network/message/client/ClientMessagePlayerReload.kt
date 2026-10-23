package com.tacz.legacy.common.network.message.client

import com.tacz.legacy.api.entity.IGunOperator
import com.tacz.legacy.common.network.TACZNetworkHandler
import com.tacz.legacy.common.network.message.event.ServerMessageReloadRejected
import io.netty.buffer.ByteBuf
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * C2S: 换弹请求。
 */
public class ClientMessagePlayerReload() : IMessage, IMessageHandler<ClientMessagePlayerReload, IMessage?> {

    override fun fromBytes(buf: ByteBuf) {}
    override fun toBytes(buf: ByteBuf) {}

    override fun onMessage(message: ClientMessagePlayerReload, ctx: MessageContext): IMessage? {
        val player: EntityPlayerMP = ctx.serverHandler.player
        ctx.serverHandler.player.serverWorld.addScheduledTask {
            val operator = IGunOperator.fromLivingEntity(player)
            operator.reload()
            // 客户端只在本地成功进入换弹时才发送请求；服务端没能进入换弹说明两侧状态不一致，
            // 通知客户端取消换弹并停止动画
            if (!operator.getSynReloadState().stateType.isReloading()) {
                TACZNetworkHandler.sendToPlayer(ServerMessageReloadRejected(), player)
            }
        }
        return null
    }
}
