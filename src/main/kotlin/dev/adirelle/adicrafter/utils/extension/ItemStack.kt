@file:Suppress("UnstableApiUsage")

import dev.adirelle.adicrafter.utils.extension.ItemAmount
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

fun ItemStack.toVariant(): ItemVariant = ItemVariant.of(this)
fun ItemStack.toAmount() = ItemAmount(this.toVariant(), count.toLong())
fun ItemStack.toNbt() = NbtCompound().also { writeNbt(it) }
