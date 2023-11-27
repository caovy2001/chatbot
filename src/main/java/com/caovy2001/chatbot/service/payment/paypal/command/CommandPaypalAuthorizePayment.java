package com.caovy2001.chatbot.service.payment.paypal.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandPaypalAuthorizePayment {
    private String userId;
}
