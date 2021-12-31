package dev.adirelle.adicrafter

import dev.adirelle.adicrafter.block.CrafterBlock
import dev.adirelle.adicrafter.blockentity.CrafterBlockEntity
import dev.adirelle.adicrafter.screen.CrafterScreenHandler
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.block.Material
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemGroup
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager

@Suppress("UNUSED")
object AdiCrafter : ModInitializer {
    private val LOGGER = LogManager.getLogger()!!

    const val MOD_ID = "adicrafter"

    val CRAFTER_ID = Identifier(MOD_ID, "crafter")
    val CRAFTER_BLOCK = CrafterBlock(FabricBlockSettings.of(Material.METAL).strength(4.0f))
    val CRAFTER_ITEM = BlockItem(CRAFTER_BLOCK, FabricItemSettings().group(ItemGroup.REDSTONE))
    val CRAFTER_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(::CrafterBlockEntity, CRAFTER_BLOCK).build()!!
    lateinit var CRAFTER_SCREEN_HANDLER: ScreenHandlerType<CrafterScreenHandler>

    override fun onInitialize() {
        Registry.register(Registry.BLOCK, CRAFTER_ID, CRAFTER_BLOCK)
        Registry.register(Registry.ITEM, CRAFTER_ID, CRAFTER_ITEM)
        Registry.register(Registry.BLOCK_ENTITY_TYPE, CRAFTER_ID, CRAFTER_BLOCK_ENTITY)
        CRAFTER_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(CRAFTER_ID, ::CrafterScreenHandler)!!

//        @Suppress("UnstableApiUsage")
//        ItemStorage.SIDED.registerForBlockEntity(
//            { entity, direction -> entity?.let { InventoryStorage.of(it.inventory, direction) } },
//            CRAFTER_BLOCK_ENTITY
//        )

        LOGGER.info("$MOD_ID initialized!")
    }
}
