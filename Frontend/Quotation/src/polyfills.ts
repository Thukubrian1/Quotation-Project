/**
 * ZONELESS ANGULAR POLYFILLS
 * 
 * This application uses provideZonelessChangeDetection()
 * NO ZONE.JS imports are needed or wanted here
 */

// Browser-only polyfills for WebSocket libraries
if (typeof window !== 'undefined') {
  // Global polyfill for @stomp/stompjs
  if (typeof (window as any).global === 'undefined') {
    (window as any).global = globalThis || window;
  }

  // Process polyfill for sockjs-client
  if (typeof (window as any).process === 'undefined') {
    (window as any).process = {
      env: { NODE_ENV: 'production' },
      browser: true,
      version: '',
      versions: { node: '16.0.0' },
      nextTick: (fn: Function) => setTimeout(fn, 0),
      cwd: () => '/',
      title: 'browser',
      platform: 'browser'
    };
  }

  // Buffer polyfill if needed
  if (typeof (window as any).Buffer === 'undefined') {
    (window as any).Buffer = {
      isBuffer: () => false,
      from: (data: any) => new TextEncoder().encode(String(data)),
      alloc: (size: number) => new Uint8Array(size)
    };
  }

  // Additional polyfills for WebSocket libraries
  if (typeof (window as any).setImmediate === 'undefined') {
    (window as any).setImmediate = (fn: Function) => setTimeout(fn, 0);
  }
}

/**
 * DO NOT ADD ZONE.JS IMPORTS HERE
 * 
 * Zone.js should never be imported when using zoneless change detection:
 * - No import 'zone.js'
 * - No import 'zone.js/node' 
 * - No import 'zone.js/testing'
 */