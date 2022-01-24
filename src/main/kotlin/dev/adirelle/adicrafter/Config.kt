package dev.adirelle.adicrafter

import dev.adirelle.adicrafter.crafter.CrafterConfig
import dev.adirelle.adicrafter.utils.lazyLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader

@Serializable
data class Config(
    val version: Int = 0,
    val crafter: CrafterConfig = CrafterConfig()
) {

    companion object {

        const val CURRENT_VERSION = 1

        private val logger by lazyLogger(Config::class.java)

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
                    logger.info("configuration loaded from file")
                }
            } catch (t: Throwable) {
                logger.warn("could not read configuration file", t)
                Config()
            }

        fun loadOrCreate(): Config {
            var config = if (configFile.exists()) load() else Config()
            if (config.version < CURRENT_VERSION) {
                logger.warn("loaded previous version of configuration, reseting to default")
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
            logger.warn("could not write configuration file", t)
        }
    }
}
