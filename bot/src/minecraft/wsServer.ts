import { Server as HttpServer } from 'http';
import { WebSocketServer, WebSocket } from 'ws';
import { connectionManager } from 'minecraft/connectionManager';

let wss: WebSocketServer | null = null;

export function initWebSocketServer(server: HttpServer): WebSocketServer {
  wss = new WebSocketServer({
    server,
    path: '/ws'
  });

  console.log('[WebSocket] Server initialized on /ws');

  wss.on('connection', (ws: WebSocket, request) => {
    const clientIp = request.socket.remoteAddress;
    console.log(`[WebSocket] New connection from ${clientIp}`);

    // Only allow one connection at a time (single Minecraft server)
    if (connectionManager.isConnected()) {
      console.log(
        '[WebSocket] Rejecting connection - another mod is already connected'
      );
      ws.close(4003, 'Another mod is already connected');
      return;
    }

    connectionManager.setConnection(ws);
  });

  wss.on('error', (error) => {
    console.error('[WebSocket] Server error:', error);
  });

  return wss;
}

export function getWebSocketServer(): WebSocketServer | null {
  return wss;
}
