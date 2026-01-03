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
}

export interface GenericResponse<T> {
  status: string;
  message: string;
  data?: T;
  debugMessage?: string;
}

