// Enhanced payment.component.ts with WebSocket real-time updates
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

// Import all required interfaces
import {
  PaymentRequest,
  PaymentResponse,
  PaymentStatusResponse,
  PaymentStatusMessage
} from '../Services/paymentservice';

import { PaymentService } from '../Services/paymentservice';
import { AuthService } from '../Services/authservice';
import { NotificationService } from '../Services/notificationservice';

interface PaymentData {
  phone: string;
  amount: number | null;
  accountReference: string;
}

enum PaymentState {
  FORM = 'FORM',
  PROCESSING = 'PROCESSING',
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED'
}

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './payment.html',
  styleUrls: ['./payment.css']
})
export class Payment implements OnInit, OnDestroy {

  paymentData: PaymentData = {
    phone: '',
    amount: null,
    accountReference: 'ORDER-' + Date.now()
  };

  currentState = PaymentState.FORM;
  PaymentState = PaymentState;

  isLoading = false;
  showError = false;
  errorMessage = '';
  isAuthenticated = false;
  wsConnected = false;

  // Payment tracking
  checkoutRequestId = '';
  merchantRequestId = '';
  mpesaReceiptNumber = '';
  currentStatusMessage = '';

  // Subscriptions
  private paymentStatusSubscription?: Subscription;
  private wsConnectionSubscription?: Subscription;
  private fallbackStatusCheckSubscription?: Subscription;

  constructor(
    private router: Router,
    private paymentService: PaymentService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.checkAuthentication();
    this.setupWebSocketConnection();
    this.testConnection();
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  private checkAuthentication(): void {
    this.isAuthenticated = this.authService.isLoggedIn();
    if (!this.isAuthenticated) {
      this.notificationService.showError(
        'Authentication Required',
        'Please log in to make payments',
        0
      );
    }
  }

  private setupWebSocketConnection(): void {
    // Monitor WebSocket connection status
    this.wsConnectionSubscription = this.paymentService.connectionStatus$.subscribe(
      connected => {
        this.wsConnected = connected;
        if (connected) {
          console.log('WebSocket connected successfully');
          this.notificationService.showSuccess(
            'Real-time Updates',
            'Connected for instant payment updates',
            3000
          );
        } else {
          console.log('WebSocket disconnected');
          if (this.currentState === PaymentState.PENDING) {
            this.notificationService.showWarning(
              'Connection Issue',
              'Real-time updates may be delayed. We\'ll keep checking for you.',
              5000
            );
            this.startFallbackStatusCheck();
          }
        }
      }
    );

    // Monitor payment status updates
    this.paymentStatusSubscription = this.paymentService.paymentStatus$.subscribe(
      (statusUpdate: PaymentStatusMessage | null) => {
        if (statusUpdate && statusUpdate.checkoutRequestId === this.checkoutRequestId) {
          console.log('Received real-time payment update:', statusUpdate);
          this.handleRealTimeStatusUpdate(statusUpdate);
        }
      }
    );
  }

  private handleRealTimeStatusUpdate(statusUpdate: PaymentStatusMessage): void {
    this.currentStatusMessage = statusUpdate.statusMessage;
    this.mpesaReceiptNumber = statusUpdate.mpesaReceiptNumber || '';

    // Stop fallback polling since we got a real-time update
    this.stopFallbackStatusCheck();

    switch (statusUpdate.status) {
      case 'COMPLETED':
        this.currentState = PaymentState.COMPLETED;
        this.notificationService.showSuccess(
          'Payment Successful!',
          `KES ${statusUpdate.amount?.toLocaleString()} payment completed successfully`,
          8000
        );
        break;

      case 'FAILED':
        this.currentState = PaymentState.FAILED;
        this.notificationService.showError(
          'Payment Failed',
          statusUpdate.statusMessage || 'The M-Pesa payment was not completed',
          8000
        );
        this.showErrorMessage(statusUpdate.statusMessage || 'Payment failed');
        break;

      case 'CANCELLED':
        this.currentState = PaymentState.CANCELLED;
        this.notificationService.showWarning(
          'Payment Cancelled',
          'The M-Pesa payment was cancelled by the user',
          6000
        );
        break;

      case 'EXPIRED':
        this.currentState = PaymentState.EXPIRED;
        this.notificationService.showWarning(
          'Payment Expired',
          'Payment request timed out. Please try again.',
          6000
        );
        this.showErrorMessage('Payment request expired');
        break;

      case 'PENDING':
        // Still pending - show periodic reminders
        this.notificationService.showInfo(
          'Still Waiting',
          statusUpdate.statusMessage || 'Please complete the M-Pesa prompt on your phone',
          4000
        );
        break;

      default:
        console.log('Unknown payment status:', statusUpdate.status);
        break;
    }
  }

  testConnection(): void {
    this.paymentService.testConnection().subscribe({
      next: (response) => {
        console.log('Backend connection successful:', response);
        this.notificationService.showSuccess(
          'Connection Successful',
          'Payment service is ready',
          3000
        );

        if (this.isAuthenticated) {
          this.testAuthenticatedConnection();
        }
      },
      error: (error) => {
        console.warn('Backend connection failed:', error.message);
        this.notificationService.showError(
          'Connection Failed',
          'Unable to connect to payment service. Please check your connection.',
          0
        );
      }
    });
  }

  private testAuthenticatedConnection(): void {
    this.paymentService.testAuthenticatedConnection().subscribe({
      next: (response) => {
        console.log('Authenticated connection successful:', response);
        this.notificationService.showInfo(
          'Authentication Verified',
          'You can make payments',
          3000
        );
      },
      error: (error) => {
        console.warn('Authenticated connection failed:', error.message);
        if (error.message.includes('Authentication')) {
          this.isAuthenticated = false;
          this.notificationService.showError(
            'Authentication Expired',
            'Please log in again to make payments',
            0
          );
        }
      }
    });
  }

  onSubmit(): void {
    if (!this.isAuthenticated) {
      this.notificationService.showError(
        'Authentication Required',
        'Please log in to make payments',
        0
      );
      return;
    }

    if (!this.isValidPaymentData()) {
      this.notificationService.showError(
        'Invalid Data',
        'Please fill in all required fields correctly',
        5000
      );
      return;
    }

    if (!this.paymentService.validatePhoneNumber(this.paymentData.phone)) {
      this.notificationService.showError(
        'Invalid Phone Number',
        'Please enter a valid Kenyan phone number (e.g., 0741819799)',
        5000
      );
      return;
    }

    this.initiatePayment();
  }

  private initiatePayment(): void {
    this.currentState = PaymentState.PROCESSING;
    this.isLoading = true;
    this.clearError();

    this.notificationService.showInfo(
      'Processing Payment',
      'Sending payment request to M-Pesa...',
      0
    );

    const paymentRequest: PaymentRequest = {
      phoneNumber: this.paymentData.phone,
      amount: this.paymentData.amount!,
      accountReference: this.paymentData.accountReference,
      transactionDescription: `Payment of KES ${this.paymentData.amount} for ${this.paymentData.accountReference}`
    };

    console.log('Initiating payment:', paymentRequest);

    this.paymentService.initiateSTKPush(paymentRequest).subscribe({
      next: (response: PaymentResponse) => {
        console.log('STK Push successful:', response);
        this.handlePaymentInitiated(response);
      },
      error: (error) => {
        console.error('STK Push failed:', error);
        this.handlePaymentError(error.message);

        if (error.message.includes('Authentication')) {
          this.isAuthenticated = false;
        }
      }
    });
  }

  private handlePaymentInitiated(response: PaymentResponse): void {
    this.notificationService.dismissByType('info');

    this.checkoutRequestId = response.checkoutRequestId;
    this.merchantRequestId = response.merchantRequestId;
    this.currentState = PaymentState.PENDING;
    this.isLoading = false;
    this.currentStatusMessage = 'Payment request sent to your phone. Please check your M-Pesa and enter your PIN.';

    this.notificationService.showSuccess(
      'Payment Request Sent',
      `Check your phone (${this.formattedPhoneNumber}) for M-Pesa prompt`,
      8000
    );

    // Subscribe to real-time updates for this specific payment
    this.paymentService.subscribeToPaymentStatus(this.checkoutRequestId);

    // Also start fallback polling as backup
    this.startFallbackStatusCheck();
  }

  private handlePaymentError(errorMessage: string): void {
    this.currentState = PaymentState.FAILED;
    this.isLoading = false;

    this.notificationService.dismissByType('info');
    this.notificationService.showError(
      'Payment Failed',
      errorMessage || 'Failed to initiate payment. Please try again.',
      8000
    );

    this.showErrorMessage(errorMessage);
  }

  // Fallback status checking when WebSocket is not available
  private startFallbackStatusCheck(): void {
    this.stopFallbackStatusCheck(); // Clear any existing interval

    // Only start fallback if WebSocket is not connected
    if (!this.wsConnected && this.checkoutRequestId) {
      console.log('Starting fallback status checking...');

      let attempts = 0;
      const maxAttempts = 15; // 1.5 minutes

      const checkStatus = () => {
        attempts++;

        if (attempts > maxAttempts) {
          this.stopFallbackStatusCheck();
          this.currentState = PaymentState.EXPIRED;
          this.notificationService.showWarning(
            'Payment Verification Timed Out',
            'Please check your M-Pesa messages to confirm if payment was completed',
            0
          );
          return;
        }

        this.paymentService.checkPaymentStatus(this.checkoutRequestId).subscribe({
          next: (status: PaymentStatusResponse) => {
            console.log(`Fallback status check (${attempts}/${maxAttempts}):`, status);

            // Convert to PaymentStatusMessage format for consistent handling
            const statusUpdate: PaymentStatusMessage = {
              checkoutRequestId: status.checkoutRequestId,
              merchantRequestId: status.merchantRequestId,
              phoneNumber: status.phoneNumber,
              amount: status.amount,
              accountReference: status.accountReference,
              status: status.status,
              statusMessage: status.statusMessage,
              mpesaReceiptNumber: status.mpesaReceiptNumber,
              transactionDate: status.transactionDate,
              updatedAt: status.updatedAt,
              resultCode: status.resultCode,
              resultDescription: status.resultDescription
            };

            this.handleRealTimeStatusUpdate(statusUpdate);
          },
          error: (error) => {
            console.error('Fallback status check error:', error);
            if (error.message.includes('Authentication')) {
              this.isAuthenticated = false;
              this.stopFallbackStatusCheck();
              this.notificationService.showError(
                'Authentication Expired',
                'Please log in again to continue',
                0
              );
            }
          }
        });
      };

      // Check immediately, then every 6 seconds
      setTimeout(checkStatus, 1000);
      this.fallbackStatusCheckSubscription = new Subscription();
      const intervalId = setInterval(checkStatus, 6000);

      this.fallbackStatusCheckSubscription.add(() => {
        clearInterval(intervalId);
      });
    }
  }

  private stopFallbackStatusCheck(): void {
    if (this.fallbackStatusCheckSubscription) {
      this.fallbackStatusCheckSubscription.unsubscribe();
      this.fallbackStatusCheckSubscription = undefined;
      console.log('Stopped fallback status checking');
    }
  }

  private isValidPaymentData(): boolean {
    return !!(
      this.paymentData.phone?.trim() &&
      this.paymentData.amount &&
      this.paymentData.amount > 0 &&
      this.paymentData.accountReference?.trim()
    );
  }

  clearError(): void {
    this.showError = false;
    this.errorMessage = '';
    this.notificationService.dismissByType('error');
  }

  private showErrorMessage(message: string): void {
    this.errorMessage = message;
    this.showError = true;
  }

  get formattedPhoneNumber(): string {
    return this.paymentService.formatPhoneNumber(this.paymentData.phone);
  }

  get formattedAmount(): string {
    return this.paymentData.amount ? `KES ${this.paymentData.amount.toLocaleString()}` : '';
  }

  startNewPayment(): void {
    this.cleanup();
    this.checkAuthentication();
    this.currentState = PaymentState.FORM;
    this.paymentData = {
      phone: '',
      amount: null,
      accountReference: 'ORDER-' + Date.now()
    };
    this.checkoutRequestId = '';
    this.merchantRequestId = '';
    this.mpesaReceiptNumber = '';
    this.currentStatusMessage = '';
    this.clearError();

    this.notificationService.dismissAll();
    this.notificationService.showInfo(
      'New Payment',
      'Ready to process a new payment',
      3000
    );
  }

  private cleanup(): void {
    this.stopFallbackStatusCheck();

    if (this.paymentStatusSubscription) {
      this.paymentStatusSubscription.unsubscribe();
    }

    if (this.wsConnectionSubscription) {
      this.wsConnectionSubscription.unsubscribe();
    }

    // Unsubscribe from WebSocket updates
    this.paymentService.unsubscribeFromPaymentStatus();
  }

  // Template helper getters
  get isFormValid(): boolean {
    return this.isValidPaymentData() && this.isAuthenticated;
  }

  get isFormState(): boolean {
    return this.currentState === PaymentState.FORM;
  }

  get isProcessingState(): boolean {
    return this.currentState === PaymentState.PROCESSING;
  }

  get isPendingState(): boolean {
    return this.currentState === PaymentState.PENDING;
  }

  get isCompletedState(): boolean {
    return this.currentState === PaymentState.COMPLETED;
  }

  get isFailedState(): boolean {
    return this.currentState === PaymentState.FAILED;
  }

  get isCancelledState(): boolean {
    return this.currentState === PaymentState.CANCELLED;
  }

  get isExpiredState(): boolean {
    return this.currentState === PaymentState.EXPIRED;
  }

  // Get current status message for display
  get displayStatusMessage(): string {
    return this.currentStatusMessage || this.getDefaultStatusMessage();
  }

  private getDefaultStatusMessage(): string {
    return this.paymentService.getStatusMessage(this.currentState);
  }

  // Connection status helpers
  get connectionStatusText(): string {
    return this.wsConnected ? 'Real-time updates active' : 'Using fallback checking';
  }

  get connectionStatusClass(): string {
    return this.wsConnected ? 'text-green-400' : 'text-yellow-400';
  }
}
