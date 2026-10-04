package com.tacz.legacy.client.renderer.item

import com.tacz.legacy.api.item.IAttachment
import com.tacz.legacy.client.model.SlotModel
import com.tacz.legacy.client.model.TACZPerspectiveAwareBakedModel
import com.tacz.legacy.client.model.bedrock.BedrockModel
import com.tacz.legacy.client.resource.TACZClientAssetManager
import com.tacz.legacy.client.resource.pojo.display.attachment.AttachmentDisplay
import com.tacz.legacy.common.resource.TACZGunPackPresentation
import com.tacz.legacy.common.resource.TACZGunPackRuntimeRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

/**
 * TEISR for attachment items.
 *
 * Context-aware via [TACZPerspectiveAwareBakedModel]:
 * - **Item presentation contexts** (GUI, dropped, fixed, head) → flat slot texture
 * - **Hand / 3D contexts** → bedrock model
 */
@SideOnly(Side.CLIENT)
internal object TACZAttachmentItemRenderer : TileEntityItemStackRenderer() {
    private val SLOT_MODEL = SlotModel()

    override fun renderByItem(stack: ItemStack) {
        renderByItem(stack, 1.0f)
    }

    override fun renderByItem(stack: ItemStack, partialTicks: Float) {
        val item = stack.item
        if (item !is IAttachment) return

        val attachmentId = item.getAttachmentId(stack)
        val snapshot = TACZGunPackRuntimeRegistry.getSnapshot()

        val displayId = TACZGunPackPresentation.resolveAttachmentDisplayId(snapshot, attachmentId) ?: return
        val display: AttachmentDisplay = TACZClientAssetManager.getAttachmentDisplay(displayId) ?: return

        val transformType = TACZPerspectiveAwareBakedModel.getCurrentTransformType()

        // Item presentation contexts → always use flat slot texture
        if (TACZPerspectiveAwareBakedModel.isItemPresentationContext(transformType)) {
            renderSlotTexture(display)
            return
        }

        // Hand / 3D contexts → try 3D model, else fall back to slot
        val modelLoc: ResourceLocation? = display.model
        val textureLoc: ResourceLocation? = display.texture
        val modelData = if (modelLoc != null) TACZClientAssetManager.getModel(modelLoc) else null
        val model: BedrockModel? = if (modelData != null) BedrockModel(modelData.pojo, modelData.version) else null
        val registeredTexture: ResourceLocation? = if (textureLoc != null) TACZClientAssetManager.getTextureLocation(textureLoc) else null

        if (model == null || registeredTexture == null) {
            renderSlotTexture(display)
            return
        }

        GlStateManager.pushMatrix()
        Minecraft.getMinecraft().textureManager.bindTexture(registeredTexture)

        GlStateManager.translate(0.5f, 2.0f, 0.5f)
        GlStateManager.scale(-1f, -1f, 1f)

        // Fixed context: rotate 90° like upstream
        if (transformType == TransformType.FIXED) {
            GlStateManager.rotate(-90f, 0f, 1f, 0f)
        }

        GlStateManager.enableLighting()
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
        )

        model.render()

        GlStateManager.disableBlend()
        GlStateManager.disableRescaleNormal()
        GlStateManager.popMatrix()
    }

    private fun renderSlotTexture(display: AttachmentDisplay) {
        val slotTexLoc = display.slotTextureLocation ?: return
        val registeredSlot = TACZClientAssetManager.getTextureLocation(slotTexLoc) ?: return
        Minecraft.getMinecraft().textureManager.bindTexture(registeredSlot)

        GlStateManager.pushMatrix()
        GlStateManager.translate(0.5f, 1.5f, 0.5f)
        GlStateManager.rotate(180f, 0f, 0f, 1f)

        // GUI 中的槽位贴图是平面图标，应与原版 2D 物品一样不受光照影响，否则会显得发暗
        if (TACZPerspectiveAwareBakedModel.getCurrentTransformType() == ItemCameraTransforms.TransformType.GUI) {
            GlStateManager.disableLighting()
        } else {
            GlStateManager.enableLighting()
        }
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
        )

        SLOT_MODEL.render()

        GlStateManager.disableBlend()
        GlStateManager.disableRescaleNormal()
        GlStateManager.popMatrix()
    }
}
