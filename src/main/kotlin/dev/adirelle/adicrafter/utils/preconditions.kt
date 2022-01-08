package dev.adirelle.adicrafter.utils

import com.google.common.base.Strings

fun <T : List<*>> expectSameSizeAs(list: T, sample: T, desc: String = "list"): T =
    expectExactSize(list, sample.size, desc)

fun <T : List<*>> expectExactSize(list: T, size: Int, desc: String = "list"): T {
    if (size != list.size) {
        throw IndexOutOfBoundsException(
            Strings.lenientFormat(
                "expected a %s of exactly %s item(s), not %s",
                desc,
                size,
                list.size
            )
        )
    }
    return list
}
