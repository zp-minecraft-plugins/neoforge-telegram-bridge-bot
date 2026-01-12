import { Telegraf, Context } from 'telegraf';
import { Message, Update } from 'telegraf/types';
import { bridge } from 'bridge/bridge';
import { getEnv } from 'shared/env';

type TextContext = Context<Update.MessageUpdate<Message.TextMessage>>;

export function chatEvent(bot: Telegraf) {
  const targetChatId = getEnv('TELEGRAM_CHAT_ID');

  bot.on('text', (ctx: TextContext) => {
    // Only forward messages from the configured chat
    if (ctx.chat.id.toString() !== targetChatId) {
      return;
    }

    // Ignore commands
    if (ctx.message.text.startsWith('/')) {
      return;
    }

    // Ignore bot messages
    if (ctx.from.is_bot) {
      return;
    }

    // Get username (prefer username, fall back to first name)
    const username = ctx.from.username || ctx.from.first_name || 'Unknown';
    const message = ctx.message.text;

    // Forward to Minecraft
    bridge.sendToMinecraft(username, message);
  });
}
