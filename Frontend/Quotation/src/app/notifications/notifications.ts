import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number; // in milliseconds, 0 means persistent
  dismissible?: boolean;
}

@Component({
  selector: 'app-notification',
  standalone: true,
  imports: [CommonModule],
  template: `
    <!-- Fixed positioning container with higher z-index -->
    <div class="fixed top-4 right-4 max-w-sm w-full space-y-3 pointer-events-none"
         style="z-index: 9999;">
      <div
        *ngFor="let notification of notifications; trackBy: trackByNotificationId"
        class="relative bg-white shadow-lg rounded-lg pointer-events-auto ring-1 ring-black ring-opacity-5 transform transition-all duration-300 ease-in-out opacity-100 translate-x-0"
        [class.animate-slide-in]="true">

        <div class="p-4">
          <div class="flex items-start">
            <!-- Icon -->
            <div class="flex-shrink-0">
              <!-- Success Icon -->
              <div *ngIf="notification.type === 'success'"
                   class="flex items-center justify-center w-8 h-8 bg-green-100 rounded-full">
                <svg class="h-5 w-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>

              <!-- Error Icon -->
              <div *ngIf="notification.type === 'error'"
                   class="flex items-center justify-center w-8 h-8 bg-red-100 rounded-full">
                <svg class="h-5 w-5 text-red-600" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </div>

              <!-- Warning Icon -->
              <div *ngIf="notification.type === 'warning'"
                   class="flex items-center justify-center w-8 h-8 bg-yellow-100 rounded-full">
                <svg class="h-5 w-5 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                </svg>
              </div>

              <!-- Info Icon -->
              <div *ngIf="notification.type === 'info'"
                   class="flex items-center justify-center w-8 h-8 bg-blue-100 rounded-full">
                <svg class="h-5 w-5 text-blue-600" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
                </svg>
              </div>
            </div>

            <!-- Content -->
            <div class="ml-3 w-0 flex-1 pt-0.5">
              <p class="text-sm font-medium text-gray-900" [innerHTML]="notification.title"></p>
              <p class="mt-1 text-sm text-gray-500 break-words" [innerHTML]="notification.message"></p>
            </div>

            <!-- Dismiss Button -->
            <div *ngIf="notification.dismissible !== false" class="ml-4 flex-shrink-0 flex">
              <button
                type="button"
                (click)="dismiss(notification.id)"
                class="bg-white rounded-md inline-flex text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors duration-200"
                aria-label="Close notification">
                <svg class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Progress Bar for auto-dismiss -->
          <div *ngIf="notification.duration && notification.duration > 0" class="mt-3 w-full bg-gray-200 rounded-full h-1.5">
            <div
              class="h-1.5 rounded-full transition-all linear"
              [ngClass]="{
                'bg-green-500': notification.type === 'success',
                'bg-red-500': notification.type === 'error',
                'bg-yellow-500': notification.type === 'warning',
                'bg-blue-500': notification.type === 'info'
              }"
              [style.width.%]="getProgressWidth(notification)"
              [style.transition-duration.ms]="notification.duration">
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Custom styles for animations -->
    <style>
      .animate-slide-in {
        animation: slideIn 0.3s ease-out;
      }

      @keyframes slideIn {
        from {
          transform: translateX(100%);
          opacity: 0;
        }
        to {
          transform: translateX(0);
          opacity: 1;
        }
      }

      /* Ensure notifications are always on top */
      .fixed {
        position: fixed !important;
      }

      /* Handle text overflow */
      .break-words {
        word-wrap: break-word;
        word-break: break-word;
      }

      /* Mobile responsiveness */
      @media (max-width: 640px) {
        .fixed.top-4.right-4 {
          top: 1rem;
          right: 1rem;
          left: 1rem;
          max-width: none;
          width: auto;
        }
      }
    </style>
  `
})
export class Notifications implements OnInit, OnDestroy {
  @Input() notifications: Notification[] = [];
  @Output() dismissNotification = new EventEmitter<string>();

  private timers: Map<string, any> = new Map();
  private progressTimers: Map<string, any> = new Map();
  private progressValues: Map<string, number> = new Map();

  ngOnInit(): void {
    // Set up auto-dismiss timers for existing notifications
    this.notifications.forEach(notification => {
      if (notification.duration && notification.duration > 0) {
        this.setupAutoDismiss(notification);
      }
    });
  }

  ngOnDestroy(): void {
    // Clear all timers
    this.timers.forEach(timer => clearTimeout(timer));
    this.progressTimers.forEach(timer => clearInterval(timer));
    this.timers.clear();
    this.progressTimers.clear();
    this.progressValues.clear();
  }

  ngOnChanges(): void {
    // Set up auto-dismiss for new notifications
    this.notifications.forEach(notification => {
      if (notification.duration && notification.duration > 0 && !this.timers.has(notification.id)) {
        this.setupAutoDismiss(notification);
      }
    });
  }

  trackByNotificationId(index: number, notification: Notification): string {
    return notification.id;
  }

  dismiss(id: string): void {
    this.dismissNotification.emit(id);
    this.clearTimers(id);
  }

  getProgressWidth(notification: Notification): number {
    return this.progressValues.get(notification.id) || 100;
  }

  private setupAutoDismiss(notification: Notification): void {
    if (!notification.duration || notification.duration <= 0) return;

    // Initialize progress
    this.progressValues.set(notification.id, 100);

    // Set up progress bar animation
    const progressInterval = 50; // Update every 50ms
    const totalSteps = notification.duration / progressInterval;
    let currentStep = 0;

    const progressTimer = setInterval(() => {
      currentStep++;
      const progressPercent = Math.max(0, 100 - (currentStep / totalSteps) * 100);
      this.progressValues.set(notification.id, progressPercent);

      if (currentStep >= totalSteps) {
        clearInterval(progressTimer);
        this.progressTimers.delete(notification.id);
      }
    }, progressInterval);

    this.progressTimers.set(notification.id, progressTimer);

    // Set up auto-dismiss timer
    const dismissTimer = setTimeout(() => {
      this.dismiss(notification.id);
    }, notification.duration);

    this.timers.set(notification.id, dismissTimer);
  }

  private clearTimers(id: string): void {
    const timer = this.timers.get(id);
    if (timer) {
      clearTimeout(timer);
      this.timers.delete(id);
    }

    const progressTimer = this.progressTimers.get(id);
    if (progressTimer) {
      clearInterval(progressTimer);
      this.progressTimers.delete(id);
    }

    this.progressValues.delete(id);
  }
}
