// Enhanced payment.service.ts with SSR-safe WebSocket support
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export interface PaymentRequest {
  phoneNumber: string;
  amount: number;
  accountReference: string;
  transactionDescription: string;
}

export interface PaymentResponse {
  merchantRequestId: string;
  checkoutRequestId: string;
  responseCode: string;
  message: string;
  status: string;
}

export interface PaymentStatusResponse {
  checkoutRequestId: string;
  merchantRequestId: string;
  phoneNumber: string;
  amount: number;
  accountReference: string;
  status: string;
  statusMessage: string;
  mpesaReceiptNumber?: string;
  transactionDate?: string;
  createdAt: string;
  updatedAt: string;
  resultCode?: number;
  resultDescription?: string;
}

export interface PaymentStatusMessage {
  checkoutRequestId: string;
  merchantRequestId: string;
  phoneNumber: string;
  amount: number;
  accountReference: string;
  status: string;
  statusMessage: string;
  mpesaReceiptNumber?: string;
  transactionDate?: string;
  updatedAt: string;
  resultCode?: number;
  resultDescription?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private baseUrl = 'http://localhost:8081/api/v1/payments';
  private wsUrl = 'http://localhost:8081/ws-payment';
  private isBrowser: boolean;

  private stompClient: any = null;
  private paymentStatusSubject = new BehaviorSubject<PaymentStatusMessage | null>(null);
  public paymentStatus$ = this.paymentStatusSubject.asObservable();
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  public connectionStatus$ = this.connectionStatusSubject.asObservable();

  private wsInitialized = false;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);

    // Only initialize WebSocket in browser after the component is ready
    if (this.isBrowser) {
      // Delay WebSocket initialization to ensure DOM is ready
      setTimeout(() => this.initializeWebSocketConnection(), 100);
    }
  }

  private getAuthHeaders(): HttpHeaders {
    let token = '';

    // Only access localStorage in browser
    if (this.isBrowser && typeof localStorage !== 'undefined') {
      token = localStorage.getItem('jwt_token') || '';
    }

    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  // Initialize WebSocket connection - SSR safe
  private async initializeWebSocketConnection(): Promise<void> {
    if (!this.isBrowser || this.wsInitialized) {
      return;
    }

    try {
      console.log('Initializing WebSocket connection...');

      // Dynamic imports to avoid SSR issues
      const { Client } = await import('@stomp/stompjs');
      const SockJS = (await import('sockjs-client')).default;

      let authToken = '';
      if (typeof localStorage !== 'undefined') {
        authToken = localStorage.getItem('jwt_token') || '';
      }

      this.stompClient = new Client({
        webSocketFactory: () => new SockJS(this.wsUrl),
        connectHeaders: {
          Authorization: authToken ? `Bearer ${authToken}` : ''
        },
        debug: (str) => {
          console.log('STOMP Debug:', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      this.stompClient.onConnect = (frame: any) => {
        console.log('WebSocket connected:', frame);
        this.connectionStatusSubject.next(true);
        this.wsInitialized = true;
      };

      this.stompClient.onDisconnect = () => {
        console.log('WebSocket disconnected');
        this.connectionStatusSubject.next(false);
      };

      this.stompClient.onStompError = (frame: any) => {
        console.error('WebSocket error:', frame);
        this.connectionStatusSubject.next(false);
      };

      // Activate the client
      this.stompClient.activate();

    } catch (error) {
      console.error('Failed to initialize WebSocket:', error);
      this.connectionStatusSubject.next(false);
    }
  }

  // Subscribe to payment status updates for a specific checkout request
  subscribeToPaymentStatus(checkoutRequestId: string): void {
    if (!this.isBrowser) {
      console.log('WebSocket subscription skipped on server');
      return;
    }

    if (this.stompClient && this.stompClient.connected) {
      const destination = `/topic/payment-status/${checkoutRequestId}`;
      console.log('Subscribing to payment status updates:', destination);

      this.stompClient.subscribe(destination, (message: any) => {
        try {
          const statusUpdate: PaymentStatusMessage = JSON.parse(message.body);
          console.log('Received payment status update:', statusUpdate);
          this.paymentStatusSubject.next(statusUpdate);
        } catch (error) {
          console.error('Error parsing payment status update:', error);
        }
      });
    } else {
      console.warn('WebSocket not connected, cannot subscribe to payment status');
      // Try to reconnect
      this.reconnectWebSocket();
    }
  }

  // Reconnect WebSocket if connection is lost
  private reconnectWebSocket(): void {
    if (!this.isBrowser) return;

    if (this.stompClient && !this.stompClient.connected) {
      console.log('Attempting to reconnect WebSocket...');
      this.stompClient.activate();
    } else if (!this.wsInitialized) {
      // Re-initialize if not done yet
      this.initializeWebSocketConnection();
    }
  }

  // Unsubscribe from payment status updates
  unsubscribeFromPaymentStatus(): void {
    if (!this.isBrowser) return;

    if (this.stompClient && this.stompClient.connected) {
      console.log('Clearing payment status subscription');
      this.paymentStatusSubject.next(null);
    }
  }

  // Test connection (public endpoint)
  testConnection(): Observable<any> {
    return this.http.get(`${this.baseUrl}/public-health`).pipe(
      map(response => response),
      catchError(this.handleError)
    );
  }

  // Test authenticated connection
  testAuthenticatedConnection(): Observable<any> {
    return this.http.get(`${this.baseUrl}/mpesa/health`, {
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => response),
      catchError(this.handleError)
    );
  }

  // Test OAuth endpoint
  testOAuth(): Observable<any> {
    return this.http.get(`${this.baseUrl}/test-oauth`, {
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => response),
      catchError(this.handleError)
    );
  }

  // Initiate STK Push
  initiateSTKPush(request: PaymentRequest): Observable<PaymentResponse> {
    return this.http.post<any>(`${this.baseUrl}/mpesa/stk-push`, request, {
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => response.data as PaymentResponse),
      catchError(this.handleError)
    );
  }

  // Check payment status (fallback method)
  checkPaymentStatus(checkoutRequestId: string): Observable<PaymentStatusResponse> {
    return this.http.get<any>(`${this.baseUrl}/status?checkoutRequestId=${checkoutRequestId}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => response.data as PaymentStatusResponse),
      catchError(this.handleError)
    );
  }

  // Phone number validation
  validatePhoneNumber(phoneNumber: string): boolean {
    if (!phoneNumber || !phoneNumber.trim()) {
      return false;
    }

    const cleaned = phoneNumber.replace(/\D/g, '');

    // Check various formats
    return (
      (cleaned.startsWith('254') && cleaned.length === 12) ||
      (cleaned.startsWith('0') && cleaned.length === 10) ||
      (cleaned.length === 9 && cleaned.startsWith('7'))
    );
  }

  // Format phone number for display
  formatPhoneNumber(phoneNumber: string): string {
    if (!phoneNumber) return '';

    const cleaned = phoneNumber.replace(/\D/g, '');

    if (cleaned.startsWith('254')) {
      return `+${cleaned}`;
    } else if (cleaned.startsWith('0')) {
      return phoneNumber;
    } else if (cleaned.length === 9) {
      return `0${cleaned}`;
    }

    return phoneNumber;
  }

  // Get human-readable status message
  getStatusMessage(status: string, resultCode?: number): string {
    switch (status?.toLowerCase()) {
      case 'pending':
        return 'Payment request sent to your phone. Please check your M-Pesa and enter your PIN.';
      case 'completed':
        return 'Payment completed successfully!';
      case 'failed':
        if (resultCode) {
          return this.getDetailedFailureMessage(resultCode);
        }
        return 'Payment failed. Please try again.';
      case 'cancelled':
        return 'Payment was cancelled by user.';
      case 'expired':
        return 'Payment request expired. Please try again.';
      default:
        return 'Unknown payment status.';
    }
  }

  private getDetailedFailureMessage(resultCode: number): string {
    switch (resultCode) {
      case 1:
        return 'Insufficient funds in your M-Pesa account.';
      case 1001:
        return 'Invalid phone number provided.';
      case 1019:
        return 'Invalid amount specified.';
      case 2001:
        return 'Wrong M-Pesa PIN entered.';
      case 1025:
        return 'Unable to process payment - account locked.';
      case 1026:
        return 'Account not active.';
      case 1027:
        return 'Not a registered M-Pesa user.';
      case 1031:
        return 'Transaction limit exceeded.';
      case 1033:
        return 'Would exceed daily transaction limit.';
      case 1034:
        return 'Would exceed monthly transaction limit.';
      case 1039:
        return 'M-Pesa service temporarily unavailable.';
      case 1040:
        return 'Insufficient balance for transaction fee.';
      case 2006:
        return 'Transaction declined by risk management.';
      case 9999:
        return 'Request timeout - please try again.';
      default:
        return 'Payment failed. Please try again.';
    }
  }

  private handleError = (error: HttpErrorResponse): Observable<never> => {
    let errorMessage = 'An unexpected error occurred';

    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    console.error('Payment service error:', error);
    return throwError(() => new Error(errorMessage));
  };

  // Cleanup method
  ngOnDestroy(): void {
    if (this.isBrowser && this.stompClient && this.stompClient.connected) {
      this.stompClient.deactivate();
    }
  }
}
