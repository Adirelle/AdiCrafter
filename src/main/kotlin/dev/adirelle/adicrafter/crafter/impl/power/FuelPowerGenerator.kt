@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

import dev.adirelle.adicrafter.utils.extensions.toNbt
import dev.adirelle.adicrafter.utils.inventory.ArrayInventoryAdapter
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess
import kotlin.math.min

class FuelPowerGenerator(capacity: Long, reloadRate: Long) :
    AbstractPowerGenerator(capacity, reloadRate), InventoryProvider {

    companion object {

        private const val STACK_NBT_KEY = "Stack"
        private const val REMAINING_TIME_NBT_KEY = "Time"

        private val fuelTable = AbstractFurnaceBlockEntity.createFuelTimeMap()
    }

    private var stacks = arrayOf(ItemStack.EMPTY)

    private var stack: ItemStack
        get() = stacks[0]
        set(value) {
            stacks[0] = value
        }

    private var remainingTime: Long = 0

    private val inventoryAdapter: SidedInventory by lazy {
        object : ArrayInventoryAdapter(stacks) {
            override fun isValid(slot: Int, stack: ItemStack) =
                super.isValid(slot, stack) && stack.item in fuelTable.keys

            override fun markDirty() {
                super.markDirty()
                onUpdate()
            }
        }
    }

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?) =
        inventoryAdapter

    override fun readFromNbt(nbt: NbtCompound) {
        super.readFromNbt(nbt)
        stack = ItemStack.fromNbt(nbt.getCompound(STACK_NBT_KEY))
        remainingTime = nbt.getLong(REMAINING_TIME_NBT_KEY)
    }

    override fun writeToNbt(nbt: NbtCompound) {
        super.writeToNbt(nbt)
        nbt.put(STACK_NBT_KEY, stack.toNbt())
        nbt.putLong(REMAINING_TIME_NBT_KEY, remainingTime)
    }

    override fun reload(delay: Long, maxAmount: Long): Long {
        val consuming = min(delay, min(remainingTime, maxAmount / reloadRate))
        if (remainingTime > consuming) {
            remainingTime -= consuming
        } else {
            remainingTime = consumeFuel()
        }
        return super.reload(consuming, maxAmount)
    }

    private fun consumeFuel(): Long {
        if (stack.isEmpty) return 0
        val fuelTime = fuelTable[stack.item] ?: return 0
        stack.decrement(1)
        return fuelTime.toLong()
    }
}

