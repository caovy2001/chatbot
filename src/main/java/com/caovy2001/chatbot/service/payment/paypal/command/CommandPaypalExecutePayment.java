package com.caovy2001.chatbot.service.payment.paypal.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandPaypalExecutePayment {
    private String userId;
    private String paymentId;
    private String PayerID;
}
