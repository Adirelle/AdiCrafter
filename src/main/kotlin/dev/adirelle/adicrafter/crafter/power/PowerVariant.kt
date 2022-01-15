@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.power

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf

enum class PowerVariant : TransferVariant<Power> {
    INSTANCE;

    override fun isBlank() = false
    override fun getObject() = Power.INSTANCE
    override fun getNbt() = null
    override fun toNbt() = NbtCompound()
    override fun toPacket(buf: PacketByteBuf?) {}
}
