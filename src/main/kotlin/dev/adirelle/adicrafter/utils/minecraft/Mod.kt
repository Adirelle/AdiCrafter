@file:Suppress("MemberVisibilityCanBePrivate", "PropertyName")

package dev.adirelle.adicrafter.utils.mod

import com.mojang.datafixers.types.Type
import dev.adirelle.adicrafter.utils.general.lazyLogger
import net.fabricmc.api.*
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.EnvType.SERVER
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@EnvironmentInterface(CLIENT, itf = ClientModInitializer::class)
@EnvironmentInterface(SERVER, itf = DedicatedServerModInitializer::class)
interface SidedModInitalizer : ModInitializer, ClientModInitializer, DedicatedServerModInitializer {

    val LOGGER: Logger

    override fun onInitialize() {
    }

    @Environment(CLIENT)
    override fun onInitializeClient() {
    }

    @Environment(SERVER)
    override fun onInitializeServer() {
    }
}

open class Mod(val MOD_ID: String) : SidedModInitalizer {

    final override val LOGGER = LogManager.getLogger(MOD_ID)!!

    init {
        LOGGER.debug("initializing $MOD_ID")
    }

    fun feature(feature: ModFeature) {
        feature.onInitialize()

        @Suppress("USELESS_IS_CHECK")
        if (feature is ClientModInitializer) {
            feature.onInitializeClient()
        }

        @Suppress("USELESS_IS_CHECK")
        if (feature is DedicatedServerModInitializer) {
            feature.onInitializeServer()
        }

        LOGGER.debug("feature $feature initialized")
    }

    override fun toString() = MOD_ID
}

open class ModFeature(mod: Mod, val NAME: String) : SidedModInitalizer {

    final override val LOGGER by lazyLogger("${mod.MOD_ID}:${NAME}")

    val MOD_ID = mod.MOD_ID
    val ID = Identifier(MOD_ID, NAME)

    init {
        LOGGER.debug("initializing feature ${ID}")
    }

    fun <T : Block> register(entry: T, id: Identifier = ID): T =
        entry.also { Registry.BLOCK.register(id, it) }

    fun <T : Item> register(entry: T, id: Identifier = ID): T =
        entry.also { Registry.ITEM.register(id, entry) }

    inline fun <T : Block> registerItemFor(
        block: T,
        crossinline settings: FabricItemSettings.() -> FabricItemSettings
    ) =
        register(BlockItem(block, FabricItemSettings().settings()))

    fun <T : Block> registerItemFor(block: T) =
        register(BlockItem(block, FabricItemSettings()))

    fun <T : BlockEntity, S : T> register(
        factory: (BlockPos, BlockState) -> T,
        vararg blocks: Block,
        id: Identifier = ID,
        type: Type<S>? = null
    ): BlockEntityType<T> =
        BlockEntityType(factory, setOf(*blocks), type)
            .also { Registry.BLOCK_ENTITY_TYPE.register(id, it) }

    fun <T : ScreenHandler> register(factory: (Int, PlayerInventory) -> T, id: Identifier = ID): ScreenHandlerType<T> =
        ScreenHandlerRegistry.registerSimple(id, factory)

    fun <T : ScreenHandler> register(
        factory: (Int, PlayerInventory, PacketByteBuf) -> T,
        id: Identifier = ID
    ): ScreenHandlerType<T> =
        ScreenHandlerRegistry.registerExtended(id, factory)

    fun <T, S> register(
        type: ScreenHandlerType<T>,
        factory: (T, PlayerInventory, Text) -> S,
    ) where T : ScreenHandler, S : Screen, S : ScreenHandlerProvider<T> =
        ScreenRegistry.register(type, factory)

    private fun <T> Registry<T>.register(id: Identifier, entry: T): T =
        Registry.register(this, id, entry)

    override fun toString() = ID.toString()
}
