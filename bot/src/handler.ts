import express, { Request, Response } from 'express';
import { createServer } from 'http';

import { Telegraf } from 'telegraf';
import { getEnv } from 'shared/env';
import { devSetup } from 'devSetup';
import { registerCommands } from 'command/commands';
import { registerEvent } from 'event/events';
import { initWebSocketServer } from 'minecraft/wsServer';
import { bridge } from 'bridge/bridge';

export async function handler() {
  const token = process.env.BOT_TOKEN;
  if (token === undefined) {
    throw new Error('BOT_TOKEN must be provided!');
  }

  // Validate required environment variables
  const telegramChatId = process.env.TELEGRAM_CHAT_ID;
  if (!telegramChatId) {
    throw new Error('TELEGRAM_CHAT_ID must be provided!');
  }

  const wsSecret = process.env.WS_SECRET;
  if (!wsSecret) {
    throw new Error('WS_SECRET must be provided!');
  }

  if (process.env.NODE_ENV === 'development') {
    await devSetup();
  }

  const bot = new Telegraf(token);

  // Initialize bridge with bot instance
  bridge.init(bot);

  registerCommands(bot);
  registerEvent(bot);

  const host = getEnv('host');
  const secretPath = `/telegraf/${bot.secretPathComponent()}`;

  await bot.telegram.setWebhook(`${host}${secretPath}`);

  const app = express();

  // Health check endpoint
  app.get('/', (req: Request, res: Response) =>
    res.send('Minecraft-Telegram Bridge Bot')
  );

  // Health check with status
  app.get('/health', (req: Request, res: Response) => {
    res.json({
      status: 'ok',
      minecraft_connected: bridge.isMinecraftConnected()
    });
  });

  // Set the bot API endpoint
  app.use(bot.webhookCallback(secretPath));

  // Create HTTP server from Express app
  const server = createServer(app);

  // Initialize WebSocket server
  initWebSocketServer(server);

  const port = process.env.PORT || 3000;
  server.listen(port, () => {
    console.log(`Bot listening on port ${port}`);
    console.log('WebSocket server ready for Minecraft mod connection');
  });
}

handler()
  .then(() => console.log('Bot Running'))
  .catch((error) => {
    console.error('Uncaught Error Thrown', error);
  });
