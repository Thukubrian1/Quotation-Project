import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

interface PaymentData {
  phone: string;
  amount: number | null;
}

@Component({
  selector: 'app-payment',
  imports: [FormsModule,CommonModule],
  templateUrl: './payment.html',
  styleUrl: './payment.css'
})
export class Payment {

  paymentData: PaymentData = {
    phone: '',
    amount: null
  };

  isLoading = false;
  showError = false;
  errorMessage = '';

  constructor(private router: Router) {}

  onSubmit(): void {
    if (!this.isValidPaymentData()) {
      this.showErrorMessage('Please fill in all required fields');
      return;
    }

    this.isLoading = true;
    this.clearError();

    // Simulate payment processing
    this.processPayment();
  }

  private processPayment(): void {
    // Replace this with your actual payment service call
    setTimeout(() => {
      try {
        // Simulate payment processing logic
        if (this.paymentData.phone && this.paymentData.amount && this.paymentData.amount > 0) {
          // Success scenario
          console.log('Payment processed successfully:', this.paymentData);
          this.router.navigate(['/payment-success']);
        } else {
          throw new Error('Invalid payment data');
        }
      } catch (error) {
        this.showErrorMessage('Payment processing failed. Please try again.');
      } finally {
        this.isLoading = false;
      }
    }, 2000);
  }

  private isValidPaymentData(): boolean {
    return !!(
      this.paymentData.phone?.trim() &&
      this.paymentData.amount &&
      this.paymentData.amount > 0
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

  gotoLanding(): void {
    this.router.navigate(['/landing']);
  }

  // Getter for template validation
  get PaymentData() {
    return {
      valid: this.isValidPaymentData()
    };
  }

}
