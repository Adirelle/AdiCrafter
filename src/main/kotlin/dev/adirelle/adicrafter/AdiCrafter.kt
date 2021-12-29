package dev.adirelle.adicrafter

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.block.Block
import net.minecraft.block.Material
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager

const val MOD_ID = "adicrafter"

val LOGGER = LogManager.getLogger(MOD_ID)

val CRAFTER_BLOCK = Block(FabricBlockSettings.of(Material.METAL).strength(4.0f))
val CRAFTER_ITEM = BlockItem(CRAFTER_BLOCK, FabricItemSettings().group(ItemGroup.REDSTONE))

@Suppress("UNUSED")
object AdiCrafter : ModInitializer {

    override fun onInitialize() {
        Registry.register(Registry.BLOCK, Identifier(MOD_ID, "crafter"), CRAFTER_BLOCK)
        Registry.register(Registry.ITEM, Identifier(MOD_ID, "crafter"), CRAFTER_ITEM)
        
        LOGGER.info("${MOD_ID} initialized!")
    }

}
