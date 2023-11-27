package com.caovy2001.chatbot.service.payment.paypal;

import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.payment.paypal.command.CommandPaypalAuthorizePayment;
import com.caovy2001.chatbot.service.payment.paypal.command.CommandPaypalExecutePayment;
import com.caovy2001.chatbot.service.payment.paypal.response.PaymentPaypalResponse;

import java.util.List;

public interface IPaymentPaypalService extends IBaseService {
    PaymentPaypalResponse authorizePayment(CommandPaypalAuthorizePayment command) throws Exception;

    PaymentPaypalResponse executePayment(CommandPaypalExecutePayment command) throws Exception;
}
