package dev.adirelle.adicrafter

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_SCREEN_HANDLER
import dev.adirelle.adicrafter.screen.CrafterScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import org.apache.logging.log4j.LogManager

@Environment(EnvType.CLIENT)
@Suppress("UNUSED")
object AdiCrafterClient : ClientModInitializer {
    private val LOGGER = LogManager.getLogger()!!

    override fun onInitializeClient() {
        ScreenRegistry.register(CRAFTER_SCREEN_HANDLER, ::CrafterScreen)

        LOGGER.info("${AdiCrafter.MOD_ID} client initialized!")
    }
}
