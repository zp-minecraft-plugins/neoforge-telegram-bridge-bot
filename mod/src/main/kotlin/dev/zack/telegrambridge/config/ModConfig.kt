package dev.zack.telegrambridge.config

import dev.zack.telegrambridge.TelegramBridgeMod
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.neoforged.fml.loading.FMLPaths
import java.nio.file.Files
import java.nio.file.Path

@Serializable
data class ConfigData(
    val botUrl: String = "ws://localhost:3000/ws",
    val secret: String = "change-me-to-match-bot-ws-secret",
    val reconnectDelayMs: Long = 5000,
    val maxReconnectDelayMs: Long = 60000
)

object ModConfig {
    private val configPath: Path = FMLPaths.CONFIGDIR.get().resolve("telegrambridge.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    var config: ConfigData = ConfigData()
        private set

    fun load() {
        try {
            if (Files.exists(configPath)) {
                val content = Files.readString(configPath)
                config = json.decodeFromString<ConfigData>(content)
                TelegramBridgeMod.LOGGER.info("Config loaded from $configPath")
            } else {
                save()
                TelegramBridgeMod.LOGGER.info("Default config created at $configPath")
            }
        } catch (e: Exception) {
            TelegramBridgeMod.LOGGER.error("Failed to load config, using defaults", e)
            config = ConfigData()
            save()
        }
    }

    private fun save() {
        try {
            val content = json.encodeToString(ConfigData.serializer(), config)
            Files.writeString(configPath, content)
        } catch (e: Exception) {
            TelegramBridgeMod.LOGGER.error("Failed to save config", e)
        }
    }
}
