package dev.adirelle.adicrafter

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_SCREEN_HANDLER
import dev.adirelle.adicrafter.screen.CrafterScreen
import dev.adirelle.adicrafter.utils.lazyLogger
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry

@Environment(EnvType.CLIENT)
@Suppress("UNUSED")
object AdiCrafterClient : ClientModInitializer {

    private val logger by lazyLogger()

    override fun onInitializeClient() {
        ScreenRegistry.register(CRAFTER_SCREEN_HANDLER, ::CrafterScreen)
        logger.info("${AdiCrafter.MOD_ID} client initialized!")
    }
}
