@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extensions

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf

fun <T : TransferVariant<*>> ResourceAmount<T>.isEmpty() = resource.isBlank || amount == 0L

fun ResourceAmount<ItemVariant>.withAmount(amount: Long) =
    if (amount <= 0) EMPTY_ITEM_AMOUNT
    else ResourceAmount(resource, amount)

operator fun ResourceAmount<ItemVariant>.plus(delta: Long) = withAmount(amount + delta)

operator fun ResourceAmount<ItemVariant>.minus(delta: Long) = withAmount(amount + delta)

operator fun ResourceAmount<ItemVariant>.compareTo(amount: Long) =
    (this.amount - amount).toInt()

val EMPTY_ITEM_AMOUNT = ResourceAmount(ItemVariant.blank(), 0L)

fun ResourceAmount<ItemVariant>.toStack() =
    if (isEmpty()) ItemStack.EMPTY else resource.toStack(amount.toInt())

fun resourceAmountFromNbt(nbt: NbtCompound): ResourceAmount<ItemVariant> {
    return ResourceAmount(
        ItemVariant.fromNbt(nbt.getCompound("Resource")),
        nbt.getLong("Amount")
    )
}

fun ResourceAmount<ItemVariant>.writeToNbt(nbt: NbtCompound) {
    nbt.put("Resource", resource.toNbt())
    nbt.putLong("Amount", amount)
}

fun ResourceAmount<ItemVariant>.toNbt() =
    NbtCompound().also { writeToNbt(it) }

fun ResourceAmount<ItemVariant>.toPacket(buf: PacketByteBuf) {
    resource.toPacket(buf)
    buf.writeLong(amount)
}

fun resourceAmountFromPacket(buf: PacketByteBuf): ResourceAmount<ItemVariant> {
    val resource = ItemVariant.fromPacket(buf)
    val amount = buf.readLong()
    return ResourceAmount<ItemVariant>(resource, amount)
}
