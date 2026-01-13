import { Telegraf } from 'telegraf';
import { pingCommand } from 'command/pingCommand';
import { playersCommand } from 'command/playersCommand';
import { whoCommand } from 'command/whoCommand';
import { registerCallbackHandlers } from 'command/callbackHandlers';

export function registerCommands(bot: Telegraf) {
  pingCommand(bot);
  playersCommand(bot);
  whoCommand(bot);
  registerCallbackHandlers(bot);
}
