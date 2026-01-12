package dev.zack.telegrambridge.events

import dev.zack.telegrambridge.TelegramBridgeMod
import dev.zack.telegrambridge.network.OutgoingMessage
import dev.zack.telegrambridge.network.WebSocketClient
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.ServerChatEvent

object ChatHandler {
    fun register() {
        NeoForge.EVENT_BUS.addListener(::onServerChat)
        TelegramBridgeMod.LOGGER.info("Chat handler registered")
    }

    private fun onServerChat(event: ServerChatEvent) {
        val player = event.player.gameProfile.name
        val message = event.message.string

        TelegramBridgeMod.LOGGER.debug("Chat: $player: $message")

        WebSocketClient.send(
            OutgoingMessage.Chat(
                player = player,
                message = message
            )
        )
    }
}
