import { Telegraf } from 'telegraf';
import { connectionManager } from 'minecraft/connectionManager';
import {
  MinecraftEvent,
  MinecraftChatEvent,
  MinecraftJoinEvent,
  MinecraftLeaveEvent,
  MinecraftDeathEvent,
  MinecraftAdvancementEvent,
  MinecraftQuestCompleteEvent,
  MinecraftPlayersResponse
} from 'minecraft/types';
import { getEnv } from 'shared/env';

class Bridge {
  private bot: Telegraf | null = null;
  private chatId: string | null = null;
  private playersCallback:
    | ((response: MinecraftPlayersResponse) => void)
    | null = null;

  init(bot: Telegraf): void {
    this.bot = bot;
    this.chatId = getEnv('TELEGRAM_CHAT_ID');

    // Listen for Minecraft events
    connectionManager.on('minecraft_event', (event) => {
      this.handleMinecraftEvent(event);
    });

    // Notify Telegram when mod connects/disconnects
    connectionManager.on('connected', () => {
      this.sendToTelegram('🟢 <b>Minecraft server connected</b>');
    });

    connectionManager.on('disconnected', () => {
      this.sendToTelegram('🔴 <b>Minecraft server disconnected</b>');
    });

    console.log('[Bridge] Initialized');
  }

  private handleMinecraftEvent(event: MinecraftEvent): void {
    switch (event.type) {
      case 'chat':
        this.handleChat(event);
        break;
      case 'join':
        this.handleJoin(event);
        break;
      case 'leave':
        this.handleLeave(event);
        break;
      case 'death':
        this.handleDeath(event);
        break;
      case 'advancement':
        this.handleAdvancement(event);
        break;
      case 'quest_complete':
        this.handleQuestComplete(event);
        break;
      case 'players_response':
        this.handlePlayersResponse(event);
        break;
    }
  }

  private handleChat(event: MinecraftChatEvent): void {
    const message = `<b>${this.escapeHtml(event.player)}</b>: ${this.escapeHtml(event.message)}`;
    this.sendToTelegram(message);
  }

  private handleJoin(event: MinecraftJoinEvent): void {
    const message = `🟢 <b>${this.escapeHtml(event.player)}</b> joined the game`;
    this.sendToTelegram(message);
  }

  private handleLeave(event: MinecraftLeaveEvent): void {
    const message = `🔴 <b>${this.escapeHtml(event.player)}</b> left the game`;
    this.sendToTelegram(message);
  }

  private handleDeath(event: MinecraftDeathEvent): void {
    const message = `💀 <b>${this.escapeHtml(event.player)}</b> ${this.escapeHtml(event.message)}`;
    this.sendToTelegram(message);
  }

  private handleAdvancement(event: MinecraftAdvancementEvent): void {
    const message = `🏆 <b>${this.escapeHtml(event.player)}</b> has made the advancement <i>${this.escapeHtml(event.advancement)}</i>`;
    this.sendToTelegram(message);
  }

  private handleQuestComplete(event: MinecraftQuestCompleteEvent): void {
    const emoji = event.isChapter ? '📖' : '✅';
    const type = event.isChapter ? 'chapter' : 'quest';
    const message = `${emoji} <b>${this.escapeHtml(event.player)}</b> completed ${type} <i>${this.escapeHtml(event.questName)}</i> in <i>${this.escapeHtml(event.chapterName)}</i>`;
    this.sendToTelegram(message);
  }

  private handlePlayersResponse(response: MinecraftPlayersResponse): void {
    if (this.playersCallback) {
      this.playersCallback(response);
      this.playersCallback = null;
    }
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

  private async sendToTelegram(message: string): Promise<void> {
    if (!this.bot || !this.chatId) {
      console.error('[Bridge] Bot or chat ID not configured');
      return;
    }

    try {
      await this.bot.telegram.sendMessage(this.chatId, message, {
        parse_mode: 'HTML'
      });
    } catch (error) {
      console.error('[Bridge] Failed to send message to Telegram:', error);
    }
  }

  // Send a message from Telegram to Minecraft
  sendToMinecraft(username: string, message: string): void {
    console.log(`[Bridge] Sending to Minecraft: ${username}: ${message}`);
    const success = connectionManager.sendToMod({
      type: 'chat',
      username,
      message
    });
    console.log(`[Bridge] Message sent: ${success}`);
  }

  // Request player list from Minecraft
  async getPlayers(): Promise<MinecraftPlayersResponse | null> {
    if (!connectionManager.isConnected()) {
      return null;
    }

    return new Promise((resolve) => {
      // Set up callback with timeout
      const timeout = setTimeout(() => {
        this.playersCallback = null;
        resolve(null);
      }, 5000);

      this.playersCallback = (response) => {
        clearTimeout(timeout);
        resolve(response);
      };

      connectionManager.sendToMod({ type: 'get_players' });
    });
  }

  isMinecraftConnected(): boolean {
    return connectionManager.isConnected();
  }
}

// Singleton instance
export const bridge = new Bridge();
