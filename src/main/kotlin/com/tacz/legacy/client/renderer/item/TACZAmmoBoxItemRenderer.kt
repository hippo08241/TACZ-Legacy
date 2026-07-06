package com.tacz.legacy.client.renderer.item



import com.tacz.legacy.api.item.IAmmoBox

import com.tacz.legacy.client.model.TACZPerspectiveAwareBakedModel

import com.tacz.legacy.client.model.blockbench.BlockbenchItemModel

import com.tacz.legacy.common.item.AmmoBoxItem

import net.minecraft.client.Minecraft

import net.minecraft.client.renderer.GlStateManager

import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType

import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer

import net.minecraft.item.ItemStack

import net.minecraft.util.ResourceLocation

import net.minecraftforge.fml.relauncher.Side

import net.minecraftforge.fml.relauncher.SideOnly



@SideOnly(Side.CLIENT)

internal object TACZAmmoBoxItemRenderer : TileEntityItemStackRenderer() {

    private val MODEL_BY_STATUE: Array<ResourceLocation> = arrayOf(

        ResourceLocation("tacz", "item/ammo_box/iron_ammo_box_open"),

        ResourceLocation("tacz", "item/ammo_box/iron_ammo_box_close"),

        ResourceLocation("tacz", "item/ammo_box/gold_ammo_box_open"),

        ResourceLocation("tacz", "item/ammo_box/gold_ammo_box_close"),

        ResourceLocation("tacz", "item/ammo_box/diamond_ammo_box_open"),

        ResourceLocation("tacz", "item/ammo_box/diamond_ammo_box_close"),

        ResourceLocation("tacz", "item/ammo_box/creative_ammo_box_open"),

        ResourceLocation("tacz", "item/ammo_box/creative_ammo_box_close"),

        ResourceLocation("tacz", "item/ammo_box/all_type_creative_ammo_box"),

    )

    private val FALLBACK_TEXTURE = ResourceLocation("tacz", "textures/item/ammo_box.png")



    override fun renderByItem(stack: ItemStack) {

        renderByItem(stack, 1.0f)

    }



    override fun renderByItem(stack: ItemStack, partialTicks: Float) {

        val item = stack.item as? IAmmoBox ?: return

        val statue = AmmoBoxItem.getStatue(stack, item).coerceIn(0, MODEL_BY_STATUE.lastIndex)

        val modelLoc = MODEL_BY_STATUE[statue]

        val model = BlockbenchItemModel.getOrLoad(modelLoc)

        val transformType = TACZPerspectiveAwareBakedModel.getCurrentTransformType()



        GlStateManager.pushMatrix()

        applyDisplayTransform(transformType)



        GlStateManager.translate(0.5f, 1.5f, 0.5f)

        GlStateManager.rotate(180f, 0f, 0f, 1f)

        GlStateManager.disableCull()

        GlStateManager.enableDepth()

        GlStateManager.enableLighting()

        GlStateManager.enableRescaleNormal()

        GlStateManager.enableBlend()

        GlStateManager.tryBlendFuncSeparate(

            GlStateManager.SourceFactor.SRC_ALPHA,

            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,

            GlStateManager.SourceFactor.ONE,

            GlStateManager.DestFactor.ZERO,

        )



        if (model != null) {

            model.render(AmmoBoxItem.getTintColor(stack))

        } else {

            Minecraft.getMinecraft().textureManager.bindTexture(FALLBACK_TEXTURE)

        }



        GlStateManager.disableBlend()

        GlStateManager.disableRescaleNormal()

        GlStateManager.enableCull()

        GlStateManager.popMatrix()

    }



    private fun applyDisplayTransform(transformType: TransformType) {

        when (transformType) {

            TransformType.GUI -> {

                GlStateManager.translate(0.5f, 0.5f, 0.5f)

                GlStateManager.scale(1.35f, 1.35f, 1.35f)

                GlStateManager.rotate(30f, 1f, 0f, 0f)

                GlStateManager.rotate(225f, 0f, 1f, 0f)

            }

            TransformType.GROUND -> {

                GlStateManager.translate(0.5f, 0.25f, 0.5f)

                GlStateManager.scale(0.5f, 0.5f, 0.5f)

            }

            TransformType.FIXED -> {

                GlStateManager.translate(0.5f, 0.5f, 0.5f)

                GlStateManager.scale(0.5f, 0.5f, 0.5f)

            }

            TransformType.THIRD_PERSON_RIGHT_HAND, TransformType.THIRD_PERSON_LEFT_HAND -> {

                GlStateManager.translate(0.5f, 0.85f, 0.5f)

                GlStateManager.scale(0.85f, 0.85f, 0.85f)

            }

            TransformType.FIRST_PERSON_RIGHT_HAND, TransformType.FIRST_PERSON_LEFT_HAND -> {

                GlStateManager.translate(0.5f, 0.85f, 0.5f)

                GlStateManager.scale(0.9f, 0.9f, 0.9f)

            }

            else -> {

                GlStateManager.translate(0.5f, 0.5f, 0.5f)

            }

        }

    }

}


