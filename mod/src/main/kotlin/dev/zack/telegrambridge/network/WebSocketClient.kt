package dev.zack.telegrambridge.network

import dev.zack.telegrambridge.TelegramBridgeMod
import dev.zack.telegrambridge.config.ModConfig
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.neoforged.neoforge.server.ServerLifecycleHooks
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlin.math.min

// Messages sent TO the bot
@Serializable
sealed class OutgoingMessage {
    @Serializable
    data class Auth(val type: String = "auth", val secret: String) : OutgoingMessage()

    @Serializable
    data class Chat(
        val type: String = "chat",
        val player: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : OutgoingMessage()

    @Serializable
    data class Join(
        val type: String = "join",
        val player: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : OutgoingMessage()

    @Serializable
    data class Leave(
        val type: String = "leave",
        val player: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : OutgoingMessage()

    @Serializable
    data class Death(
        val type: String = "death",
        val player: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : OutgoingMessage()

    @Serializable
    data class Advancement(
        val type: String = "advancement",
        val player: String,
        val advancement: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : OutgoingMessage()

    @Serializable
    data class PlayersResponse(
        val type: String = "players_response",
        val players: List<String>,
        val count: Int,
        val max: Int
    ) : OutgoingMessage()
}

// Messages received FROM the bot
@Serializable
data class IncomingMessage(
    val type: String,
    val username: String? = null,
    val message: String? = null
)

object WebSocketClient {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var currentReconnectDelay = ModConfig.config.reconnectDelayMs
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var isConnected = false

    @Volatile
    private var shouldReconnect = true

    fun connect() {
        shouldReconnect = true
        doConnect()
    }

    private fun doConnect() {
        val url = ModConfig.config.botUrl
        TelegramBridgeMod.LOGGER.info("Connecting to bot at $url")

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                TelegramBridgeMod.LOGGER.info("WebSocket connected, authenticating...")
                isConnected = true
                currentReconnectDelay = ModConfig.config.reconnectDelayMs

                // Send authentication
                val auth = OutgoingMessage.Auth(secret = ModConfig.config.secret)
                webSocket.send(json.encodeToString(auth))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                TelegramBridgeMod.LOGGER.info("WebSocket closing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                TelegramBridgeMod.LOGGER.info("WebSocket closed: $code $reason")
                isConnected = false
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                TelegramBridgeMod.LOGGER.error("WebSocket error: ${t.message}")
                isConnected = false
                scheduleReconnect()
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val message = json.decodeFromString<IncomingMessage>(text)
            when (message.type) {
                "chat" -> handleChatMessage(message)
                "get_players" -> handleGetPlayers()
                else -> TelegramBridgeMod.LOGGER.warn("Unknown message type: ${message.type}")
            }
        } catch (e: Exception) {
            TelegramBridgeMod.LOGGER.error("Failed to parse message: $text", e)
        }
    }

    private fun handleChatMessage(message: IncomingMessage) {
        val server = ServerLifecycleHooks.getCurrentServer() ?: return
        val username = message.username ?: "Unknown"
        val text = message.message ?: return

        // Broadcast to all players with colored formatting
        val component = Component.literal("")
            .append(Component.literal("[Telegram] ").withStyle { it.withColor(0x0088cc) })
            .append(Component.literal("$username: ").withStyle { it.withBold(true) })
            .append(Component.literal(text))

        server.playerList.broadcastSystemMessage(component, false)
        TelegramBridgeMod.LOGGER.info("[Telegram] $username: $text")
    }

    private fun handleGetPlayers() {
        val server = ServerLifecycleHooks.getCurrentServer() ?: return
        val players = server.playerList.players.map { it.gameProfile.name }
        val response = OutgoingMessage.PlayersResponse(
            players = players,
            count = players.size,
            max = server.maxPlayers
        )
        send(response)
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            TelegramBridgeMod.LOGGER.info("Reconnecting in ${currentReconnectDelay}ms...")
            delay(currentReconnectDelay)

            // Exponential backoff
            currentReconnectDelay = min(
                currentReconnectDelay * 2,
                ModConfig.config.maxReconnectDelayMs
            )

            doConnect()
        }
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "Server shutting down")
        webSocket = null
        isConnected = false
    }

    fun send(message: OutgoingMessage) {
        if (!isConnected) {
            TelegramBridgeMod.LOGGER.warn("Cannot send message, not connected")
            return
        }

        try {
            val text = when (message) {
                is OutgoingMessage.Auth -> json.encodeToString(message)
                is OutgoingMessage.Chat -> json.encodeToString(message)
                is OutgoingMessage.Join -> json.encodeToString(message)
                is OutgoingMessage.Leave -> json.encodeToString(message)
                is OutgoingMessage.Death -> json.encodeToString(message)
                is OutgoingMessage.Advancement -> json.encodeToString(message)
                is OutgoingMessage.PlayersResponse -> json.encodeToString(message)
            }
            webSocket?.send(text)
        } catch (e: Exception) {
            TelegramBridgeMod.LOGGER.error("Failed to send message", e)
        }
    }

    fun isConnected(): Boolean = isConnected
}
