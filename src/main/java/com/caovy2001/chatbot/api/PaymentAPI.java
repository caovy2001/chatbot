package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.payment.paypal.IPaymentPaypalService;
import com.caovy2001.chatbot.service.payment.paypal.command.CommandPaypalAuthorizePayment;
import com.caovy2001.chatbot.service.payment.paypal.command.CommandPaypalExecutePayment;
import com.caovy2001.chatbot.service.payment.paypal.response.PaymentPaypalResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentAPI {
    @Autowired
    private IPaymentPaypalService paymentServicePaypal;

    @PostMapping("/paypal/authorize_payment")
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    public ResponseEntity<?> authorizePayment() {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            CommandPaypalAuthorizePayment command = CommandPaypalAuthorizePayment.builder()
                    .userId(userEntity.getId())
                    .build();
            return ResponseEntity.ok(paymentServicePaypal.authorizePayment(command));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(paymentServicePaypal.returnException(e.getMessage(), PaymentPaypalResponse.class));
        }
    }

    @PostMapping("/paypal/execute_payment")
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    public ResponseEntity<?> executePayment(@RequestBody CommandPaypalExecutePayment command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUserId(userEntity.getId());
            return ResponseEntity.ok(paymentServicePaypal.executePayment(command));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(paymentServicePaypal.returnException(e.getMessage(), PaymentPaypalResponse.class));
        }
    }
}
