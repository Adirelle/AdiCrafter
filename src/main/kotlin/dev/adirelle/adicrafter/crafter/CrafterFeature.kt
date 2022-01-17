@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.utils.ModFeature
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.item.ItemGroup

object CrafterFeature : ModFeature(AdiCrafter, "crafter") {

    val BLOCK = register(CrafterBlock())

    val BLOCK_ENTITY_TYPE = register(::CrafterBlockEntity, BLOCK)

    val SCREEN_HANDLER_TYPE = registerExtended(::CrafterScreenHandler)

    val ITEM = registerItemFor(BLOCK) {
        group(ItemGroup.REDSTONE)
    }

    override fun onInitialize() {
        super.onInitialize()
        ItemStorage.SIDED.registerForBlockEntity({ blockEntity, _ -> blockEntity.storage }, BLOCK_ENTITY_TYPE)
    }

    @Environment(CLIENT)
    override fun onInitializeClient() {
        super.onInitializeClient()
        register(SCREEN_HANDLER_TYPE, ::CrafterScreen)
    }
}
