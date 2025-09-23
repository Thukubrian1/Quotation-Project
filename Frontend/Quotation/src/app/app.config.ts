import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, withFetch } from '@angular/common/http';
import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { AuthInterceptor } from './interceptors/AuthInterceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Using zoneless change detection - remove zone.js imports
    provideZonelessChangeDetection(),
    provideHttpClient(
      withInterceptorsFromDi(),
      withFetch() // Enable fetch API for better performance
    ),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    provideRouter(routes),
    provideClientHydration(withEventReplay()),
  ]
};
