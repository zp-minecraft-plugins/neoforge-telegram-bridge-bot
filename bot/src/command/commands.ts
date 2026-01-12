import { Telegraf } from 'telegraf';
import { pingCommand } from 'command/pingCommand';
import { playersCommand } from 'command/playersCommand';

export function registerCommands(bot: Telegraf) {
  pingCommand(bot);
  playersCommand(bot);
}
