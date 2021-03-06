package dev.adirelle.adicrafter

import dev.adirelle.adicrafter.crafter.CrafterConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader

@Serializable
data class Config(
    val version: Int = CURRENT_VERSION,
    val crafter: CrafterConfig = CrafterConfig()
) {

    companion object {

        val LOGGER by AdiCrafter::LOGGER

        const val CURRENT_VERSION = 1

        private val json = Json {
            encodeDefaults = true
            prettyPrint = true
            ignoreUnknownKeys = true
        }

        private val configFile by lazy {
            FabricLoader.getInstance().configDir.resolve("${AdiCrafter.MOD_ID}.json").toFile()
        }

        private fun load(): Config =
            try {
                json.decodeFromString<Config>(configFile.readText()).also {
                    LOGGER.info("Configuration loaded from file")
                }
            } catch (t: Throwable) {
                LOGGER.warn("Could not read configuration file", t)
                Config()
            }

        fun loadOrCreate(): Config {
            var config = if (configFile.exists()) load() else Config()
            if (config.version < CURRENT_VERSION) {
                LOGGER.warn("Ignoring previous version of configuration, reset to default")
                config = Config()
            }
            config.save()
            return config
        }
    }

    fun save() {
        try {
            val content = json.encodeToString(serializer(), this)
            configFile.writeText(content)
        } catch (t: Throwable) {
            LOGGER.warn("Could not write configuration file", t)
        }
    }
}
