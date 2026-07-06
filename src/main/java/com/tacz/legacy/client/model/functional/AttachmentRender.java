package com.tacz.legacy.client.model.functional;

import com.tacz.legacy.api.entity.IGunOperator;
import com.tacz.legacy.api.item.IGun;
import com.tacz.legacy.api.item.IAttachment;
import com.tacz.legacy.api.item.attachment.AttachmentType;
import com.tacz.legacy.client.event.FirstPersonRenderGunEvent;
import com.tacz.legacy.client.model.BedrockAttachmentModel;
import com.tacz.legacy.client.model.BedrockGunModel;
import com.tacz.legacy.client.model.IFunctionalRenderer;
import com.tacz.legacy.client.resource.TACZClientAssetManager;
import com.tacz.legacy.client.resource.index.ClientAttachmentIndex;
import com.tacz.legacy.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.legacy.client.util.RenderHelper;
import com.tacz.legacy.common.resource.TACZGunPackPresentation;
import com.tacz.legacy.common.resource.TACZGunPackRuntimeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

/**
 * Functional renderer for attachments mounted on a gun model node.
 */
public class AttachmentRender implements IFunctionalRenderer {
    private final BedrockGunModel bedrockGunModel;
    private final AttachmentType type;

    public AttachmentRender(BedrockGunModel bedrockGunModel, AttachmentType type) {
        this.bedrockGunModel = bedrockGunModel;
        this.type = type;
    }

    @Nullable
    private static ResolvedAttachmentAssets resolveAssets(ItemStack attachmentItem) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        if (iAttachment == null) {
            return null;
        }
        ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentItem);
        ClientAttachmentIndex attachmentIndex = TACZClientAssetManager.INSTANCE.getAttachmentIndex(attachmentId);

        ResourceLocation displayId = TACZGunPackPresentation.INSTANCE.resolveAttachmentDisplayId(
                TACZGunPackRuntimeRegistry.currentSnapshotForJava(),
                attachmentId
        );
        if (displayId == null) {
            return null;
        }
        AttachmentDisplay display = TACZClientAssetManager.INSTANCE.getAttachmentDisplay(displayId);
        if (display == null) {
            return null;
        }

        ResourceLocation textureLoc = attachmentIndex != null && attachmentIndex.getModelTexture() != null
                ? attachmentIndex.getModelTexture()
                : display.getTexture();
        if (textureLoc == null) {
            return null;
        }
        ResourceLocation registeredTexture = TACZClientAssetManager.INSTANCE.getTextureLocation(textureLoc);
        if (registeredTexture == null) {
            return null;
        }

        BedrockAttachmentModel model = null;
        if (attachmentIndex != null) {
            model = attachmentIndex.getAttachmentModel();
            if (model == null && !BeamRenderer.getRenderContext().isFirstPerson()) {
                ClientAttachmentIndex.LodModel lodModel = attachmentIndex.getLodModel();
                if (lodModel != null) {
                    model = lodModel.getModel();
                    ResourceLocation lodTexture = TACZClientAssetManager.INSTANCE.getTextureLocation(lodModel.getTexture());
                    if (lodTexture != null) {
                        registeredTexture = lodTexture;
                    }
                }
            }
        }
        if (model == null) {
            ResourceLocation modelLoc = display.getModel();
            if (modelLoc == null) {
                return null;
            }
            TACZClientAssetManager.ModelData modelData = TACZClientAssetManager.INSTANCE.getModel(modelLoc);
            if (modelData == null) {
                return null;
            }
            model = new BedrockAttachmentModel(modelData.getPojo(), modelData.getVersion());
            model.setIsScope(attachmentIndex != null ? attachmentIndex.isScope() : display.isScope());
            model.setIsSight(attachmentIndex != null ? attachmentIndex.isSight() : display.isSight());
        }
        return new ResolvedAttachmentAssets(model, registeredTexture);
    }

    public static void renderAttachment(
            ItemStack attachmentItem,
            @Nullable ItemStack gunItem,
            @Nullable ResourceLocation gunTexture,
            int light,
            boolean bloomOnly
    ) {
        renderAttachment(attachmentItem, gunItem, gunTexture, light, bloomOnly, false);
    }

    public static void renderAttachment(
            ItemStack attachmentItem,
            @Nullable ItemStack gunItem,
            @Nullable ResourceLocation gunTexture,
            int light,
            boolean bloomOnly,
            boolean forceOpticPipeline
    ) {
        if (attachmentItem == null || attachmentItem.isEmpty()) {
            return;
        }
        ResolvedAttachmentAssets assets = resolveAssets(attachmentItem);
        if (assets == null) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(0.0f, -1.5f, 0.0f);
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            mc.getTextureManager().bindTexture(assets.registeredTexture);
            if (bloomOnly) {
                assets.model.renderBloom(attachmentItem, gunItem);
            } else if (forceOpticPipeline || shouldUseOpticRenderPipeline(assets.model)) {
                assets.model.render(attachmentItem, gunItem);
            } else {
                assets.model.renderMountedOnGun(attachmentItem, gunItem);
            }
            assets.model.cleanAnimationTransform();
            assets.model.cleanCameraAnimationTransform();
        } finally {
            restoreAttachmentGlState();
            GlStateManager.popMatrix();
            if (gunTexture != null) {
                mc.getTextureManager().bindTexture(gunTexture);
            }
        }
    }

    private static void restoreAttachmentGlState() {
        RenderHelper.disableItemEntityStencilTest();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
    }

    @Override
    public void render(int light) {
        ItemStack attachmentItem = resolveRenderableAttachmentItem();
        if (attachmentItem.isEmpty()) {
            return;
        }
        renderAttachment(
                attachmentItem,
                bedrockGunModel.getCurrentGunItem(),
                bedrockGunModel.getActiveGunTexture(),
                light,
                false
        );
    }

    @Override
    public void renderBloom(int light) {
        ItemStack attachmentItem = resolveRenderableAttachmentItem();
        if (attachmentItem.isEmpty()) {
            return;
        }
        renderAttachment(
                attachmentItem,
                bedrockGunModel.getCurrentGunItem(),
                bedrockGunModel.getActiveGunTexture(),
                light,
                true
        );
    }

    public static boolean shouldUseOpticRenderPipeline(BedrockAttachmentModel model) {
        if (!model.isScope() && !model.isSight()) {
            return false;
        }
        if (!BeamRenderer.getRenderContext().isFirstPerson()) {
            return false;
        }
        return resolveCurrentAimingProgress() > 0.01f;
    }

    public static float resolveCurrentAimingProgress() {
        Float preparedAimingProgress = FirstPersonRenderGunEvent.getPreparedAimingProgress();
        if (preparedAimingProgress != null) {
            return MathHelper.clamp(preparedAimingProgress, 0.0f, 1.0f);
        }
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return 0.0f;
        }
        return MathHelper.clamp(IGunOperator.fromLivingEntity(player).getSynAimingProgress(), 0.0f, 1.0f);
    }

    private ItemStack resolveRenderableAttachmentItem() {
        ItemStack gunItem = bedrockGunModel.getCurrentGunItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun != null) {
            ItemStack installed = iGun.getAttachment(gunItem, type);
            if (!installed.isEmpty()) {
                return installed;
            }
        }
        ItemStack effective = bedrockGunModel.getAttachmentItem(type);
        return effective == null ? ItemStack.EMPTY : effective;
    }

    private static final class ResolvedAttachmentAssets {
        private final BedrockAttachmentModel model;
        private final ResourceLocation registeredTexture;

        private ResolvedAttachmentAssets(BedrockAttachmentModel model, ResourceLocation registeredTexture) {
            this.model = model;
            this.registeredTexture = registeredTexture;
        }
    }
}
