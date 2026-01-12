import { Telegraf } from 'telegraf';

import { chatEvent } from 'event/chatEvent';

export function registerEvent(bot: Telegraf) {
  chatEvent(bot);
}
