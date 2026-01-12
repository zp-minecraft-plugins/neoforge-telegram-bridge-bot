package dev.zack.telegrambridge.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import dev.zack.telegrambridge.TelegramBridgeMod
import dev.zack.telegrambridge.config.ModConfig
import dev.zack.telegrambridge.events.FTBQuestsHandler
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object TelegramBridgeCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("telegrambridge")
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("quests")
                        .then(
                            Commands.literal("on")
                                .executes { ctx -> setQuestNotifications(ctx, true) }
                        )
                        .then(
                            Commands.literal("off")
                                .executes { ctx -> setQuestNotifications(ctx, false) }
                        )
                        .executes { ctx -> showQuestStatus(ctx) }
                )
                .then(
                    Commands.literal("status")
                        .executes { ctx -> showStatus(ctx) }
                )
        )

        TelegramBridgeMod.LOGGER.info("Commands registered")
    }

    private fun setQuestNotifications(ctx: CommandContext<CommandSourceStack>, enabled: Boolean): Int {
        if (!FTBQuestsHandler.isAvailable()) {
            ctx.source.sendFailure(Component.literal("FTB Quests is not installed"))
            return 0
        }

        ModConfig.config.questNotifications = enabled
        ModConfig.save()

        val status = if (enabled) "enabled" else "disabled"
        ctx.source.sendSuccess(
            { Component.literal("Quest notifications $status") },
            true
        )
        return 1
    }

    private fun showQuestStatus(ctx: CommandContext<CommandSourceStack>): Int {
        if (!FTBQuestsHandler.isAvailable()) {
            ctx.source.sendFailure(Component.literal("FTB Quests is not installed"))
            return 0
        }

        val status = if (ModConfig.config.questNotifications) "enabled" else "disabled"
        ctx.source.sendSuccess(
            { Component.literal("Quest notifications are currently $status") },
            false
        )
        return 1
    }

    private fun showStatus(ctx: CommandContext<CommandSourceStack>): Int {
        val questStatus = if (FTBQuestsHandler.isAvailable()) {
            val enabled = if (ModConfig.config.questNotifications) "enabled" else "disabled"
            "FTB Quests: $enabled"
        } else {
            "FTB Quests: not installed"
        }

        ctx.source.sendSuccess(
            { Component.literal("Telegram Bridge Status:\n$questStatus") },
            false
        )
        return 1
    }
}
