package dev.adirelle.adicrafter.utils

class BitArray private constructor(
    val size: Int,
    private var bits: Int
) : Iterable<Boolean> {

    fun clear() {
        bits = 0
    }

    fun isEmpty() = bits == 0

    operator fun get(index: Int): Boolean {
        require(index in 0 until size)
        return (bits.shr(index) and 1) != 0
    }

    operator fun set(index: Int, value: Boolean) {
        require(index in 0 until size)
        val bit = 1.shl(index)
        bits =
            if (value) bits or bit
            else bits and bit.inv()
    }

    fun toInt() = bits

    override fun iterator(): Iterator<Boolean> =
        object : Iterator<Boolean> {
            private var index: Int = 0
            override fun hasNext() = index < size
            override fun next() = this@BitArray[index++]
        }

    companion object {

        fun of(size: Int, vararg values: Int): BitArray {
            require(size <= Int.SIZE_BITS)
            return BitArray(size, 0).apply {
                values.forEach { set(it, true) }
            }
        }

        fun fromInt(size: Int, bits: Int): BitArray =
            BitArray(size, bits)
    }
}
