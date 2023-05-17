package com.caovy2001.chatbot.utils;

import com.caovy2001.chatbot.entity.UserEntity;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class JWTUtil {
    private static final String USER = "user";
    private static final String SECRET = "9e0efee1-8db7-48f6-b0c7-289d775660e4-9e0efee1-8db7-48f6-b0c7-289d775660e4";
    public static String generateToken(Object obj) {
        // Phần này bị xóa
//        String secret_vul = null;
//        if (obj instanceof UserEntity userEntity) {
//            userEntity.setToken(SECRET + "_" + userEntity.getToken());
//            secret_vul = userEntity.getToken();
//        }

        String token = null;
        try {
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
            builder.claim(USER, obj);
            JWTClaimsSet claimsSet = builder.build();
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            // Dòng này được thay thế bằng dòng kế tiếp nó
//            JWSSigner signer = new MACSigner(StringUtils.isNotBlank(secret_vul)? secret_vul.getBytes(): SECRET.getBytes());
            JWSSigner signer = new MACSigner(SECRET.getBytes()); // SECRET này không được chứa trong Payload của Token
            signedJWT.sign(signer);
            token = signedJWT.serialize();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return token;
    }
}
