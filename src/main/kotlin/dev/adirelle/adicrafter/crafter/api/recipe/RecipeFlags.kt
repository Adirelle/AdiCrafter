package dev.adirelle.adicrafter.crafter.api.recipe

import net.minecraft.nbt.NbtInt
import net.minecraft.network.PacketByteBuf

@JvmInline
value class RecipeFlags private constructor(private val value: Int) {

    operator fun contains(b: RecipeFlags) =
        (value and b.value) != 0

    operator fun plus(flag: RecipeFlags) =
        RecipeFlags(value or flag.value)

    operator fun minus(flag: RecipeFlags) =
        RecipeFlags(value and flag.value.inv())

    fun set(flag: RecipeFlags, enable: Boolean): RecipeFlags =
        if (enable) this + flag
        else this - flag

    fun toInt() =
        value

    fun toNbt(): NbtInt =
        NbtInt.of(value)

    fun writeToPacket(buf: PacketByteBuf) {
        buf.writeInt(value)
    }

    override fun toString() =
        "[fuzzy=%s fluids=%s]".format(
            if (FUZZY in this) "yes" else "no",
            if (FLUIDS in this) "yes" else "no"
        )

    companion object {

        val NONE = RecipeFlags(0x00)
        val ALL = RecipeFlags(0x03)

        val FUZZY = RecipeFlags(0x01)
        val FLUIDS = RecipeFlags(0x02)

        fun of(value: Int) =
            RecipeFlags(value and ALL.value)

        fun fromNbt(nbt: Int) =
            of(nbt)

        fun fromPacket(buf: PacketByteBuf) =
            RecipeFlags(buf.readInt() and ALL.value)
    }
}
