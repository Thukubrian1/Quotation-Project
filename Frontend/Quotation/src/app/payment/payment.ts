import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';

// Import all required interfaces from PaymentModel
import {
  PaymentRequest,
  PaymentResponse,
  PaymentStatusResponse
} from '../Models/PaymentModel';

import { Paymentservice } from '../Services/paymentservice';
import { AuthService } from '../Services/authservice';

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
    accountReference: 'ORDER-' + Date.now() // Auto-generate reference
  };

  currentState = PaymentState.FORM;
  PaymentState = PaymentState; // Make enum available to template

  isLoading = false;
  showError = false;
  errorMessage = '';
  isAuthenticated = false;

  // Payment tracking
  checkoutRequestId = '';
  merchantRequestId = '';
  mpesaReceiptNumber = '';
  statusCheckInterval?: Subscription;
  statusCheckAttempts = 0;
  maxStatusCheckAttempts = 20; // Check for 2 minutes (20 * 6 seconds)

  constructor(
    private router: Router,
    private paymentService: Paymentservice,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Check if user is authenticated
    this.checkAuthentication();

    // Test connection on component load
    this.testConnection();
  }

  ngOnDestroy(): void {
    this.stopStatusChecking();
  }

  private checkAuthentication(): void {
    this.isAuthenticated = this.authService.isLoggedIn();
    if (!this.isAuthenticated) {
      this.showErrorMessage('Please log in to make payments');
      // Optionally redirect to login page
      // this.router.navigate(['/login']);
    }
  }

  testConnection(): void {
    // First try public health check
    this.paymentService.testConnection().subscribe({
      next: (response) => {
        console.log('Backend connection successful:', response);

        // If user is authenticated, also test authenticated endpoint
        if (this.isAuthenticated) {
          this.testAuthenticatedConnection();
        }
      },
      error: (error) => {
        console.warn('Backend connection failed:', error.message);
        this.showErrorMessage('Unable to connect to payment service. Please check your connection.');
      }
    });
  }

  private testAuthenticatedConnection(): void {
    this.paymentService.testAuthenticatedConnection().subscribe({
      next: (response) => {
        console.log('Authenticated connection successful:', response);
      },
      error: (error) => {
        console.warn('Authenticated connection failed:', error.message);
        if (error.message.includes('Authentication')) {
          this.isAuthenticated = false;
          this.showErrorMessage('Authentication expired. Please log in again.');
        }
      }
    });
  }

  onSubmit(): void {
    // Check authentication before processing
    if (!this.isAuthenticated) {
      this.showErrorMessage('Please log in to make payments');
      return;
    }

    if (!this.isValidPaymentData()) {
      this.showErrorMessage('Please fill in all required fields correctly');
      return;
    }

    if (!this.paymentService.validatePhoneNumber(this.paymentData.phone)) {
      this.showErrorMessage('Please enter a valid Kenyan phone number (e.g., 0741819799)');
      return;
    }

    this.initiatePayment();
  }

  private initiatePayment(): void {
    this.currentState = PaymentState.PROCESSING;
    this.isLoading = true;
    this.clearError();

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

        // Check if it's an authentication error
        if (error.message.includes('Authentication')) {
          this.isAuthenticated = false;
        }
      }
    });
  }

  private handlePaymentInitiated(response: PaymentResponse): void {
    this.checkoutRequestId = response.checkoutRequestId;
    this.merchantRequestId = response.merchantRequestId;
    this.currentState = PaymentState.PENDING;
    this.isLoading = false;

    // Start checking payment status
    this.startStatusChecking();
  }

  private handlePaymentError(errorMessage: string): void {
    this.currentState = PaymentState.FAILED;
    this.isLoading = false;
    this.showErrorMessage(errorMessage);
  }

  private startStatusChecking(): void {
    this.statusCheckAttempts = 0;

    // Check status every 6 seconds
    this.statusCheckInterval = interval(6000).subscribe(() => {
      this.checkPaymentStatus();
    });

    // Also check immediately after 1 second
    setTimeout(() => this.checkPaymentStatus(), 1000);
  }

  private checkPaymentStatus(): void {
    if (this.statusCheckAttempts >= this.maxStatusCheckAttempts) {
      this.stopStatusChecking();
      this.currentState = PaymentState.EXPIRED;
      this.showErrorMessage('Payment verification timed out. Please check your M-Pesa messages.');
      return;
    }

    this.statusCheckAttempts++;
    console.log(`Checking payment status (attempt ${this.statusCheckAttempts}/${this.maxStatusCheckAttempts})`);

    this.paymentService.checkPaymentStatus(this.checkoutRequestId).subscribe({
      next: (status: PaymentStatusResponse) => {
        console.log('Payment status:', status);
        this.handleStatusUpdate(status);
      },
      error: (error) => {
        console.error('Status check error:', error);

        // Check if it's an authentication error
        if (error.message.includes('Authentication')) {
          this.isAuthenticated = false;
          this.stopStatusChecking();
          this.showErrorMessage('Authentication expired. Please log in again.');
          return;
        }

        // Continue checking unless it's a critical error
        if (this.statusCheckAttempts >= this.maxStatusCheckAttempts) {
          this.stopStatusChecking();
          this.showErrorMessage('Unable to verify payment status. Please check your M-Pesa messages.');
        }
      }
    });
  }

  private handleStatusUpdate(status: PaymentStatusResponse): void {
    switch (status.status) {
      case 'COMPLETED':
        this.stopStatusChecking();
        this.currentState = PaymentState.COMPLETED;
        this.mpesaReceiptNumber = status.mpesaReceiptNumber || '';
        break;

      case 'FAILED':
        this.stopStatusChecking();
        this.currentState = PaymentState.FAILED;
        this.showErrorMessage('Payment failed. Please try again.');
        break;

      case 'CANCELLED':
        this.stopStatusChecking();
        this.currentState = PaymentState.CANCELLED;
        this.showErrorMessage('Payment was cancelled.');
        break;

      case 'PENDING':
        // Continue checking
        break;

      default:
        console.log('Payment still pending...');
        break;
    }
  }

  private stopStatusChecking(): void {
    if (this.statusCheckInterval) {
      this.statusCheckInterval.unsubscribe();
      this.statusCheckInterval = undefined;
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
  }

  private showErrorMessage(message: string): void {
    this.errorMessage = message;
    this.showError = true;
  }

  // UI Helper methods
  get formattedPhoneNumber(): string {
    return this.paymentService.formatPhoneNumber(this.paymentData.phone);
  }

  get formattedAmount(): string {
    return this.paymentData.amount ? `KES ${this.paymentData.amount.toLocaleString()}` : '';
  }

  // Navigation methods
  startNewPayment(): void {
    this.checkAuthentication(); // Re-check auth status
    this.currentState = PaymentState.FORM;
    this.paymentData = {
      phone: '',
      amount: null,
      accountReference: 'ORDER-' + Date.now()
    };
    this.checkoutRequestId = '';
    this.merchantRequestId = '';
    this.mpesaReceiptNumber = '';
    this.clearError();
  }

  goHome(): void {
    this.router.navigate(['/']);
  }

  // Handle login redirect
  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  // Template helper getters
  get PaymentData() {
    return {
      valid: this.isValidPaymentData() && this.isAuthenticated
    };
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
}
