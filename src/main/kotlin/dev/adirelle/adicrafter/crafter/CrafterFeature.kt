@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.utils.ModFeature
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

@Suppress("MemberVisibilityCanBePrivate")
object CrafterFeature : ModFeature(AdiCrafter) {

    private val config by lazy { AdiCrafter.config.crafter }

    private val factory by lazy { CrafterFactory(config) }

    val BASIC_CRAFTER by lazy { factory.basic.block }
    val BASIC_CRAFTER_ENTITY_TYPE by lazy { factory.basic.blockEntityType }

    val FUELED_CRAFTER by lazy { factory.fueled.block }
    val FUELED_CRAFTER_ENTITY_TYPE by lazy { factory.fueled.blockEntityType }

    val REDSTONE_CRAFTER by lazy { factory.redstone.block }
    val REDSTONE_CRAFTER_ENTITY_TYPE by lazy { factory.redstone.blockEntityType }

    val SCREEN_HANDLER_TYPE = registerExtended(id("crafter"), ::CrafterScreenHandler)

    override fun onInitialize() {
        super.onInitialize()

        BASIC_CRAFTER?.let { block ->
            registerBlock(id("crafter"), block, BASIC_CRAFTER_ENTITY_TYPE!!)
            LOGGER.info("Automated crafter initialized")
        }

        FUELED_CRAFTER?.let { block ->
            registerBlock(id("fueled_crafter"), block, FUELED_CRAFTER_ENTITY_TYPE!!)
            LOGGER.info("Fueled automated crafter initialized")
        }

        REDSTONE_CRAFTER?.let { block ->
            registerBlock(id("redstone_crafter"), block, REDSTONE_CRAFTER_ENTITY_TYPE!!)
            LOGGER.info("Redstone automated crafter initialized")
        }
    }

    @Environment(CLIENT)
    override fun onInitializeClient() {
        super.onInitializeClient()
        registerScreen(SCREEN_HANDLER_TYPE, ::CrafterScreen)
    }

    fun registerBlock(
        id: Identifier,
        block: CrafterBlock,
        blockEntityType: BlockEntityType<CrafterBlockEntity>
    ) {
        registerBlock(id, block)
        registerItemFor(id, block) { group(ItemGroup.REDSTONE) }
        registerBlockEntity(id, blockEntityType)
        ItemStorage.SIDED.registerForBlockEntity(
            { blockEntity, _ -> blockEntity.dataAccessor.crafter },
            blockEntityType
        )
    }
}
