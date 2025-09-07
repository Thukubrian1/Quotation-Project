package com.paymentservice.paymentservice.Service;

import com.paymentservice.paymentservice.DTOs.PaymentRequest;
import com.paymentservice.paymentservice.DTOs.PaymentResponse;
import com.paymentservice.paymentservice.DTOs.StkCallback;
import com.shared.sharedlib.Dtos.GenericResponse;

public interface MpesaService {

    GenericResponse  initiateSTKPush(PaymentRequest request);

    void handleCallback(StkCallback callback);
}
