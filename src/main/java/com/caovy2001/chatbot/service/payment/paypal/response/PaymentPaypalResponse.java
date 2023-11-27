package com.caovy2001.chatbot.service.payment.paypal.response;

import com.caovy2001.chatbot.service.ResponseBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentPaypalResponse extends ResponseBase {
    private String approvalLink;
}
