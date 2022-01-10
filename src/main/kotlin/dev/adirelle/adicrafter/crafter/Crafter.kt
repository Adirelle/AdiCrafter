@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.utils.minecraft.ModFeature
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.item.ItemGroup

object Crafter : ModFeature(AdiCrafter, "crafter") {

    val BLOCK = register(CrafterBlock())

    val BLOCK_ENTITY_TYPE = register(::CrafterBlockEntity, BLOCK)

    val SCREEN_HANDLER_TYPE = register(::CrafterScreenHandler)

    val ITEM = registerItemFor(BLOCK) {
        group(ItemGroup.REDSTONE)
    }

    @Environment(CLIENT)
    val SCREEN = register(SCREEN_HANDLER_TYPE, ::CrafterScreen)

    init {
        ItemStorage.SIDED.registerForBlockEntity({ blockEntity, _ -> blockEntity.storage }, BLOCK_ENTITY_TYPE)
    }
}
