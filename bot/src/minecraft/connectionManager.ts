import { EventEmitter } from 'events';
import WebSocket from 'ws';
import {
  MinecraftEvent,
  BotMessage,
  isMinecraftEvent,
  isAuthMessage
} from 'minecraft/types';
import { getEnv } from 'shared/env';

interface ConnectionManagerEvents {
  connected: () => void;
  disconnected: () => void;
  minecraft_event: (event: MinecraftEvent) => void;
}

class ConnectionManager extends EventEmitter {
  private connection: WebSocket | null = null;
  private authenticated = false;
  private messageQueue: BotMessage[] = [];

  constructor() {
    super();
  }

  setConnection(ws: WebSocket): void {
    this.connection = ws;
    this.authenticated = false;

    ws.on('message', (data: Buffer) => {
      this.handleMessage(data);
    });

    ws.on('close', () => {
      console.log('[ConnectionManager] Mod disconnected');
      this.connection = null;
      this.authenticated = false;
      this.emit('disconnected');
    });

    ws.on('error', (error) => {
      console.error('[ConnectionManager] WebSocket error:', error);
    });
  }

  private handleMessage(data: Buffer): void {
    try {
      const message = JSON.parse(data.toString());

      // Handle authentication
      if (!this.authenticated) {
        if (isAuthMessage(message)) {
          const secret = getEnv('WS_SECRET');
          if (message.secret === secret) {
            this.authenticated = true;
            console.log('[ConnectionManager] Mod authenticated successfully');
            this.emit('connected');
            this.flushMessageQueue();
          } else {
            console.warn(
              '[ConnectionManager] Invalid auth secret, closing connection'
            );
            this.connection?.close(4001, 'Invalid authentication');
          }
        } else {
          console.warn(
            '[ConnectionManager] First message must be auth, closing connection'
          );
          this.connection?.close(4002, 'Authentication required');
        }
        return;
      }

      // Handle Minecraft events
      if (isMinecraftEvent(message)) {
        this.emit('minecraft_event', message);
      } else {
        console.warn('[ConnectionManager] Unknown message type:', message);
      }
    } catch (error) {
      console.error('[ConnectionManager] Failed to parse message:', error);
    }
  }

  private flushMessageQueue(): void {
    while (this.messageQueue.length > 0) {
      const message = this.messageQueue.shift();
      if (message) {
        this.sendToMod(message);
      }
    }
  }

  sendToMod(message: BotMessage): boolean {
    if (!this.connection || !this.authenticated) {
      // Queue message for when connection is restored
      this.messageQueue.push(message);
      console.log('[ConnectionManager] Connection not ready, message queued');
      return false;
    }

    try {
      this.connection.send(JSON.stringify(message));
      return true;
    } catch (error) {
      console.error('[ConnectionManager] Failed to send message:', error);
      this.messageQueue.push(message);
      return false;
    }
  }

  isConnected(): boolean {
    return this.connection !== null && this.authenticated;
  }

  // Type-safe event emitter methods
  on<K extends keyof ConnectionManagerEvents>(
    event: K,
    listener: ConnectionManagerEvents[K]
  ): this {
    return super.on(event, listener);
  }

  emit<K extends keyof ConnectionManagerEvents>(
    event: K,
    ...args: Parameters<ConnectionManagerEvents[K]>
  ): boolean {
    return super.emit(event, ...args);
  }
}

// Singleton instance
export const connectionManager = new ConnectionManager();
