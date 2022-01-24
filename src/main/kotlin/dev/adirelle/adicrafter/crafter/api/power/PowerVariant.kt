@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api.power

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf

object PowerVariant : TransferVariant<Power> {

    override fun isBlank() = false
    override fun getObject() = Power
    override fun getNbt(): NbtCompound? = null
    override fun toNbt() = NbtCompound()
    override fun toPacket(buf: PacketByteBuf) {}
    override fun toString() = Power.toString()
}
