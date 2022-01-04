@file:Suppress("MemberVisibilityCanBePrivate", "PropertyName")

package dev.adirelle.adicrafter.utils.mod

import com.mojang.datafixers.types.Type
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.EnvType.SERVER
import net.fabricmc.api.EnvironmentInterface
import net.fabricmc.api.ModInitializer
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


interface ClientSide

interface ServerSide

interface ModInit {

    val MOD_ID: String
    val LOGGER: Logger
}

@EnvironmentInterface(CLIENT, itf = ClientSide::class)
@EnvironmentInterface(SERVER, itf = ServerSide::class)
interface ModSidedInit : ModInit {

    fun serverOnly(block: ModInit.() -> Unit) {
        if (this is ServerSide) {
            this.block()
        }
    }

    fun clientOnly(block: ModInit.() -> Unit) {
        if (this is ClientSide) {
            this.block()
        }
    }
}

interface Mod : ModSidedInit, ModInitializer {

    fun features(block: () -> Unit) {
        LOGGER.info("loading features")
        block()
        LOGGER.info("features loaded")
    }
}

//inline fun ModSidedInit.serverOnly(crossinline block: ModInit.() -> Unit) {
//    if (this is ClientSide) {
//        this.block()
//    }
//}
//
//inline fun ModSidedInit.clientOnly(crossinline block: ModInit.() -> Unit) {
//    if (this is ClientSide) {
//        this.block()
//    }
//}

inline fun mod(MOD_ID: String, crossinline block: Mod.() -> Unit): Mod =
    object : Mod {
        override val LOGGER by lazy { LogManager.getLogger(MOD_ID)!! }
        override val MOD_ID = MOD_ID

        override fun onInitialize() {
            LOGGER.info("initializing $MOD_ID")
            block(this)
            serverOnly { LOGGER.info("$MOD_ID initialized on server") }
            clientOnly { LOGGER.info("$MOD_ID initialized on client") }
            LOGGER.info("$MOD_ID initialized")
        }
    }

open class ModFeature(mod: ModInit, val NAME: String) : ModSidedInit {

    final override val LOGGER by lazy { LogManager.getLogger("${MOD_ID}:${NAME}")!! }
    final override val MOD_ID = mod.MOD_ID
    val ID = Identifier(MOD_ID, NAME)

    fun <T : Block> register(entry: T, id: Identifier = ID) =
        Registry.BLOCK.register(id, entry)

    fun <T : Item> register(entry: T, id: Identifier = ID) =
        Registry.ITEM.register(id, entry)

    inline fun <T : Block> registerItemFor(
        block: T,
        crossinline settings: FabricItemSettings.() -> FabricItemSettings
    ) =
        register(BlockItem(block, FabricItemSettings().settings()))!!

    fun <T : Block> registerItemFor(block: T) =
        register(BlockItem(block, FabricItemSettings()))!!

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
}
