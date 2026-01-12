package dev.zack.telegrambridge.events

import dev.zack.telegrambridge.TelegramBridgeMod
import dev.zack.telegrambridge.network.OutgoingMessage
import dev.zack.telegrambridge.network.WebSocketClient
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.AdvancementEvent

object AdvancementHandler {
    fun register() {
        NeoForge.EVENT_BUS.addListener(::onAdvancementEarn)
        TelegramBridgeMod.LOGGER.info("Advancement handler registered")
    }

    private fun onAdvancementEarn(event: AdvancementEvent.AdvancementEarnEvent) {
        val player = event.entity.gameProfile.name
        val advancement = event.advancement

        // Only send announcements for advancements that should be announced
        val display = advancement.value.display.orElse(null) ?: return
        if (!display.shouldAnnounceChat()) return

        val title = display.title.string

        TelegramBridgeMod.LOGGER.debug("Advancement: $player earned $title")

        WebSocketClient.send(
            OutgoingMessage.Advancement(
                player = player,
                advancement = title
            )
        )
    }
}
