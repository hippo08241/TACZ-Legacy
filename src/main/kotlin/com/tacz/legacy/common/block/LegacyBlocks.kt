@file:Suppress("DEPRECATION")

package com.tacz.legacy.common.block

import com.tacz.legacy.TACZLegacy
import com.tacz.legacy.common.block.entity.GunSmithTableTileEntity
import com.tacz.legacy.common.block.entity.StatueTileEntity
import com.tacz.legacy.common.block.entity.TargetTileEntity
import com.tacz.legacy.common.gui.LegacyGuiIds
import com.tacz.legacy.common.registry.LegacyCreativeTabs
import com.tacz.legacy.common.registry.LegacySoundEvents
import com.tacz.legacy.common.item.LegacyBlockItem
import net.minecraft.block.BlockContainer
import net.minecraft.block.BlockHorizontal
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.InventoryHelper
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumBlockRenderType
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.Mirror
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.registries.IForgeRegistry
import java.util.Random

internal object LegacyBlocks {
    internal val GUN_SMITH_TABLE: LegacyGunSmithTableBlock = LegacyGunSmithTableBlock("gun_smith_table")
    internal val WORKBENCH_A: LegacyGunSmithTableBlock = LegacyGunSmithTableBlock("workbench_a")
    internal val WORKBENCH_B: LegacyGunSmithTableBlock = LegacyGunSmithTableBlock("workbench_b")
    internal val WORKBENCH_C: LegacyGunSmithTableBlock = LegacyGunSmithTableBlock("workbench_c")
    internal val TARGET: LegacyTargetBlock = LegacyTargetBlock("target")
    internal val STATUE: LegacyStatueBlock = LegacyStatueBlock("statue")

    internal val allBlocks: List<BlockContainer> = listOf(
        GUN_SMITH_TABLE,
        WORKBENCH_A,
        WORKBENCH_B,
        WORKBENCH_C,
        TARGET,
        STATUE,
    )

    internal fun registerAll(registry: IForgeRegistry<net.minecraft.block.Block>): Unit {
        allBlocks.forEach(registry::register)
    }
}

internal abstract class LegacyBaseBlock(path: String, material: Material, soundType: SoundType) : BlockContainer(material) {
    init {
        registryName = ResourceLocation(TACZLegacy.MOD_ID, path)
        setTranslationKey("${TACZLegacy.MOD_ID}.$path")
        setCreativeTab(LegacyCreativeTabs.DECORATION)
        setSoundType(soundType)
        setHardness(2.0f)
        setResistance(3.0f)
        setTickRandomly(false)
    }

    override fun getRenderType(state: IBlockState): EnumBlockRenderType = EnumBlockRenderType.MODEL

    override fun isOpaqueCube(state: IBlockState): Boolean = false

    override fun isFullCube(state: IBlockState): Boolean = false
}

internal class LegacyGunSmithTableBlock(path: String) : LegacyBaseBlock(path, Material.WOOD, SoundType.WOOD) {
    init {
        defaultState = blockState.baseState.withProperty(FACING, EnumFacing.NORTH)
    }

    override fun getRenderType(state: IBlockState): EnumBlockRenderType = EnumBlockRenderType.ENTITYBLOCK_ANIMATED

    override fun createBlockState(): BlockStateContainer = BlockStateContainer(this, FACING)

    override fun getStateFromMeta(meta: Int): IBlockState =
        defaultState.withProperty(FACING, EnumFacing.byHorizontalIndex(meta and 3))

    override fun getMetaFromState(state: IBlockState): Int = state.getValue(FACING).horizontalIndex

    override fun withRotation(state: IBlockState, rot: Rotation): IBlockState =
        state.withProperty(FACING, rot.rotate(state.getValue(FACING)))

    override fun withMirror(state: IBlockState, mirrorIn: Mirror): IBlockState =
        state.withRotation(mirrorIn.toRotation(state.getValue(FACING)))

    override fun getStateForPlacement(
        world: World,
        pos: BlockPos,
        facing: EnumFacing,
        hitX: Float,
        hitY: Float,
        hitZ: Float,
        meta: Int,
        placer: EntityLivingBase,
        hand: EnumHand,
    ): IBlockState = defaultState.withProperty(FACING, placer.horizontalFacing)

    override fun createNewTileEntity(worldIn: World, meta: Int): TileEntity = GunSmithTableTileEntity().apply {
        blockId = GunSmithTableTileEntity.resolveWorkbenchBlockId(requireNotNull(this@LegacyGunSmithTableBlock.registryName))
    }

    override fun onBlockActivated(
        worldIn: World,
        pos: BlockPos,
        state: IBlockState,
        playerIn: EntityPlayer,
        hand: EnumHand,
        facing: EnumFacing,
        hitX: Float,
        hitY: Float,
        hitZ: Float,
    ): Boolean {
        if (worldIn.isRemote) {
            return true
        }
        if (worldIn.getTileEntity(pos) !is GunSmithTableTileEntity) {
            return true
        }
        playerIn.openGui(TACZLegacy, LegacyGuiIds.GUN_SMITH_TABLE, worldIn, pos.x, pos.y, pos.z)
        worldIn.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f)
        return true
    }

    /**
     * 掉落物需要携带 TileEntity 中的 BlockId，否则枪包工作台（如配件工作台）被破坏后
     * 会变回没有 BlockId 的默认物品。
     */
    override fun getDrops(drops: NonNullList<ItemStack>, world: IBlockAccess, pos: BlockPos, state: IBlockState, fortune: Int) {
        drops += createStackWithBlockId(world.getTileEntity(pos) as? GunSmithTableTileEntity)
    }

    override fun getPickBlock(
        state: IBlockState,
        target: RayTraceResult,
        world: World,
        pos: BlockPos,
        player: EntityPlayer,
    ): ItemStack = createStackWithBlockId(world.getTileEntity(pos) as? GunSmithTableTileEntity)

    // 推迟方块移除，使 getDrops 在 harvestBlock 中仍能读取到 TileEntity（与原版花盆相同的做法）。
    override fun removedByPlayer(
        state: IBlockState,
        world: World,
        pos: BlockPos,
        player: EntityPlayer,
        willHarvest: Boolean,
    ): Boolean {
        if (willHarvest) {
            return true
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest)
    }

    override fun harvestBlock(
        worldIn: World,
        player: EntityPlayer,
        pos: BlockPos,
        state: IBlockState,
        te: TileEntity?,
        stack: ItemStack,
    ) {
        super.harvestBlock(worldIn, player, pos, state, te, stack)
        worldIn.setBlockToAir(pos)
    }

    private fun createStackWithBlockId(tile: GunSmithTableTileEntity?): ItemStack {
        val item = Item.getItemFromBlock(this)
        val stack = ItemStack(item)
        if (tile != null && item is LegacyBlockItem) {
            item.setBlockId(stack, tile.blockId)
        }
        return stack
    }

    internal companion object {
        internal val FACING: PropertyDirection = BlockHorizontal.FACING
    }
}

internal class LegacyTargetBlock(path: String) : LegacyBaseBlock(path, Material.WOOD, SoundType.WOOD) {
    init {
        setTickRandomly(true)
    }

    override fun createNewTileEntity(worldIn: World, meta: Int): TileEntity = TargetTileEntity()

    override fun onBlockActivated(
        worldIn: World,
        pos: BlockPos,
        state: IBlockState,
        playerIn: EntityPlayer,
        hand: EnumHand,
        facing: EnumFacing,
        hitX: Float,
        hitY: Float,
        hitZ: Float,
    ): Boolean {
        if (worldIn.isRemote) {
            return true
        }
        trigger(worldIn, pos, state)
        return true
    }

    /** 被子弹击中时触发（与上游 TargetBlock.onProjectileHit 一致）。 */
    internal fun onBulletHit(worldIn: World, pos: BlockPos, state: IBlockState): Unit {
        if (worldIn.isRemote) {
            return
        }
        trigger(worldIn, pos, state)
    }

    private fun trigger(worldIn: World, pos: BlockPos, state: IBlockState): Unit {
        val tile = worldIn.getTileEntity(pos) as? TargetTileEntity ?: return
        tile.trigger()
        worldIn.notifyBlockUpdate(pos, state, state, 3)
        worldIn.notifyNeighborsOfStateChange(pos, this, false)
        worldIn.playSound(null, pos, LegacySoundEvents.TARGET_BLOCK_HIT, SoundCategory.BLOCKS, 1.0f, 1.0f)
        worldIn.scheduleUpdate(pos, this, 40)
    }

    override fun canProvidePower(state: IBlockState): Boolean = true

    override fun getWeakPower(state: IBlockState, blockAccess: net.minecraft.world.IBlockAccess, pos: BlockPos, side: EnumFacing): Int {
        val tile = blockAccess.getTileEntity(pos) as? TargetTileEntity ?: return 0
        return if (tile.triggered) 15 else 0
    }

    override fun getStrongPower(state: IBlockState, blockAccess: net.minecraft.world.IBlockAccess, pos: BlockPos, side: EnumFacing): Int =
        getWeakPower(state, blockAccess, pos, side)

    override fun updateTick(worldIn: World, pos: BlockPos, state: IBlockState, rand: Random): Unit {
        val tile = worldIn.getTileEntity(pos) as? TargetTileEntity ?: return
        if (tile.triggered) {
            tile.reset()
            worldIn.notifyBlockUpdate(pos, state, state, 3)
            worldIn.notifyNeighborsOfStateChange(pos, this, false)
        }
    }
}

internal class LegacyStatueBlock(path: String) : LegacyBaseBlock(path, Material.ROCK, SoundType.STONE) {
    init {
        setHardness(3.0f)
        setResistance(6.0f)
    }

    override fun createNewTileEntity(worldIn: World, meta: Int): TileEntity = StatueTileEntity()

    override fun onBlockActivated(
        worldIn: World,
        pos: BlockPos,
        state: IBlockState,
        playerIn: EntityPlayer,
        hand: EnumHand,
        facing: EnumFacing,
        hitX: Float,
        hitY: Float,
        hitZ: Float,
    ): Boolean {
        if (worldIn.isRemote) {
            return true
        }
        val tile = worldIn.getTileEntity(pos) as? StatueTileEntity ?: return true
        val held = playerIn.getHeldItem(hand)
        if (!held.isEmpty && tile.storedItem.isEmpty) {
            val copy = held.copy()
            copy.count = 1
            tile.store(copy)
            held.shrink(1)
            worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0f, 1.0f)
            return true
        }
        if (held.isEmpty && !tile.storedItem.isEmpty) {
            InventoryHelper.spawnItemStack(worldIn, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, tile.storedItem.copy())
            tile.clear()
            worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1.0f, 1.0f)
            return true
        }
        return true
    }

    override fun breakBlock(worldIn: World, pos: BlockPos, state: IBlockState): Unit {
        val tile = worldIn.getTileEntity(pos) as? StatueTileEntity
        if (tile != null && !tile.storedItem.isEmpty) {
            InventoryHelper.spawnItemStack(worldIn, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, tile.storedItem.copy())
            tile.clear()
        }
        super.breakBlock(worldIn, pos, state)
    }
}
