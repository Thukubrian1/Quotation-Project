package com.paymentservice.paymentservice.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StkCallback {
    @JsonProperty("Body")
    private StkCallbackBody body;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StkCallbackBody {
        @JsonProperty("stkCallback")
        private StkCallbackData stkCallback;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StkCallbackData {
        @JsonProperty("MerchantRequestID")
        private String merchantRequestID;

        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestID;

        @JsonProperty("ResultCode")
        private int resultCode;

        @JsonProperty("ResultDesc")
        private String resultDesc;

        @JsonProperty("CallbackMetadata")
        private CallbackMetadata callbackMetadata;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CallbackMetadata {
        @JsonProperty("Item")
        private List<CallbackItem> item;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CallbackItem {
        @JsonProperty("Name")
        private String name;

        @JsonProperty("Value")
        private Object value;
    }
}
