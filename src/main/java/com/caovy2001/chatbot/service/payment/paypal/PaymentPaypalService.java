package com.caovy2001.chatbot.service.payment.paypal;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.payment.paypal.command.CommandPaypalAuthorizePayment;
import com.caovy2001.chatbot.service.payment.paypal.command.CommandPaypalExecutePayment;
import com.caovy2001.chatbot.service.payment.paypal.response.PaymentPaypalResponse;
import com.caovy2001.chatbot.service.user.IUserService;
import com.caovy2001.chatbot.service.user.command.CommandGetListUser;
import com.caovy2001.chatbot.service.user.command.CommandUserUpdate;
import com.caovy2001.chatbot.service.user.enumeration.UserServicePack;
import com.caovy2001.chatbot.utils.JWTUtil;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class PaymentPaypalService extends BaseService implements IPaymentPaypalService {
    @Autowired
    private IUserService userService;

    @Override
    public PaymentPaypalResponse authorizePayment(CommandPaypalAuthorizePayment command) throws Exception {
        if (StringUtils.isBlank(command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        // Nếu userEntity.currentServicePack=PREMIUM thì không cho thanh toán nữa
        List<UserEntity> users = userService.getList(CommandGetListUser.builder()
                .id(command.getUserId())
                .build());
        if (CollectionUtils.isEmpty(users)) {
            throw new Exception(ExceptionConstant.User.user_not_found);
        }
        if (users.get(0).getCurrentServicePack() == UserServicePack.PREMIUM) {
            throw new Exception(ExceptionConstant.User.user_have_premium_already);
        }

        //region Tạo Payer
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");
        payer.setPayerInfo(new PayerInfo());
        //endregion

        //region Set redirect url
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(Constant.Paypal.cancelUrl);
        redirectUrls.setReturnUrl(Constant.Paypal.returnUrl);
        //endregion

        //region Create transaction
        Details details = new Details();
        Amount amount = new Amount();
        amount.setCurrency("USD");
        amount.setTotal(String.valueOf("299.99"));
        amount.setDetails(details);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("Test order 1");

        ItemList itemList = new ItemList();
        itemList.setItems(new ArrayList<>());
        Item item = new Item();
        item.setCurrency("USD");
        item.setName("Chatbot Premium");
        item.setPrice(String.valueOf("299.99"));
        item.setQuantity("1");
        itemList.getItems().add(item);
        transaction.setItemList(itemList);
        //endregion

        //region create request
        Payment requestPayment = new Payment();
        requestPayment.setTransactions(List.of(transaction));
        requestPayment.setRedirectUrls(redirectUrls);
        requestPayment.setPayer(payer);
        requestPayment.setIntent("authorize");

        APIContext apiContext = new APIContext(Constant.Paypal.CLIENT_ID, Constant.Paypal.CLIENT_SECRET, Constant.Paypal.MODE);
        Payment approvedPayment = requestPayment.create(apiContext);

        //endregion

        AtomicReference<String> approvalLinkAtomic = new AtomicReference<String>();
        approvedPayment.getLinks().stream().filter(l -> "approval_url".equalsIgnoreCase(l.getRel())).findFirst().ifPresent(l -> {
            approvalLinkAtomic.set(l.getHref());
        });
        return PaymentPaypalResponse.builder().approvalLink(approvalLinkAtomic.get()).build();
    }

    @Override
    public PaymentPaypalResponse executePayment(CommandPaypalExecutePayment command) throws Exception {
        if (StringUtils.isAnyBlank(command.getUserId(), command.getPaymentId(), command.getPayerID())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        // Execute payment
        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(command.getPayerID());
        Payment payment = new Payment().setId(command.getPaymentId());
        APIContext apiContext = new APIContext(Constant.Paypal.CLIENT_ID, Constant.Paypal.CLIENT_SECRET, Constant.Paypal.MODE);

        payment = payment.execute(apiContext, paymentExecution);
        if (CollectionUtils.isEmpty(payment.getTransactions())) {
            throw new Exception("payment_process_fail");
        }

        List<UserEntity> userEntities = userService.getList(CommandGetListUser.builder()
                .id(command.getUserId())
                .build());
        if (CollectionUtils.isEmpty(userEntities)) {
            throw new Exception(ExceptionConstant.User.user_not_found);
        }

        // Tạo lại token cho user
        UserEntity userEntityToRecreateToken = UserEntity.builder()
                .username(userEntities.get(0).getUsername())
                .fullname(userEntities.get(0).getFullname())
                .currentServicePack(UserServicePack.PREMIUM)
                .token(String.valueOf(System.currentTimeMillis()))
                .build();

        // Update userEntity.currentServicePack
        userService.update(CommandUserUpdate.builder()
                .userId(command.getUserId())
                .currentServicePack(UserServicePack.PREMIUM)
                .token(JWTUtil.generateToken(userEntityToRecreateToken))
                .build());

        return PaymentPaypalResponse.builder().build();
    }

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(@NonNull T commandGetListBase) {
        return null;
    }

    @Override
    protected <Entity extends BaseEntity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {

    }
}
