import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import {
  GenericResponse,
  PaymentStatusResponse,
  PaymentRequest,
  PaymentResponse
} from '../Models/PaymentModel';
import { AuthService } from './authservice';

@Injectable({
  providedIn: 'root'
})
export class Paymentservice {
  private readonly BASE_URL = 'http://localhost:8081/api/v1/payments';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    if (token) {
      return new HttpHeaders({
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      });
    }
    return new HttpHeaders({
      'Content-Type': 'application/json'
    });
  }

  initiateSTKPush(paymentRequest: PaymentRequest): Observable<PaymentResponse> {
    const url = `${this.BASE_URL}/mpesa/stk-push`;
    const headers = this.getAuthHeaders();

    return this.http.post<GenericResponse<PaymentResponse>>(url, paymentRequest, { headers })
      .pipe(
        map(response => {
          if (response.status === 'SUCCESS' && response.data) {
            return response.data;
          }
          throw new Error(response.message || 'Failed to initiate payment');
        }),
        catchError(error => {
          console.error('STK Push Error:', error);
          let errorMessage = 'Payment initiation failed';

          if (error.status === 401) {
            errorMessage = 'Authentication required. Please log in again.';
            // Optionally redirect to login or clear invalid token
            this.authService.logout();
          } else if (error.error?.message) {
            errorMessage = error.error.message;
          } else if (error.message) {
            errorMessage = error.message;
          }

          return throwError(() => new Error(errorMessage));
        })
      );
  }

  checkPaymentStatus(checkoutRequestId: string): Observable<PaymentStatusResponse> {
    const url = `${this.BASE_URL}/status?checkoutRequestId=${checkoutRequestId}`;
    const headers = this.getAuthHeaders();

    return this.http.get<GenericResponse<PaymentStatusResponse>>(url, { headers })
      .pipe(
        map(response => {
          if (response.status === 'SUCCESS' && response.data) {
            return response.data;
          }
          throw new Error(response.message || 'Failed to get payment status');
        }),
        catchError(error => {
          console.error('Payment Status Error:', error);
          let errorMessage = 'Failed to check payment status';

          if (error.status === 401) {
            errorMessage = 'Authentication required. Please log in again.';
            this.authService.logout();
          } else if (error.error?.message) {
            errorMessage = error.error.message;
          } else if (error.message) {
            errorMessage = error.message;
          }

          return throwError(() => new Error(errorMessage));
        })
      );
  }

  testConnection(): Observable<string> {
    // Use the public health endpoint instead of the authenticated one
    const url = `${this.BASE_URL}/public-health`;

    return this.http.get<GenericResponse<string>>(url)
      .pipe(
        map(response => {
          if (response.status === 'SUCCESS' && response.data) {
            return response.data;
          }
          throw new Error(response.message || 'Health check failed');
        }),
        catchError(error => {
          console.error('Health Check Error:', error);
          let errorMessage = 'Connection test failed';

          if (error.error?.message) {
            errorMessage = error.error.message;
          } else if (error.message) {
            errorMessage = error.message;
          }

          return throwError(() => new Error(errorMessage));
        })
      );
  }

  // Alternative method to test authenticated connection
  testAuthenticatedConnection(): Observable<string> {
    const url = `${this.BASE_URL}/mpesa/health`;
    const headers = this.getAuthHeaders();

    return this.http.get<GenericResponse<string>>(url, { headers })
      .pipe(
        map(response => {
          if (response.status === 'SUCCESS' && response.data) {
            return response.data;
          }
          throw new Error(response.message || 'Authenticated health check failed');
        }),
        catchError(error => {
          console.error('Authenticated Health Check Error:', error);
          let errorMessage = 'Authenticated connection test failed';

          if (error.status === 401) {
            errorMessage = 'Authentication failed. Please log in.';
            this.authService.logout();
          } else if (error.error?.message) {
            errorMessage = error.error.message;
          } else if (error.message) {
            errorMessage = error.message;
          }

          return throwError(() => new Error(errorMessage));
        })
      );
  }

  // Utility method to format phone number for display
  formatPhoneNumber(phoneNumber: string): string {
    if (!phoneNumber) return '';

    const cleaned = phoneNumber.replace(/[^0-9]/g, '');

    if (cleaned.length === 12 && cleaned.startsWith('254')) {
      return `+${cleaned.substring(0, 3)} ${cleaned.substring(3, 6)} ${cleaned.substring(6, 9)} ${cleaned.substring(9)}`;
    } else if (cleaned.length === 10 && cleaned.startsWith('0')) {
      return `${cleaned.substring(0, 4)} ${cleaned.substring(4, 7)} ${cleaned.substring(7)}`;
    }

    return phoneNumber;
  }

  // Validate phone number format
  validatePhoneNumber(phoneNumber: string): boolean {
    if (!phoneNumber) return false;

    const cleaned = phoneNumber.replace(/[^0-9]/g, '');

    return (
      (cleaned.length === 10 && cleaned.startsWith('0')) ||
      (cleaned.length === 12 && cleaned.startsWith('254')) ||
      (cleaned.length === 9 && /^7\d{8}$/.test(cleaned))
    );
  }
}
