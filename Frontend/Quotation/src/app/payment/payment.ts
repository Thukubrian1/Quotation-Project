// Enhanced payment.component.ts - Fixed notification blocking
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { takeWhile } from 'rxjs/operators';

import {
  PaymentRequest,
  PaymentResponse,
  PaymentStatusResponse,
  PaymentStatusMessage,
  PaymentService
} from '../Services/paymentservice';

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

  checkoutRequestId = '';
  merchantRequestId = '';
  mpesaReceiptNumber = '';
  currentStatusMessage = '';
  resultCode: number | undefined;

  private paymentStatusSubscription?: Subscription;
  private wsConnectionSubscription?: Subscription;
  private fallbackPollingSubscription?: Subscription;
  private isPaymentInProgress = false;
  private lastUpdateTime = 0;

  constructor(
    private paymentService: PaymentService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
    this.checkAuthentication();
    this.setupWebSocketConnection();
    this.testConnection();
    this.setupBeforeUnloadHandler();
  }

  ngOnDestroy(): void {
    this.cleanup();
    this.removeBeforeUnloadHandler();
  }

  private setupBeforeUnloadHandler(): void {
    window.addEventListener('beforeunload', this.handleBeforeUnload);
  }

  private removeBeforeUnloadHandler(): void {
    window.removeEventListener('beforeunload', this.handleBeforeUnload);
  }

  private handleBeforeUnload = (event: BeforeUnloadEvent): string | void => {
    if (this.isPaymentInProgress && this.currentState === PaymentState.PENDING) {
      const message = 'Payment is in progress. Are you sure you want to leave?';
      event.preventDefault();
      event.returnValue = message;
      return message;
    }
  };

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
    this.wsConnectionSubscription = this.paymentService.connectionStatus$.subscribe(
      connected => {
        this.wsConnected = connected;
        if (connected) {
          console.log('✅ WebSocket connected - Real-time updates enabled');
          if (this.currentState !== PaymentState.PENDING) {
            this.notificationService.showSuccess(
              'Connected',
              'Real-time payment updates enabled',
              3000
            );
          }
        } else {
          console.log('❌ WebSocket disconnected');
          if (this.currentState === PaymentState.PENDING) {
            console.log('⚠️ WebSocket down, relying on backend polling + fallback');
            this.startFallbackPolling();
          }
        }
      }
    );

    this.paymentStatusSubscription = this.paymentService.paymentStatus$.subscribe(
      (statusUpdate: PaymentStatusMessage | null) => {
        if (statusUpdate && statusUpdate.checkoutRequestId === this.checkoutRequestId) {
          console.log('📨 Real-time update received:', statusUpdate);
          this.lastUpdateTime = Date.now();
          this.stopFallbackPolling();
          this.handlePaymentStatusUpdate(statusUpdate);
        }
      }
    );
  }

  private handlePaymentStatusUpdate(statusUpdate: PaymentStatusMessage): void {
    this.currentStatusMessage = statusUpdate.statusMessage;
    this.mpesaReceiptNumber = statusUpdate.mpesaReceiptNumber || '';
    this.resultCode = statusUpdate.resultCode;

    console.log(`Payment ${statusUpdate.eventType || statusUpdate.status}:`, statusUpdate.statusMessage);

    const status = statusUpdate.status.toUpperCase();

    switch (status) {
      case 'PENDING':
        if (statusUpdate.eventType === 'INITIATED') {
          this.currentState = PaymentState.PENDING;
          this.isPaymentInProgress = true;
        }
        // Status updates silently - UI will reflect changes automatically
        break;

      case 'COMPLETED':
        this.handlePaymentSuccess(statusUpdate);
        break;

      case 'FAILED':
        this.handlePaymentFailure(statusUpdate);
        break;

      case 'CANCELLED':
        this.handlePaymentCancelled(statusUpdate);
        break;

      case 'EXPIRED':
        this.handlePaymentExpired(statusUpdate);
        break;

      default:
        console.warn('Unknown payment status:', statusUpdate.status);
        break;
    }
  }

  private handlePaymentSuccess(statusUpdate: PaymentStatusMessage): void {
    this.currentState = PaymentState.COMPLETED;
    this.isLoading = false;
    this.isPaymentInProgress = false;

    this.notificationService.dismissAll();
    this.notificationService.showSuccess(
      'Payment Successful!',
      `KES ${statusUpdate.amount?.toLocaleString()} paid successfully`,
      0
    );

    this.playNotificationSound('success');
  }

  private handlePaymentFailure(statusUpdate: PaymentStatusMessage): void {
    this.currentState = PaymentState.FAILED;
    this.isLoading = false;
    this.isPaymentInProgress = false;

    this.notificationService.dismissAll();
    this.notificationService.showError(
      'Payment Failed',
      statusUpdate.statusMessage,
      0
    );
    this.showErrorMessage(statusUpdate.statusMessage);

    this.playNotificationSound('error');
  }

  private handlePaymentCancelled(statusUpdate: PaymentStatusMessage): void {
    this.currentState = PaymentState.CANCELLED;
    this.isLoading = false;
    this.isPaymentInProgress = false;

    this.notificationService.dismissAll();
    this.notificationService.showWarning(
      'Payment Cancelled',
      'You cancelled the M-Pesa payment',
      0
    );
  }

  private handlePaymentExpired(statusUpdate: PaymentStatusMessage): void {
    this.currentState = PaymentState.EXPIRED;
    this.isLoading = false;
    this.isPaymentInProgress = false;

    this.notificationService.dismissAll();
    this.notificationService.showWarning(
      'Payment Expired',
      statusUpdate.statusMessage,
      0
    );
    this.showErrorMessage(statusUpdate.statusMessage);
  }

  private playNotificationSound(type: 'success' | 'error'): void {
    try {
      const audio = new Audio();
      audio.volume = 0.5;
    } catch (error) {
      // Silent fail
    }
  }

  testConnection(): void {
    this.paymentService.testConnection().subscribe({
      next: (response) => {
        console.log('✅ Backend connection successful:', response);
        if (this.isAuthenticated) {
          this.testAuthenticatedConnection();
        }
      },
      error: (error) => {
        console.error('❌ Backend connection failed:', error.message);
        this.notificationService.showError(
          'Connection Failed',
          'Unable to connect to payment service',
          0
        );
      }
    });
  }

  private testAuthenticatedConnection(): void {
    this.paymentService.testAuthenticatedConnection().subscribe({
      next: (response) => {
        console.log('✅ Authenticated connection successful:', response);
      },
      error: (error) => {
        console.error('❌ Authentication failed:', error.message);
        if (error.message.includes('Authentication') || error.message.includes('401')) {
          this.isAuthenticated = false;
          this.notificationService.showError(
            'Session Expired',
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
        'Please enter a valid Kenyan phone number',
        5000
      );
      return;
    }

    this.initiatePayment();
  }

  private initiatePayment(): void {
    this.currentState = PaymentState.PROCESSING;
    this.isLoading = true;
    this.isPaymentInProgress = true;
    this.clearError();
    this.lastUpdateTime = Date.now();

    // No notification during processing - UI loading state is sufficient

    const paymentRequest: PaymentRequest = {
      phoneNumber: this.paymentData.phone,
      amount: this.paymentData.amount!,
      accountReference: this.paymentData.accountReference,
      transactionDescription: `Payment for ${this.paymentData.accountReference}`
    };

    console.log('💳 Initiating payment:', {
      ...paymentRequest,
      phoneNumber: this.paymentService.formatPhoneNumber(paymentRequest.phoneNumber)
    });

    this.paymentService.initiateSTKPush(paymentRequest).subscribe({
      next: (response: PaymentResponse) => {
        console.log('✅ STK Push initiated:', response);
        this.handlePaymentInitiated(response);
      },
      error: (error) => {
        console.error('❌ STK Push failed:', error);
        this.handlePaymentError(error.message);
      }
    });
  }

  private handlePaymentInitiated(response: PaymentResponse): void {
    this.notificationService.dismissAll();

    this.checkoutRequestId = response.checkoutRequestId;
    this.merchantRequestId = response.merchantRequestId;
    this.currentState = PaymentState.PENDING;
    this.isLoading = false;

    console.log(`📡 Subscribing to payment updates for: ${this.checkoutRequestId}`);
    console.log(`⏱️ Backend will poll M-Pesa every 3 seconds for up to 2 minutes`);

    this.paymentService.subscribeToPaymentStatus(this.checkoutRequestId);

    setTimeout(() => {
      if (this.currentState === PaymentState.PENDING) {
        const timeSinceLastUpdate = Date.now() - this.lastUpdateTime;
        if (timeSinceLastUpdate > 15000) {
          console.log('⚠️ No updates for 15s, starting safety fallback');
          this.startFallbackPolling();
        }
      }
    }, 15000);

    // Single persistent notification - won't block status updates
    this.notificationService.showInfo(
      'Check Your Phone',
      `M-Pesa prompt sent to ${this.formattedPhoneNumber}. Enter your PIN to complete payment.`,
      0
    );
  }

  private handlePaymentError(errorMessage: string): void {
    this.currentState = PaymentState.FAILED;
    this.isLoading = false;
    this.isPaymentInProgress = false;

    this.notificationService.dismissAll();
    this.notificationService.showError(
      'Payment Failed',
      errorMessage || 'Failed to initiate payment',
      0
    );

    this.showErrorMessage(errorMessage);

    if (errorMessage.includes('Authentication') || errorMessage.includes('401')) {
      this.isAuthenticated = false;
    }
  }

  private startFallbackPolling(): void {
    this.stopFallbackPolling();

    if (!this.checkoutRequestId) return;

    console.log('🔄 Starting safety fallback polling (checks every 10s)...');

    let attempts = 0;
    const maxAttempts = 15;

    this.fallbackPollingSubscription = interval(5000)
      .pipe(takeWhile(() => attempts < maxAttempts && this.currentState === PaymentState.PENDING))
      .subscribe(() => {
        attempts++;

        const timeSinceLastUpdate = Date.now() - this.lastUpdateTime;
        if (timeSinceLastUpdate < 4000) {
          console.log('✅ Recent update received, skipping fallback poll');
          return;
        }

        console.log(`🔍 Safety fallback check ${attempts}/${maxAttempts}`);

        this.paymentService.checkPaymentStatus(this.checkoutRequestId).subscribe({
          next: (status: PaymentStatusResponse) => {
            console.log('📊 Fallback status:', status);

            if (status.status !== 'PENDING') {
              this.lastUpdateTime = Date.now();

              const statusMessage: PaymentStatusMessage = {
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

              this.handlePaymentStatusUpdate(statusMessage);
            }
          },
          error: (error) => {
            console.error('❌ Fallback status check error:', error);
            if (error.message.includes('Authentication')) {
              this.stopFallbackPolling();
              this.isAuthenticated = false;
              this.isPaymentInProgress = false;
              this.notificationService.showError(
                'Session Expired',
                'Please log in again',
                0
              );
            }
          }
        });
      });
  }

  private stopFallbackPolling(): void {
    if (this.fallbackPollingSubscription) {
      this.fallbackPollingSubscription.unsubscribe();
      this.fallbackPollingSubscription = undefined;
      console.log('⏹️ Stopped fallback polling');
    }
  }

  private isValidPaymentData(): boolean {
    return !!(
      this.paymentData.phone?.trim() &&
      this.paymentData.amount &&
      this.paymentData.amount > 0 &&
      this.paymentData.amount <= 70000 &&
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
    this.resultCode = undefined;
    this.isPaymentInProgress = false;
    this.lastUpdateTime = 0;
    this.clearError();

    this.notificationService.dismissAll();
    this.notificationService.showInfo(
      'New Payment',
      'Ready to process a new payment',
      3000
    );
  }

  private cleanup(): void {
    this.stopFallbackPolling();

    if (this.paymentStatusSubscription) {
      this.paymentStatusSubscription.unsubscribe();
    }

    this.paymentService.unsubscribeFromPaymentStatus();
  }

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

  get displayStatusMessage(): string {
    if (this.currentStatusMessage) {
      return this.currentStatusMessage;
    }
    return this.paymentService.getStatusMessage(this.currentState);
  }

  get connectionStatusText(): string {
    if (this.wsConnected) {
      return 'Real-time updates active';
    } else if (this.currentState === PaymentState.PENDING) {
      return 'Checking payment status...';
    } else {
      return 'Connecting...';
    }
  }

  get connectionStatusClass(): string {
    return this.wsConnected ? 'text-green-400' : 'text-yellow-400';
  }

  canDeactivate(): boolean {
    if (this.isPaymentInProgress && this.currentState === PaymentState.PENDING) {
      return confirm('Payment is in progress. Are you sure you want to leave this page?');
    }
    return true;
  }
}