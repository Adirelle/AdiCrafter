package dev.adirelle.adicrafter.utils.general

import com.google.common.base.Strings

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
