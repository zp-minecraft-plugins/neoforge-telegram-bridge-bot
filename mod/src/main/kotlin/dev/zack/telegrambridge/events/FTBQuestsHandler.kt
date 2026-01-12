package dev.zack.telegrambridge.events

import dev.zack.telegrambridge.TelegramBridgeMod
import dev.zack.telegrambridge.config.ModConfig
import dev.zack.telegrambridge.network.OutgoingMessage
import dev.zack.telegrambridge.network.WebSocketClient
import net.neoforged.fml.ModList

object FTBQuestsHandler {
    private var isAvailable = false

    fun register() {
        // Check if FTB Quests is loaded
        if (!ModList.get().isLoaded("ftbquests")) {
            TelegramBridgeMod.LOGGER.info("FTB Quests not found, skipping integration")
            return
        }

        try {
            // Register using a separate class to avoid ClassNotFoundException
            FTBQuestsEventHandler.register()
            isAvailable = true
            TelegramBridgeMod.LOGGER.info("FTB Quests integration registered")
        } catch (e: Exception) {
            TelegramBridgeMod.LOGGER.error("Failed to register FTB Quests integration", e)
        }
    }

    fun isAvailable(): Boolean = isAvailable
}

// Separate class to isolate FTB Quests imports
private object FTBQuestsEventHandler {
    fun register() {
        dev.ftb.mods.ftbquests.events.ObjectCompletedEvent.GENERIC.register { event ->
            if (!ModConfig.config.questNotifications) {
                return@register dev.architectury.event.EventResult.pass()
            }

            val teamData = event.data
            val questObject = event.`object`

            // Get team/player name - use team name if available
            val teamName = teamData.name?.toString() ?: "Unknown"

            // Get quest and chapter names
            val questName = questObject.title.string
            val chapter = questObject.getQuestChapter()
            val chapterName = chapter?.title?.string ?: "Unknown"
            val isChapter = questObject is dev.ftb.mods.ftbquests.quest.Chapter

            TelegramBridgeMod.LOGGER.debug(
                "Quest completed: $teamName completed $questName in chapter $chapterName (isChapter=$isChapter)"
            )

            WebSocketClient.send(
                OutgoingMessage.QuestComplete(
                    player = teamName,
                    questName = questName,
                    chapterName = chapterName,
                    isChapter = isChapter
                )
            )

            dev.architectury.event.EventResult.pass()
        }
    }
}
