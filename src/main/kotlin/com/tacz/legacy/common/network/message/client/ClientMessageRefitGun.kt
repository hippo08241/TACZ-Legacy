package com.tacz.legacy.common.network.message.client

import com.tacz.legacy.api.item.attachment.AttachmentType
import com.tacz.legacy.common.application.refit.LegacyGunRefitRuntime
import com.tacz.legacy.common.network.TACZNetworkHandler
import com.tacz.legacy.common.network.message.event.ServerMessageRefreshRefitScreen
import io.netty.buffer.ByteBuf
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.items.ItemHandlerHelper

public class ClientMessageRefitGun() : IMessage, IMessageHandler<ClientMessageRefitGun, IMessage?> {
    private var attachmentSlotIndex: Int = -1
    private var gunSlotIndex: Int = -1
    private var attachmentType: AttachmentType = AttachmentType.NONE

    public constructor(attachmentSlotIndex: Int, gunSlotIndex: Int, attachmentType: AttachmentType) : this() {
        this.attachmentSlotIndex = attachmentSlotIndex
        this.gunSlotIndex = gunSlotIndex
        this.attachmentType = attachmentType
    }

    override fun fromBytes(buf: ByteBuf) {
        attachmentSlotIndex = buf.readInt()
        gunSlotIndex = buf.readInt()
        attachmentType = AttachmentType.values().getOrElse(buf.readInt()) { AttachmentType.NONE }
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(attachmentSlotIndex)
        buf.writeInt(gunSlotIndex)
        buf.writeInt(attachmentType.ordinal)
    }

    override fun onMessage(message: ClientMessageRefitGun, ctx: MessageContext): IMessage? {
        val player: EntityPlayerMP = ctx.serverHandler.player
        player.serverWorld.addScheduledTask {
            if (message.attachmentSlotIndex !in 0 until player.inventory.sizeInventory) {
                return@addScheduledTask
            }
            if (message.gunSlotIndex !in 0 until player.inventory.sizeInventory) {
                return@addScheduledTask
            }
            if (message.attachmentSlotIndex == message.gunSlotIndex) {
                return@addScheduledTask
            }
            val gunStack = player.inventory.getStackInSlot(message.gunSlotIndex)
            val attachmentStack = player.inventory.getStackInSlot(message.attachmentSlotIndex)
            val swapResult = LegacyGunRefitRuntime.swapAttachment(gunStack, attachmentStack, message.attachmentType) ?: return@addScheduledTask
            // 只消耗一个配件；若槽位里还有剩余，则把卸下的旧配件另行返还给玩家，避免整组被覆盖丢失
            val remaining = attachmentStack.copy().apply { shrink(1) }
            if (remaining.isEmpty) {
                player.inventory.setInventorySlotContents(message.attachmentSlotIndex, swapResult.previousAttachment)
            } else {
                player.inventory.setInventorySlotContents(message.attachmentSlotIndex, remaining)
                if (!swapResult.previousAttachment.isEmpty) {
                    ItemHandlerHelper.giveItemToPlayer(player, swapResult.previousAttachment)
                }
            }
            if (swapResult.requiresAmmoRefund) {
                LegacyGunRefitRuntime.refundLoadedAmmo(gunStack, player.capabilities.isCreativeMode).refundStacks.forEach { stack ->
                    ItemHandlerHelper.giveItemToPlayer(player, stack)
                }
            }
            player.inventory.markDirty()
            player.inventoryContainer.detectAndSendChanges()
            TACZNetworkHandler.sendToPlayer(ServerMessageRefreshRefitScreen(), player)
        }
        return null
    }
}
