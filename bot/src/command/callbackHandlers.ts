import { Telegraf } from 'telegraf';
import { bridge } from 'bridge/bridge';

export function registerCallbackHandlers(bot: Telegraf) {
  bot.action('show_players', async (ctx) => {
    if (!bridge.isMinecraftConnected()) {
      await ctx.answerCbQuery('Minecraft server is not connected');
      return;
    }

    const response = await bridge.getPlayers();

    if (!response) {
      await ctx.answerCbQuery('Failed to get player list');
      return;
    }

    if (response.count === 0) {
      await ctx.answerCbQuery('No players online');
      return;
    }

    const playerList = response.players.map((p) => `• ${p}`).join('\n');
    const message = `<b>Online Players (${response.count}/${response.max}):</b>\n${playerList}`;

    await ctx.answerCbQuery();
    await ctx.replyWithHTML(message);
  });
}
