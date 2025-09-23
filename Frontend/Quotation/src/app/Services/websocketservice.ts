import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: any;
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  async connect(): Promise<void> {
    // Only initialize WebSocket in browser environment
    if (!this.isBrowser) {
      console.log('WebSocket connection skipped on server');
      return;
    }

    try {
      // Dynamic import to avoid loading on server
      const { Client } = await import('@stomp/stompjs');
      const SockJS = (await import('sockjs-client')).default;

      this.stompClient = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        connectHeaders: {
          // your headers
        },
        debug: (str) => {
          console.log('STOMP: ' + str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      this.stompClient.onConnect = (frame: any) => {
        console.log('Connected: ' + frame);
      };

      this.stompClient.onStompError = (frame: any) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      };

      this.stompClient.activate();
    } catch (error) {
      console.error('Failed to initialize WebSocket:', error);
    }
  }

  disconnect(): void {
    if (this.stompClient && this.isBrowser) {
      this.stompClient.deactivate();
    }
  }
}
