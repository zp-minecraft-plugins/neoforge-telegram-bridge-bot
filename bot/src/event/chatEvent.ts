import { Telegraf, Context } from 'telegraf';
import { Message, Update } from 'telegraf/types';
import { bridge } from 'bridge/bridge';
import { getEnv } from 'shared/env';

type TextContext = Context<Update.MessageUpdate<Message.TextMessage>>;

export function chatEvent(bot: Telegraf) {
  const targetChatId = getEnv('TELEGRAM_CHAT_ID');
  console.log(`[ChatEvent] Listening for messages from chat ID: ${targetChatId}`);

  // Debug: log all incoming updates
  bot.use((ctx, next) => {
    console.log(`[ChatEvent] Received update type: ${ctx.updateType}`);
    return next();
  });

  bot.on('text', (ctx: TextContext) => {
    const incomingChatId = ctx.chat.id.toString();

    // Only forward messages from the configured chat
    if (incomingChatId !== targetChatId) {
      console.log(
        `[ChatEvent] Ignoring message from chat ${incomingChatId} (expected ${targetChatId})`
      );
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

    console.log(`[ChatEvent] Forwarding message from ${username}: ${message}`);

    // Forward to Minecraft
    bridge.sendToMinecraft(username, message);
  });
}
