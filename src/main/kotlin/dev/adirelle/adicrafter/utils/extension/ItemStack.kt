@file:Suppress("UnstableApiUsage")

import dev.adirelle.adicrafter.utils.extension.ItemAmount
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList

fun ItemStack.toVariant(): ItemVariant = ItemVariant.of(this)
fun ItemStack.toAmount() = ItemAmount(this.toVariant(), count.toLong())
fun ItemStack.toNbt() = NbtCompound().also { writeNbt(it) }

fun Iterable<ItemStack>.toNbt() = NbtList().also { nbt ->
    this.forEach { nbt.add(it.toNbt()) }
}

fun MutableList<ItemStack>.readNbt(nbtList: NbtList) {
    nbtList.forEachIndexed { idx, nbt ->
        if (nbt is NbtCompound) {
            val stack = ItemStack.fromNbt(nbt)
            if (idx >= size) {
                add(stack)
            } else {
                this[idx] = stack
            }
        }
    }
}
