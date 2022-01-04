package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.utils.mod.ModFeature
import net.minecraft.item.ItemGroup

object Crafter : ModFeature(AdiCrafter, "crafter") {

    val BLOCK = register(CrafterBlock())

    val BLOCK_ENTITY_TYPE = register(::CrafterBlockEntity, BLOCK)

    val SCREEN_HANDLER_TYPE = register(::CrafterScreenHandler)

    init {
        registerItemFor(BLOCK) {
            group(ItemGroup.REDSTONE)
        }

        clientOnly {
            register(SCREEN_HANDLER_TYPE, ::CrafterScreen)
        }
    }
}
