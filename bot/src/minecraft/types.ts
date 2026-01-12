// Message types for WebSocket communication between bot and Minecraft mod

// Mod -> Bot messages
export type MinecraftEventType =
  | 'chat'
  | 'join'
  | 'leave'
  | 'death'
  | 'advancement'
  | 'quest_complete'
  | 'players_response';

export interface MinecraftChatEvent {
  type: 'chat';
  player: string;
  message: string;
  timestamp: number;
}

export interface MinecraftJoinEvent {
  type: 'join';
  player: string;
  timestamp: number;
}

export interface MinecraftLeaveEvent {
  type: 'leave';
  player: string;
  timestamp: number;
}

export interface MinecraftDeathEvent {
  type: 'death';
  player: string;
  message: string; // Death message like "was slain by Zombie"
  timestamp: number;
}

export interface MinecraftAdvancementEvent {
  type: 'advancement';
  player: string;
  advancement: string; // Advancement title
  timestamp: number;
}

export interface MinecraftQuestCompleteEvent {
  type: 'quest_complete';
  player: string;
  questName: string;
  chapterName: string;
  isChapter: boolean;
  timestamp: number;
}

export interface MinecraftPlayersResponse {
  type: 'players_response';
  players: string[];
  count: number;
  max: number;
}

export type MinecraftEvent =
  | MinecraftChatEvent
  | MinecraftJoinEvent
  | MinecraftLeaveEvent
  | MinecraftDeathEvent
  | MinecraftAdvancementEvent
  | MinecraftQuestCompleteEvent
  | MinecraftPlayersResponse;

// Bot -> Mod messages
export type BotMessageType = 'chat' | 'get_players';

export interface BotChatMessage {
  type: 'chat';
  username: string;
  message: string;
}

export interface BotGetPlayersMessage {
  type: 'get_players';
}

export type BotMessage = BotChatMessage | BotGetPlayersMessage;

// Authentication message (mod sends on connect)
export interface AuthMessage {
  type: 'auth';
  secret: string;
}

// Type guards
export function isMinecraftEvent(data: unknown): data is MinecraftEvent {
  if (typeof data !== 'object' || data === null) return false;
  const obj = data as Record<string, unknown>;
  return (
    typeof obj.type === 'string' &&
    [
      'chat',
      'join',
      'leave',
      'death',
      'advancement',
      'quest_complete',
      'players_response'
    ].includes(obj.type)
  );
}

export function isAuthMessage(data: unknown): data is AuthMessage {
  if (typeof data !== 'object' || data === null) return false;
  const obj = data as Record<string, unknown>;
  return obj.type === 'auth' && typeof obj.secret === 'string';
}
