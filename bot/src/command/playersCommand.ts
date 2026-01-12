import { Telegraf } from 'telegraf';
import { bridge } from 'bridge/bridge';

export function playersCommand(bot: Telegraf) {
  bot.command('players', async (ctx) => {
    if (!bridge.isMinecraftConnected()) {
      ctx.replyWithHTML('<i>Minecraft server is not connected</i>');
      return;
    }

    const response = await bridge.getPlayers();

    if (!response) {
      ctx.replyWithHTML('<i>Failed to get player list (timeout)</i>');
      return;
    }

    if (response.count === 0) {
      ctx.replyWithHTML('<i>No players online</i>');
      return;
    }

    const playerList = response.players.map((p) => `• ${p}`).join('\n');
    const message = `<b>Online Players (${response.count}/${response.max}):</b>\n${playerList}`;
    ctx.replyWithHTML(message);
  });
}
