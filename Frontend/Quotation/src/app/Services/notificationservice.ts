// notificationservice.ts - Enhanced with better persistence control
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Notification } from '../notifications/notifications';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notifications$ = new BehaviorSubject<Notification[]>([]);
  private notificationIdCounter = 0;

  constructor() {}

  // Get notifications observable
  getNotifications(): Observable<Notification[]> {
    return this.notifications$.asObservable();
  }

  // Add a notification with better default handling
  private addNotification(notification: Omit<Notification, 'id'>): string {
    const id = this.generateId();
    const fullNotification: Notification = {
      id,
      duration: 5000, // Default 5 seconds
      dismissible: true, // Default dismissible
      ...notification
    };

    const currentNotifications = this.notifications$.value;
    this.notifications$.next([...currentNotifications, fullNotification]);

    return id;
  }

  // Show success notification with better persistence for page transitions
  showSuccess(title: string, message: string, duration?: number): string {
    return this.addNotification({
      type: 'success',
      title,
      message,
      duration: duration !== undefined ? duration : 6000 // Slightly longer for success messages
    });
  }

  // Show error notification with better persistence for page transitions
  showError(title: string, message: string, duration?: number): string {
    return this.addNotification({
      type: 'error',
      title,
      message,
      duration: duration !== undefined ? duration : 8000 // Errors stay longer by default
    });
  }

  // Show warning notification
  showWarning(title: string, message: string, duration?: number): string {
    return this.addNotification({
      type: 'warning',
      title,
      message,
      duration: duration !== undefined ? duration : 7000 // Warnings stay a bit longer
    });
  }

  // Show info notification
  showInfo(title: string, message: string, duration?: number): string {
    return this.addNotification({
      type: 'info',
      title,
      message,
      duration: duration !== undefined ? duration : 5000
    });
  }

  // Show persistent notification (doesn't auto-dismiss)
  showPersistent(type: 'success' | 'error' | 'warning' | 'info', title: string, message: string): string {
    return this.addNotification({
      type,
      title,
      message,
      duration: 0, // Persistent
      dismissible: true
    });
  }

  // Show notification that persists across page transitions
  showNavigationPersistent(type: 'success' | 'error' | 'warning' | 'info', title: string, message: string, duration: number = 8000): string {
    return this.addNotification({
      type,
      title,
      message,
      duration: duration, // Long enough for page transitions
      dismissible: true
    });
  }

  // Dismiss a specific notification
  dismiss(id: string): void {
    const currentNotifications = this.notifications$.value;
    const updatedNotifications = currentNotifications.filter(n => n.id !== id);
    this.notifications$.next(updatedNotifications);
  }

  // Dismiss all notifications
  dismissAll(): void {
    this.notifications$.next([]);
  }

  // Dismiss all notifications of a specific type
  dismissByType(type: 'success' | 'error' | 'warning' | 'info'): void {
    const currentNotifications = this.notifications$.value;
    const updatedNotifications = currentNotifications.filter(n => n.type !== type);
    this.notifications$.next(updatedNotifications);
  }

  // Dismiss all notifications except persistent ones
  dismissNonPersistent(): void {
    const currentNotifications = this.notifications$.value;
    const updatedNotifications = currentNotifications.filter(n => n.duration === 0);
    this.notifications$.next(updatedNotifications);
  }

  // Get current notification count
  getNotificationCount(): number {
    return this.notifications$.value.length;
  }

  // Check if there are any notifications of a specific type
  hasNotificationType(type: 'success' | 'error' | 'warning' | 'info'): boolean {
    return this.notifications$.value.some(n => n.type === type);
  }

  // Helper methods for common login scenarios with navigation persistence
  showLoginSuccess(username?: string): string {
    const message = username
      ? `Welcome back, ${username}! You have been successfully logged in.`
      : 'You have been successfully logged in.';

    return this.showNavigationPersistent('success', 'Login Successful', message, 6000);
  }

  showLoginError(errorType: 'credentials' | 'connection' | 'timeout' | 'server' | 'account' | 'generic', details?: string): string {
    let title = 'Login Failed';
    let message = '';

    switch (errorType) {
      case 'credentials':
        message = 'Invalid email or password. Please check your credentials and try again.';
        break;
      case 'connection':
        message = 'Unable to connect to the server. Please check your internet connection and try again.';
        break;
      case 'timeout':
        message = 'The login request timed out. Please try again.';
        break;
      case 'server':
        message = 'Server error occurred. Please try again later.';
        break;
      case 'account':
        message = details || 'There is an issue with your account. Please contact support.';
        break;
      case 'generic':
      default:
        message = details || 'An unexpected error occurred. Please try again.';
        break;
    }

    return this.showNavigationPersistent('error', title, message, 10000);
  }

  showValidationError(field: string, error: string): string {
    return this.showError(
      'Validation Error',
      `${field}: ${error}`,
      5000
    );
  }

  showNetworkError(): string {
    return this.showNavigationPersistent(
      'error',
      'Network Error',
      'Please check your internet connection and try again.',
      8000
    );
  }

  // Payment-specific helper methods
  showPaymentSuccess(amount: string, receipt?: string): string {
    const message = receipt
      ? `Payment of ${amount} completed successfully. Receipt: ${receipt}`
      : `Payment of ${amount} completed successfully.`;

    return this.showNavigationPersistent('success', 'Payment Successful', message, 8000);
  }

  showPaymentError(message: string): string {
    return this.showNavigationPersistent('error', 'Payment Failed', message, 10000);
  }

  showPaymentPending(phoneNumber: string): string {
    return this.showNavigationPersistent(
      'info',
      'Payment Pending',
      `Check your phone (${phoneNumber}) for M-Pesa prompt`,
      10000
    );
  }

  // Route change helper - call this when navigating to important pages
  prepareForNavigation(): void {
    // Convert short-duration notifications to longer ones for page transitions
    const currentNotifications = this.notifications$.value;
    const updatedNotifications = currentNotifications.map(notification => {
      if (notification.duration && notification.duration > 0 && notification.duration < 6000) {
        return {
          ...notification,
          duration: 6000 // Extend duration for page transition
        };
      }
      return notification;
    });

    this.notifications$.next(updatedNotifications);
  }

  private generateId(): string {
    return `notification_${++this.notificationIdCounter}_${Date.now()}`;
  }
}
