package dev.adirelle.adicrafter.crafter.impl.power

import net.minecraft.item.Item
import net.minecraft.tag.ServerTagManagerHolder
import net.minecraft.tag.Tag
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import java.util.*

class RedstoneGenerator(
    powerPerDust: Long
) : ItemConsumerGenerator(
    { item -> getRedstoneValue(item).map { it * powerPerDust } }
) {

    companion object {

        private fun getItemTag(name: String): Tag<Item> =
            ServerTagManagerHolder.getTagManager().getTag(Registry.ITEM_KEY, Identifier(name)) {
                IllegalArgumentException("unknown tag $it")
            }

        private val redstoneItems: Map<Item, Long> by lazy {
            buildMap {
                for (item in getItemTag("c:redstone_dusts").values()) {
                    put(item, 1L)
                }
                for (item in getItemTag("c:redstone_blocks").values()) {
                    put(item, 9L)
                }
            }
        }

        private fun getRedstoneValue(item: Item): Optional<Long> =
            Optional.ofNullable(redstoneItems[item])
    }
}
