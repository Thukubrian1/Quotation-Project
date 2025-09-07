package com.paymentservice.paymentservice.Service;

import com.paymentservice.paymentservice.DTOs.PaymentRequest;
import com.paymentservice.paymentservice.DTOs.PaymentResponse;
import com.paymentservice.paymentservice.DTOs.StkCallback;

public interface MpesaService {

    PaymentResponse initiateSTKPush(PaymentRequest request);

    void handleCallback(StkCallback callback);
}
