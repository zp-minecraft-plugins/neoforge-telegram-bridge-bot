package dev.zack.telegrambridge.events

import dev.zack.telegrambridge.TelegramBridgeMod
import dev.zack.telegrambridge.network.OutgoingMessage
import dev.zack.telegrambridge.network.WebSocketClient
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent

object PlayerHandler {
    fun register() {
        NeoForge.EVENT_BUS.addListener(::onPlayerLoggedIn)
        NeoForge.EVENT_BUS.addListener(::onPlayerLoggedOut)
        NeoForge.EVENT_BUS.addListener(::onPlayerDeath)
        TelegramBridgeMod.LOGGER.info("Player handler registered")
    }

    private fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity
        if (player is ServerPlayer) {
            val name = player.gameProfile.name
            TelegramBridgeMod.LOGGER.debug("Player joined: $name")

            WebSocketClient.send(
                OutgoingMessage.Join(player = name)
            )
        }
    }

    private fun onPlayerLoggedOut(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity
        if (player is ServerPlayer) {
            val name = player.gameProfile.name
            TelegramBridgeMod.LOGGER.debug("Player left: $name")

            WebSocketClient.send(
                OutgoingMessage.Leave(player = name)
            )
        }
    }

    private fun onPlayerDeath(event: LivingDeathEvent) {
        val entity = event.entity
        if (entity is ServerPlayer) {
            val name = entity.gameProfile.name
            val source = event.source

            // Get the death message
            val deathMessage = source.getLocalizedDeathMessage(entity).string
            // Remove player name from start since we add it separately
            val message = deathMessage.removePrefix(name).trim()

            TelegramBridgeMod.LOGGER.debug("Player died: $name - $message")

            WebSocketClient.send(
                OutgoingMessage.Death(
                    player = name,
                    message = message
                )
            )
        }
    }
}
