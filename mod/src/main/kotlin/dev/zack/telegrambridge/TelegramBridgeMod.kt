package dev.zack.telegrambridge

import dev.zack.telegrambridge.commands.TelegramBridgeCommand
import dev.zack.telegrambridge.config.ModConfig
import dev.zack.telegrambridge.events.AdvancementHandler
import dev.zack.telegrambridge.events.ChatHandler
import dev.zack.telegrambridge.events.FTBQuestsHandler
import dev.zack.telegrambridge.events.PlayerHandler
import dev.zack.telegrambridge.network.WebSocketClient
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(TelegramBridgeMod.MOD_ID)
class TelegramBridgeMod(modEventBus: IEventBus, modContainer: ModContainer) {
    companion object {
        const val MOD_ID = "telegrambridge"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }

    init {
        // Load configuration
        ModConfig.load()

        // Register mod lifecycle events
        modEventBus.addListener(::onDedicatedServerSetup)

        // Register game events
        NeoForge.EVENT_BUS.addListener(::onServerStarted)
        NeoForge.EVENT_BUS.addListener(::onServerStopping)
        NeoForge.EVENT_BUS.addListener(::onRegisterCommands)

        // Register event handlers
        ChatHandler.register()
        PlayerHandler.register()
        AdvancementHandler.register()
        FTBQuestsHandler.register()

        LOGGER.info("Telegram Bridge mod initialized")
    }

    private fun onDedicatedServerSetup(event: FMLDedicatedServerSetupEvent) {
        LOGGER.info("Telegram Bridge configuring for dedicated server")
    }

    private fun onServerStarted(event: ServerStartedEvent) {
        LOGGER.info("Server started, connecting to Telegram bot...")
        WebSocketClient.connect()
    }

    private fun onServerStopping(event: ServerStoppingEvent) {
        LOGGER.info("Server stopping, disconnecting from Telegram bot...")
        WebSocketClient.disconnect()
    }

    private fun onRegisterCommands(event: RegisterCommandsEvent) {
        TelegramBridgeCommand.register(event.dispatcher)
    }
}
