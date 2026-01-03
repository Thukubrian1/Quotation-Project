// Enhanced payment.service.ts with correct WebSocket endpoints
import { Injectable, Inject, PLATFORM_ID, NgZone } from '@angular/core';
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
  eventType?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private baseUrl = 'http://localhost:8083/api/v1/payments';
  private wsUrl = 'http://localhost:8083/ws-payment';
  private isBrowser: boolean;

  private stompClient: any = null;
  private paymentStatusSubject = new BehaviorSubject<PaymentStatusMessage | null>(null);
  public paymentStatus$ = this.paymentStatusSubject.asObservable();
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  public connectionStatus$ = this.connectionStatusSubject.asObservable();

  private wsInitialized = false;
  private currentSubscription: any = null;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object,
    private ngZone: NgZone
  ) {
    this.isBrowser = isPlatformBrowser(platformId);

    if (this.isBrowser) {
      setTimeout(() => this.initializeWebSocketConnection(), 100);
    }
  }

  private getAuthHeaders(): HttpHeaders {
    let token = '';

    if (this.isBrowser && typeof localStorage !== 'undefined') {
      token = localStorage.getItem('jwt_token') || '';
    }

    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  private async initializeWebSocketConnection(): Promise<void> {
    if (!this.isBrowser || this.wsInitialized) {
      return;
    }

    try {
      console.log('Initializing WebSocket connection to:', this.wsUrl);

      const { Client } = await import('@stomp/stompjs');
      const SockJS = (await import('sockjs-client')).default;

      let authToken = '';
      if (typeof localStorage !== 'undefined') {
        authToken = localStorage.getItem('jwt_token') || '';
      }

      this.stompClient = new Client({
        webSocketFactory: () => new SockJS(this.wsUrl),
        connectHeaders: authToken ? {
          Authorization: `Bearer ${authToken}`
        } : {},
        debug: (str) => {
          console.log('STOMP:', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      this.stompClient.onConnect = (frame: any) => {
        console.log('✓ WebSocket connected successfully');
        this.ngZone.run(() => {
          this.connectionStatusSubject.next(true);
        });
        this.wsInitialized = true;
      };

      this.stompClient.onDisconnect = () => {
        console.log('✗ WebSocket disconnected');
        this.ngZone.run(() => {
          this.connectionStatusSubject.next(false);
        });
      };

      this.stompClient.onStompError = (frame: any) => {
        console.error('WebSocket STOMP error:', frame.headers['message']);
        console.error('Error details:', frame.body);
        this.ngZone.run(() => {
          this.connectionStatusSubject.next(false);
        });
      };

      this.stompClient.onWebSocketError = (error: any) => {
        console.error('WebSocket connection error:', error);
        this.ngZone.run(() => {
          this.connectionStatusSubject.next(false);
        });
      };

      this.stompClient.activate();

    } catch (error) {
      console.error('Failed to initialize WebSocket:', error);
      this.ngZone.run(() => {
        this.connectionStatusSubject.next(false);
      });
    }
  }

  // Subscribe to payment status updates - CORRECTED ENDPOINT
  subscribeToPaymentStatus(checkoutRequestId: string): void {
    if (!this.isBrowser) {
      console.log('WebSocket subscription skipped on server');
      return;
    }

    // Unsubscribe from any previous subscription
    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
      this.currentSubscription = null;
    }

    if (this.stompClient && this.stompClient.connected) {
      // CRITICAL: Match the backend endpoint exactly
      const destination = `/topic/payment/${checkoutRequestId}`;
      console.log(`Subscribing to: ${destination}`);

      this.currentSubscription = this.stompClient.subscribe(destination, (message: any) => {
        try {
          const statusUpdate: PaymentStatusMessage = JSON.parse(message.body);
          console.log('📨 Real-time update received:', statusUpdate);
          this.ngZone.run(() => {
            this.paymentStatusSubject.next(statusUpdate);
          });
        } catch (error) {
          console.error('Error parsing payment status update:', error);
        }
      });

      console.log('✓ Subscribed to payment updates');
    } else {
      console.warn('WebSocket not connected, queuing subscription...');
      // Wait for connection and retry
      const connectionCheck = setInterval(() => {
        if (this.stompClient && this.stompClient.connected) {
          clearInterval(connectionCheck);
          this.subscribeToPaymentStatus(checkoutRequestId);
        }
      }, 500);

      // Stop checking after 10 seconds
      setTimeout(() => clearInterval(connectionCheck), 10000);
    }
  }

  private reconnectWebSocket(): void {
    if (!this.isBrowser) return;

    if (this.stompClient && !this.stompClient.connected) {
      console.log('Attempting to reconnect WebSocket...');
      this.stompClient.activate();
    } else if (!this.wsInitialized) {
      this.initializeWebSocketConnection();
    }
  }

  unsubscribeFromPaymentStatus(): void {
    if (!this.isBrowser) return;

    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
      this.currentSubscription = null;
      console.log('Unsubscribed from payment updates');
    }
    this.ngZone.run(() => {
      this.paymentStatusSubject.next(null);
    });
  }

  testConnection(): Observable<any> {
    return this.http.get(`${this.baseUrl}/public-health`).pipe(
      map(response => response),
      catchError(this.handleError)
    );
  }

  testAuthenticatedConnection(): Observable<any> {
    return this.http.get(`${this.baseUrl}/mpesa/health`, {
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => response),
      catchError(this.handleError)
    );
  }

  testOAuth(): Observable<any> {
    return this.http.get(`${this.baseUrl}/test-oauth`, {
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => response),
      catchError(this.handleError)
    );
  }

  initiateSTKPush(request: PaymentRequest): Observable<PaymentResponse> {
    console.log('Initiating STK Push:', request);
    return this.http.post<any>(`${this.baseUrl}/mpesa/stk-push`, request, {
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => {
        console.log('STK Push response:', response);
        return response.data as PaymentResponse;
      }),
      catchError(this.handleError)
    );
  }

  checkPaymentStatus(checkoutRequestId: string): Observable<PaymentStatusResponse> {
    return this.http.get<any>(`${this.baseUrl}/status?checkoutRequestId=${checkoutRequestId}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => response.data as PaymentStatusResponse),
      catchError(this.handleError)
    );
  }

  validatePhoneNumber(phoneNumber: string): boolean {
    if (!phoneNumber || !phoneNumber.trim()) {
      return false;
    }

    const cleaned = phoneNumber.replace(/\D/g, '');

    return (
      (cleaned.startsWith('254') && cleaned.length === 12) ||
      (cleaned.startsWith('0') && cleaned.length === 10) ||
      (cleaned.length === 9 && (cleaned.startsWith('7') || cleaned.startsWith('1')))
    );
  }

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

  getStatusMessage(status: string, resultCode?: number): string {
    switch (status?.toUpperCase()) {
      case 'FORM':
        return 'Ready to process payment';
      case 'PROCESSING':
        return 'Initiating payment request...';
      case 'PENDING':
        return 'Payment request sent to your phone. Please check your M-Pesa and enter your PIN.';
      case 'COMPLETED':
        return 'Payment completed successfully!';
      case 'FAILED':
        if (resultCode) {
          return this.getDetailedFailureMessage(resultCode);
        }
        return 'Payment failed. Please try again.';
      case 'CANCELLED':
        return 'Payment was cancelled by user.';
      case 'EXPIRED':
        return 'Payment request expired. Please try again.';
      default:
        return 'Unknown payment status.';
    }
  }

  private getDetailedFailureMessage(resultCode: number): string {
    const messages: { [key: number]: string } = {
      0: 'Payment Completed Successfully',
      1: 'Insufficient funds in your M-Pesa account',
      17: 'Payment cancelled by user',
      1032: 'Payment cancelled by user',
      1036: 'Payment cancelled by user',
      1037: 'Waiting for user to complete payment',
      1012: 'Payment request timed out',
      2001: 'Wrong M-Pesa PIN entered',
      4001: 'Invalid merchant configuration',
      4909: 'Payment Cancelled - User Declined',

    };

    return messages[resultCode] || 'Payment failed. Please try again.';
  }

  private handleError = (error: HttpErrorResponse): Observable<never> => {
    let errorMessage = 'An unexpected error occurred';

    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.error?.debugMessage) {
      errorMessage = error.error.debugMessage;
    } else if (error.message) {
      errorMessage = error.message;
    } else if (error.status === 0) {
      errorMessage = 'Unable to connect to payment service. Please check your connection.';
    }

    console.error('Payment service error:', {
      status: error.status,
      message: errorMessage,
      error: error.error
    });

    return throwError(() => new Error(errorMessage));
  };

  disconnectWebSocket(): void {
    if (this.isBrowser && this.stompClient) {
      this.unsubscribeFromPaymentStatus();
      if (this.stompClient.connected) {
        this.stompClient.deactivate();
      }
      this.wsInitialized = false;
      this.ngZone.run(() => {
        this.connectionStatusSubject.next(false);
      });
    }
  }

  ngOnDestroy(): void {
    this.disconnectWebSocket();
  }
}