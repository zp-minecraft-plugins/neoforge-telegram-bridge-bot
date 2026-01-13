import { Telegraf, Markup } from 'telegraf';
import { bridge } from 'bridge/bridge';

export function whoCommand(bot: Telegraf) {
  bot.command('who', async (ctx) => {
    if (!bridge.isMinecraftConnected()) {
      ctx.replyWithHTML('<i>Minecraft server is not connected</i>');
      return;
    }

    const response = await bridge.getPlayers();

    if (!response) {
      ctx.replyWithHTML('<i>Failed to get player list (timeout)</i>');
      return;
    }

    const message = `<b>Players online: ${response.count}/${response.max}</b>`;
    ctx.replyWithHTML(
      message,
      Markup.inlineKeyboard([
        Markup.button.callback('👥 Show Players', 'show_players')
      ])
    );
  });
}
