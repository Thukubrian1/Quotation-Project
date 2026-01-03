import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Notifications, Notification } from './notifications/notifications';
import { NotificationService } from './Services/notificationservice';
import { Subject, takeUntil } from 'rxjs';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Notifications],
  template: `
    <!-- Global Notification Component with fixed positioning -->
    <div class="notification-overlay">
      <app-notification
        [notifications]="notifications"
        (dismissNotification)="onNotificationDismiss($event)">
      </app-notification>
    </div>

    <!-- Main Application Content -->
    <main class="app-main">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    :host {
      display: block;
      min-height: 100vh;
      position: relative;
    }

    .notification-overlay {
      position: fixed;
      top: 0;
      right: 0;
      z-index: 10000;
      pointer-events: none;
      width: 100%;
      height: 100%;
    }

    .app-main {
      position: relative;
      z-index: 1;
      min-height: 100vh;
    }

    /* Ensure notifications are properly positioned on mobile */
    @media (max-width: 640px) {
      .notification-overlay {
        padding: 1rem;
      }
    }

    /* Global notification styles */
    ::ng-deep app-notification .fixed {
      position: fixed !important;
      top: 1rem !important;
      right: 1rem !important;
      z-index: 10001 !important;
      max-width: 24rem !important;
      width: 100% !important;
    }

    /* Mobile responsive adjustments */
    @media (max-width: 640px) {
      ::ng-deep app-notification .fixed {
        top: 1rem !important;
        right: 1rem !important;
        left: 1rem !important;
        max-width: none !important;
        width: auto !important;
      }
    }

    /* Ensure notification content is readable */
    ::ng-deep app-notification .bg-white {
      background-color: white !important;
      color: #111827 !important;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04) !important;
      border: 1px solid rgba(0, 0, 0, 0.1) !important;
    }

    /* Progress bar visibility */
    ::ng-deep app-notification .bg-gray-200 {
      background-color: #E5E7EB !important;
    }

    /* Icon visibility improvements */
    ::ng-deep app-notification .text-green-600 {
      color: #059669 !important;
    }

    ::ng-deep app-notification .text-red-600 {
      color: #DC2626 !important;
    }

    ::ng-deep app-notification .text-yellow-600 {
      color: #D97706 !important;
    }

    ::ng-deep app-notification .text-blue-600 {
      color: #2563EB !important;
    }

    /* Icon background improvements */
    ::ng-deep app-notification .bg-green-100 {
      background-color: #DCFCE7 !important;
    }

    ::ng-deep app-notification .bg-red-100 {
      background-color: #FEE2E2 !important;
    }

    ::ng-deep app-notification .bg-yellow-100 {
      background-color: #FEF3C7 !important;
    }

    ::ng-deep app-notification .bg-blue-100 {
      background-color: #DBEAFE !important;
    }
  `]
})
export class App {

  protected readonly title = signal('Quotation');

  notifications: Notification[] = [];
  private destroy$ = new Subject<void>();

  constructor(private notificationService: NotificationService) { }

  ngOnInit(): void {
    // Subscribe to global notifications
    this.notificationService.getNotifications()
      .pipe(takeUntil(this.destroy$))
      .subscribe(notifications => {
        this.notifications = notifications;
        console.log('App notifications updated:', notifications.length);
      });

    // Test notification on app load (remove in production)
    // Test notification removed to improve startup UX
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onNotificationDismiss(notificationId: string): void {
    console.log('Dismissing notification:', notificationId);
    this.notificationService.dismiss(notificationId);
  }
}
