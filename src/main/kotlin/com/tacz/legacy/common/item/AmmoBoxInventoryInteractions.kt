package com.tacz.legacy.common.item

import com.tacz.legacy.api.DefaultAssets
import com.tacz.legacy.api.item.IAmmo
import com.tacz.legacy.api.item.IAmmoBox
import com.tacz.legacy.common.config.LegacyConfigManager
import com.tacz.legacy.common.resource.TACZGunPackRuntimeRegistry
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.SoundCategory
import kotlin.math.min

internal object AmmoBoxInventoryInteractions {
    @JvmStatic
    fun tryHandleSlotClick(
        container: Container,
        slotId: Int,
        dragType: Int,
        clickType: ClickType,
        player: EntityPlayer,
    ): Boolean {
        if (clickType != ClickType.PICKUP || dragType != 1 || slotId < 0) {
            return false
        }
        val slot = container.getSlot(slotId) ?: return false
        if (!slot.canTakeStack(player)) {
            return false
        }

        val cursor = player.inventory.itemStack
        val slotStack = slot.stack

        if (!cursor.isEmpty && cursor.item is IAmmoBox) {
            val boxItem = cursor.item as IAmmoBox
            return if (slotStack.isEmpty) {
                withdrawAmmo(cursor, boxItem, slot, player)
            } else {
                depositAmmo(cursor, boxItem, slot, slotStack, player)
            }
        }

        if (!slotStack.isEmpty && slotStack.item is IAmmoBox) {
            val boxItem = slotStack.item as IAmmoBox
            return if (cursor.isEmpty) {
                withdrawAmmoIntoCursor(slotStack, boxItem, slot, player)
            } else {
                depositAmmoFromCursor(slotStack, boxItem, cursor, slot, player)
            }
        }

        return false
    }

    private fun withdrawAmmo(
        ammoBox: ItemStack,
        boxItem: IAmmoBox,
        slot: Slot,
        player: EntityPlayer,
    ): Boolean {
        if (boxItem.isAllTypeCreative(ammoBox) || boxItem.isCreative(ammoBox)) {
            return false
        }
        val boxAmmoId = boxItem.getAmmoId(ammoBox)
        if (boxAmmoId == DefaultAssets.EMPTY_AMMO_ID) {
            return false
        }
        val boxAmmoCount = boxItem.getAmmoCount(ammoBox)
        if (boxAmmoCount <= 0) {
            return false
        }
        val ammoDef = TACZGunPackRuntimeRegistry.getSnapshot().ammos[boxAmmoId] ?: return false
        val takeCount = min(ammoDef.stackSize, boxAmmoCount)
        val takeAmmo = ItemStack(LegacyItems.AMMO, takeCount)
        (LegacyItems.AMMO as IAmmo).setAmmoId(takeAmmo, boxAmmoId)
        slot.putStack(takeAmmo)
        val remainCount = boxAmmoCount - takeCount
        boxItem.setAmmoCount(ammoBox, remainCount)
        if (remainCount <= 0) {
            boxItem.setAmmoId(ammoBox, DefaultAssets.EMPTY_AMMO_ID)
        }
        slot.onSlotChanged()
        player.inventory.itemStack = ammoBox
        playRemoveSound(player)
        return true
    }

    private fun depositAmmo(
        ammoBox: ItemStack,
        boxItem: IAmmoBox,
        slot: Slot,
        slotStack: ItemStack,
        player: EntityPlayer,
    ): Boolean {
        if (boxItem.isAllTypeCreative(ammoBox)) {
            return false
        }
        val slotAmmo = slotStack.item as? IAmmo ?: return false
        val slotAmmoId = slotAmmo.getAmmoId(slotStack)
        if (slotAmmoId == DefaultAssets.EMPTY_AMMO_ID) {
            return false
        }
        var boxAmmoId = boxItem.getAmmoId(ammoBox)
        if (boxAmmoId == DefaultAssets.EMPTY_AMMO_ID) {
            boxItem.setAmmoId(ammoBox, slotAmmoId)
            boxAmmoId = slotAmmoId
        } else if (slotAmmoId != boxAmmoId) {
            return false
        }
        val ammoDef = TACZGunPackRuntimeRegistry.getSnapshot().ammos[slotAmmoId] ?: return false
        if (boxItem.isCreative(ammoBox)) {
            boxItem.setAmmoCount(ammoBox, Int.MAX_VALUE)
            slot.onSlotChanged()
            player.inventory.itemStack = ammoBox
            playInsertSound(player)
            return true
        }
        val boxAmmoCount = boxItem.getAmmoCount(ammoBox)
        val levelMultiplier = boxItem.getAmmoLevel(ammoBox) + 1
        val maxSize = ammoDef.stackSize * LegacyConfigManager.server.ammoBoxStackSize * levelMultiplier
        val needCount = maxSize - boxAmmoCount
        if (needCount <= 0) {
            return false
        }
        val takeCount = min(slotStack.count, needCount)
        if (takeCount <= 0) {
            return false
        }
        slot.decrStackSize(takeCount)
        boxItem.setAmmoCount(ammoBox, boxAmmoCount + takeCount)
        slot.onSlotChanged()
        player.inventory.itemStack = ammoBox
        playInsertSound(player)
        return true
    }

    private fun withdrawAmmoIntoCursor(
        ammoBox: ItemStack,
        boxItem: IAmmoBox,
        slot: Slot,
        player: EntityPlayer,
    ): Boolean {
        if (boxItem.isAllTypeCreative(ammoBox) || boxItem.isCreative(ammoBox)) {
            return false
        }
        val boxAmmoId = boxItem.getAmmoId(ammoBox)
        if (boxAmmoId == DefaultAssets.EMPTY_AMMO_ID) {
            return false
        }
        val boxAmmoCount = boxItem.getAmmoCount(ammoBox)
        if (boxAmmoCount <= 0) {
            return false
        }
        val ammoDef = TACZGunPackRuntimeRegistry.getSnapshot().ammos[boxAmmoId] ?: return false
        val takeCount = min(ammoDef.stackSize, boxAmmoCount)
        val takeAmmo = ItemStack(LegacyItems.AMMO, takeCount)
        (LegacyItems.AMMO as IAmmo).setAmmoId(takeAmmo, boxAmmoId)
        player.inventory.itemStack = takeAmmo
        val remainCount = boxAmmoCount - takeCount
        boxItem.setAmmoCount(ammoBox, remainCount)
        if (remainCount <= 0) {
            boxItem.setAmmoId(ammoBox, DefaultAssets.EMPTY_AMMO_ID)
        }
        slot.onSlotChanged()
        playRemoveSound(player)
        return true
    }

    private fun depositAmmoFromCursor(
        ammoBox: ItemStack,
        boxItem: IAmmoBox,
        cursor: ItemStack,
        slot: Slot,
        player: EntityPlayer,
    ): Boolean {
        if (boxItem.isAllTypeCreative(ammoBox)) {
            return false
        }
        val cursorAmmo = cursor.item as? IAmmo ?: return false
        val cursorAmmoId = cursorAmmo.getAmmoId(cursor)
        if (cursorAmmoId == DefaultAssets.EMPTY_AMMO_ID) {
            return false
        }
        var boxAmmoId = boxItem.getAmmoId(ammoBox)
        if (boxAmmoId == DefaultAssets.EMPTY_AMMO_ID) {
            boxItem.setAmmoId(ammoBox, cursorAmmoId)
            boxAmmoId = cursorAmmoId
        } else if (cursorAmmoId != boxAmmoId) {
            return false
        }
        val ammoDef = TACZGunPackRuntimeRegistry.getSnapshot().ammos[cursorAmmoId] ?: return false
        if (boxItem.isCreative(ammoBox)) {
            boxItem.setAmmoCount(ammoBox, Int.MAX_VALUE)
            slot.onSlotChanged()
            playInsertSound(player)
            return true
        }
        val boxAmmoCount = boxItem.getAmmoCount(ammoBox)
        val levelMultiplier = boxItem.getAmmoLevel(ammoBox) + 1
        val maxSize = ammoDef.stackSize * LegacyConfigManager.server.ammoBoxStackSize * levelMultiplier
        val needCount = maxSize - boxAmmoCount
        if (needCount <= 0) {
            return false
        }
        val takeCount = min(cursor.count, needCount)
        if (takeCount <= 0) {
            return false
        }
        cursor.shrink(takeCount)
        player.inventory.itemStack = if (cursor.isEmpty) ItemStack.EMPTY else cursor
        boxItem.setAmmoCount(ammoBox, boxAmmoCount + takeCount)
        slot.onSlotChanged()
        playInsertSound(player)
        return true
    }

    private fun playInsertSound(player: EntityPlayer) {
        player.world.playSound(
            null,
            player.posX,
            player.posY,
            player.posZ,
            SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM,
            SoundCategory.PLAYERS,
            0.8f,
            0.8f + player.world.rand.nextFloat() * 0.4f,
        )
    }

    private fun playRemoveSound(player: EntityPlayer) {
        player.world.playSound(
            null,
            player.posX,
            player.posY,
            player.posZ,
            SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM,
            SoundCategory.PLAYERS,
            0.8f,
            0.8f + player.world.rand.nextFloat() * 0.4f,
        )
    }
}