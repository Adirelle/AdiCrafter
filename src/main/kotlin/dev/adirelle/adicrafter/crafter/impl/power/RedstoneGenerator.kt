package dev.adirelle.adicrafter.crafter.impl.power

import dev.adirelle.adicrafter.crafter.api.power.PowerSource.Listener
import dev.adirelle.adicrafter.utils.memoize
import net.minecraft.item.Item
import net.minecraft.tag.ServerTagManagerHolder
import net.minecraft.tag.Tag
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

class RedstoneGenerator(
    powerPerDust: Long,
    listener: Listener
) : ItemConsumerGenerator(memoizedFuelMap(powerPerDust), listener) {

    override fun hasPowerBar() = true

    companion object {

        private fun getItemTag(name: String): Tag<Item> =
            ServerTagManagerHolder.getTagManager().getTag(Registry.ITEM_KEY, Identifier(name)) {
                IllegalArgumentException("unknown tag $it")
            }

        private fun createFuelMap(powerPerDust: Long) = buildMap {
            for (item in getItemTag("c:redstone_dusts").values()) {
                put(item, powerPerDust)
            }
            for (item in getItemTag("c:redstone_blocks").values()) {
                put(item, 9 * powerPerDust)
            }
        }

        private val memoizedFuelMap = memoize(::createFuelMap)
    }
}
