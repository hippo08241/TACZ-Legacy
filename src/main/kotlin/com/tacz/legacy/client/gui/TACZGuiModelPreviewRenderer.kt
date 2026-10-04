package com.tacz.legacy.client.gui

import com.tacz.legacy.api.item.IAttachment
import com.tacz.legacy.api.item.IAmmo
import com.tacz.legacy.api.item.IGun
import com.tacz.legacy.api.item.attachment.AttachmentType
import com.tacz.legacy.client.event.FirstPersonRenderMatrices
import com.tacz.legacy.client.model.BedrockAnimatedModel
import com.tacz.legacy.client.model.BedrockGunModel
import com.tacz.legacy.client.model.bedrock.BedrockModel
import com.tacz.legacy.client.model.functional.BeamRenderer
import com.tacz.legacy.client.renderer.bloom.TACZBloomBridge
import com.tacz.legacy.client.resource.TACZClientAssetManager
import com.tacz.legacy.client.resource.pojo.TransformScale
import com.tacz.legacy.client.resource.pojo.display.ammo.AmmoDisplay
import com.tacz.legacy.client.resource.pojo.display.attachment.AttachmentDisplay
import com.tacz.legacy.client.resource.pojo.display.block.BlockDisplay
import com.tacz.legacy.client.resource.pojo.display.gun.GunDisplay
import com.tacz.legacy.common.item.LegacyBlockItem
import com.tacz.legacy.common.resource.TACZGunPackPresentation
import com.tacz.legacy.common.resource.TACZGunPackRuntimeRegistry
import com.tacz.legacy.common.resource.TACZRuntimeSnapshot
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11

internal object TACZGuiPreviewResolver {
    internal enum class PreviewKind {
        GUN,
        ATTACHMENT,
        AMMO,
        BLOCK,
    }

    internal data class PreviewTarget(
        val kind: PreviewKind,
        val displayId: ResourceLocation,
    )

    fun resolve(
        stack: ItemStack,
        snapshot: TACZRuntimeSnapshot = TACZGunPackRuntimeRegistry.getSnapshot(),
    ): PreviewTarget? {
        if (stack.isEmpty) {
            return null
        }
        val item = stack.item
        return when (item) {
            is IGun -> TACZGunPackPresentation.resolveGunDisplayId(snapshot, item.getGunId(stack))
                ?.let { PreviewTarget(PreviewKind.GUN, it) }
            is IAttachment -> TACZGunPackPresentation.resolveAttachmentDisplayId(snapshot, item.getAttachmentId(stack))
                ?.let { PreviewTarget(PreviewKind.ATTACHMENT, it) }
            is IAmmo -> TACZGunPackPresentation.resolveAmmoDisplayId(snapshot, item.getAmmoId(stack))
                ?.let { PreviewTarget(PreviewKind.AMMO, it) }
            is LegacyBlockItem -> TACZGunPackPresentation.resolveBlockDisplayId(snapshot, item.getBlockId(stack))
                ?.let { PreviewTarget(PreviewKind.BLOCK, it) }
            else -> null
        }
    }
}

internal object TACZGuiModelPreviewRenderer {
    private data class ResolvedPreviewModel(
        val modelId: ResourceLocation,
        val modelData: TACZClientAssetManager.ModelData,
        val textureLocation: ResourceLocation,
        val scaleMultiplier: Float,
        val verticalOffset: Float,
    )

    /** 模型几何中心（基岩版像素坐标），按 ModelData 缓存 */
    private val modelCenters = java.util.WeakHashMap<TACZClientAssetManager.ModelData, FloatArray?>()

    fun renderStackPreview(
        stack: ItemStack,
        centerX: Float,
        centerY: Float,
        scale: Float,
        yaw: Float = -30.0f,
        pitch: Float = 12.0f,
        refitAttachmentType: AttachmentType = AttachmentType.NONE,
    ): Boolean {
        if (stack.isEmpty || scale <= 0.0f) {
            return false
        }
        val target = TACZGuiPreviewResolver.resolve(stack) ?: return false
        if (target.kind == TACZGuiPreviewResolver.PreviewKind.GUN && renderGunStackPreview(stack, target.displayId, centerX, centerY, scale, yaw, pitch, refitAttachmentType)) {
            return true
        }
        val resolved = when (target.kind) {
            TACZGuiPreviewResolver.PreviewKind.GUN -> resolveGun(target.displayId)
            TACZGuiPreviewResolver.PreviewKind.ATTACHMENT -> resolveAttachment(target.displayId)
            TACZGuiPreviewResolver.PreviewKind.AMMO -> resolveAmmo(target.displayId)
            TACZGuiPreviewResolver.PreviewKind.BLOCK -> resolveBlock(target.displayId)
        } ?: return false
        renderModel(resolved, centerX, centerY, scale, yaw, pitch)
        return true
    }

    private fun renderGunStackPreview(
        stack: ItemStack,
        displayId: ResourceLocation,
        centerX: Float,
        centerY: Float,
        scale: Float,
        yaw: Float,
        pitch: Float,
        refitAttachmentType: AttachmentType,
    ): Boolean {
        val display: GunDisplay = TACZClientAssetManager.getGunDisplay(displayId) ?: return false
        val displayInstance = TACZClientAssetManager.getGunDisplayInstance(displayId) ?: return false
        val model = displayInstance.gunModel ?: return false
        val textureId = displayInstance.modelTexture ?: return false
        val textureLocation = TACZClientAssetManager.getTextureLocation(textureId) ?: return false
        val scaleMultiplier = transformScaleMultiplier(display.transform?.scale, 1.0f)

        val mc = Minecraft.getMinecraft()
        mc.textureManager.bindTexture(textureLocation)

        GlStateManager.pushMatrix()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.translate(centerX, centerY, 300.0f)
        GlStateManager.scale(scale * scaleMultiplier, scale * scaleMultiplier, scale * scaleMultiplier)
        GlStateManager.rotate(180.0f, 0.0f, 0.0f, 1.0f)
        GlStateManager.rotate(yaw, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(pitch, 1.0f, 0.0f, 0.0f)
        applyPreviewViewMatrix(model, refitAttachmentType)
        GlStateManager.translate(0.0f, 1.8f, 0.0f)
        GlStateManager.scale(-1.0f, -1.0f, 1.0f)

        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.enableLighting()
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
        )

        displayInstance.setActiveGunTexture(textureLocation)
        model.renderHand = false
        val previousBeamContext = BeamRenderer.pushRenderContext(BeamRenderer.RenderContext.GUI_PREVIEW)
        try {
            model.render(stack)
        } finally {
            BeamRenderer.popRenderContext(previousBeamContext)
        }
        captureBloomIfSupported(textureLocation, model) {
            model.renderHand = false
            model.renderBloom(stack)
        }
        model.cleanAnimationTransform()
        model.cleanCameraAnimationTransform()

        GlStateManager.enableCull()
        GlStateManager.disableBlend()
        GlStateManager.disableRescaleNormal()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.popMatrix()
        return true
    }

    private fun applyPreviewViewMatrix(model: BedrockGunModel, refitAttachmentType: AttachmentType) {
        val matrix = previewViewMatrix(model, refitAttachmentType)
        GlStateManager.translate(0.0f, 1.5f, 0.0f)
        multiplyMatrix(matrix)
        GlStateManager.translate(0.0f, -1.5f, 0.0f)
    }

    private fun previewViewMatrix(model: BedrockGunModel, refitAttachmentType: AttachmentType): Matrix4f {
        val nodePath = FirstPersonRenderMatrices.fromBedrockPath(model.getRefitAttachmentViewPath(refitAttachmentType))
        return FirstPersonRenderMatrices.buildPositioningNodeInverse(nodePath)
    }

    private fun multiplyMatrix(matrix: Matrix4f) {
        MATRIX_BUFFER.clear()
        matrix.get(MATRIX_BUFFER)
        MATRIX_BUFFER.rewind()
        GL11.glMultMatrix(MATRIX_BUFFER)
    }

    private fun resolveGun(displayId: ResourceLocation): ResolvedPreviewModel? {
        val display: GunDisplay = TACZClientAssetManager.getGunDisplay(displayId) ?: return null
        val modelId = display.modelLocation ?: return null
        val textureId = display.modelTexture ?: return null
        val modelData = TACZClientAssetManager.getModel(modelId) ?: return null
        val textureLocation = TACZClientAssetManager.getTextureLocation(textureId) ?: return null
        return ResolvedPreviewModel(
            modelId = modelId,
            modelData = modelData,
            textureLocation = textureLocation,
            scaleMultiplier = transformScaleMultiplier(display.transform?.scale, 1.0f),
            verticalOffset = 1.8f,
        )
    }

    private fun resolveAttachment(displayId: ResourceLocation): ResolvedPreviewModel? {
        val display: AttachmentDisplay = TACZClientAssetManager.getAttachmentDisplay(displayId) ?: return null
        val modelId = display.model ?: return null
        val textureId = display.texture ?: return null
        val modelData = TACZClientAssetManager.getModel(modelId) ?: return null
        val textureLocation = TACZClientAssetManager.getTextureLocation(textureId) ?: return null
        return ResolvedPreviewModel(
            modelId = modelId,
            modelData = modelData,
            textureLocation = textureLocation,
            scaleMultiplier = 1.0f,
            verticalOffset = 1.65f,
        )
    }

    private fun resolveAmmo(displayId: ResourceLocation): ResolvedPreviewModel? {
        val display: AmmoDisplay = TACZClientAssetManager.getAmmoDisplay(displayId) ?: return null
        val modelId = display.modelLocation ?: return null
        val textureId = display.modelTexture ?: return null
        val modelData = TACZClientAssetManager.getModel(modelId) ?: return null
        val textureLocation = TACZClientAssetManager.getTextureLocation(textureId) ?: return null
        return ResolvedPreviewModel(
            modelId = modelId,
            modelData = modelData,
            textureLocation = textureLocation,
            scaleMultiplier = transformScaleMultiplier(display.transform?.scale, 0.95f),
            verticalOffset = 1.6f,
        )
    }

    private fun resolveBlock(displayId: ResourceLocation): ResolvedPreviewModel? {
        val display: BlockDisplay = TACZClientAssetManager.getBlockDisplay(displayId) ?: return null
        val modelId = display.modelLocation ?: return null
        val textureId = display.modelTexture ?: return null
        val modelData = TACZClientAssetManager.getModel(modelId) ?: return null
        val textureLocation = TACZClientAssetManager.getTextureLocation(textureId) ?: return null
        return ResolvedPreviewModel(
            modelId = modelId,
            modelData = modelData,
            textureLocation = textureLocation,
            scaleMultiplier = 1.0f,
            verticalOffset = 1.4f,
        )
    }

    private fun renderModel(
        resolved: ResolvedPreviewModel,
        centerX: Float,
        centerY: Float,
        scale: Float,
        yaw: Float,
        pitch: Float,
    ) {
        val model = TACZClientAssetManager.getStaticBlockModel(resolved.modelId) ?: return
        val mc = Minecraft.getMinecraft()
        mc.textureManager.bindTexture(resolved.textureLocation)

        GlStateManager.pushMatrix()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.translate(centerX, centerY, 300.0f)
        GlStateManager.scale(scale * resolved.scaleMultiplier, scale * resolved.scaleMultiplier, scale * resolved.scaleMultiplier)
        GlStateManager.rotate(180.0f, 0.0f, 0.0f, 1.0f)
        GlStateManager.rotate(yaw, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(pitch, 1.0f, 0.0f, 0.0f)
        // 以模型几何包围盒中心为旋转/显示中心，避免非 1x1 模型（如 2 格宽的枪械工作台、2 格高的配件工作台）偏移
        val center = modelCenter(resolved.modelData)
        if (center != null) {
            GlStateManager.translate(center[0] / 16.0f, 1.5f - center[1] / 16.0f, -center[2] / 16.0f)
        } else {
            GlStateManager.translate(0.0f, resolved.verticalOffset, 0.0f)
        }
        GlStateManager.scale(-1.0f, -1.0f, 1.0f)

        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.enableLighting()
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
        )

        model.render()
        captureBloomIfSupported(resolved.textureLocation, model) {
            model.renderBloom()
        }

        GlStateManager.enableCull()
        GlStateManager.disableBlend()
        GlStateManager.disableRescaleNormal()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.popMatrix()
    }

    /**
     * 计算模型所有方块（cube）的轴对齐包围盒中心，单位为基岩版像素。忽略骨骼旋转，作为显示用近似值已足够。
     */
    private fun modelCenter(modelData: TACZClientAssetManager.ModelData): FloatArray? {
        if (modelCenters.containsKey(modelData)) {
            return modelCenters[modelData]
        }
        val bones = modelData.pojo.geometryModelNew?.bones ?: modelData.pojo.geometryModelLegacy?.bones
        val min = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        val max = floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        var found = false
        bones.orEmpty().forEach { bone ->
            bone.cubes.orEmpty().forEach { cube ->
                val origin = cube.origin
                val size = cube.size
                if (origin != null && size != null && origin.size >= 3 && size.size >= 3) {
                    for (axis in 0 until 3) {
                        min[axis] = minOf(min[axis], origin[axis])
                        max[axis] = maxOf(max[axis], origin[axis] + size[axis])
                    }
                    found = true
                }
            }
        }
        val center = if (found) FloatArray(3) { axis -> (min[axis] + max[axis]) / 2.0f } else null
        modelCenters[modelData] = center
        return center
    }

    private fun captureBloomIfSupported(
        texture: ResourceLocation,
        model: BedrockModel,
        renderBloom: () -> Unit,
    ) {
        when (model) {
            is BedrockAnimatedModel -> {
                val snapshot = model.captureRenderState()
                TACZBloomBridge.captureCurrentModelBloom(texture) {
                    model.restoreRenderState(snapshot)
                    renderBloom()
                    model.cleanAnimationTransform()
                    model.cleanCameraAnimationTransform()
                }
            }
            else -> {
                TACZBloomBridge.captureCurrentModelBloom(texture, renderBloom)
            }
        }
    }

    private fun transformScaleMultiplier(scale: TransformScale?, fallback: Float): Float {
        val vector = scale?.fixed ?: scale?.thirdPerson ?: scale?.ground ?: return fallback
        return averageScale(vector).coerceIn(0.35f, 2.25f)
    }

    private fun averageScale(vector: Vector3f): Float = (vector.x + vector.y + vector.z) / 3.0f

    private val MATRIX_BUFFER = BufferUtils.createFloatBuffer(16)
}
