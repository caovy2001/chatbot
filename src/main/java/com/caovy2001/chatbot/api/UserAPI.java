package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.ResponseBase;
import com.caovy2001.chatbot.service.user.IUserService;
import com.caovy2001.chatbot.service.user.command.CommandUserLogin;
import com.caovy2001.chatbot.service.user.command.CommandUserSignUp;
import com.caovy2001.chatbot.service.user.response.ResponseUserGetSecretKey;
import com.caovy2001.chatbot.service.user.response.ResponseUserLogin;
import com.caovy2001.chatbot.service.user.response.ResponseUserSignUp;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserAPI {
    @Autowired
    private IUserService userService;

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getById(@PathVariable String id) throws Exception{
        UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
            throw new Exception("auth_invalid");
        }

//        if (!userEntity.getId().equals(id)) {
//            throw new Exception("auth_invalid");
//        }

        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseUserLogin> login(@RequestBody CommandUserLogin command) {
        try {
            ResponseUserLogin responseUserLogin = userService.login(command);
            return ResponseEntity.ok(responseUserLogin);
        } catch (Exception e) {
            return ResponseEntity.ok(userService.returnException(ExceptionConstant.error_occur, ResponseUserLogin.class));
        }
    }

    @PostMapping("/sign_up")
    public ResponseEntity<ResponseUserSignUp> signUp(@RequestBody CommandUserSignUp commandUserSignUp) throws Exception {
        try {
            ResponseUserSignUp responseUserSignUp = userService.signUp(commandUserSignUp);
            return ResponseEntity.ok(responseUserSignUp);
        } catch (Exception e) {
            return ResponseEntity.ok(userService.returnException(ExceptionConstant.error_occur, ResponseUserSignUp.class));
        }
    }

    @GetMapping("/get_secret_key")
    public ResponseEntity<ResponseUserGetSecretKey> getSecretKey() {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            return ResponseEntity.ok(ResponseUserGetSecretKey.builder()
                            .userId(userEntity.getId())
                            .secretKey(userEntity.getSecretKey())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(userService.returnException(ExceptionConstant.error_occur, ResponseUserGetSecretKey.class));
        }
    }

    @PostMapping("/login_from_data_everywhere")
    public ResponseEntity<ResponseBase> loginFromDataEverywhere(@RequestBody CommandUserLogin command) {
        try {
            ResponseBase responseBase = userService.loginFromDataEverywhere(command);
            return ResponseEntity.ok(responseBase);
        } catch (Exception e) {
            return ResponseEntity.ok(userService.returnException(ExceptionConstant.error_occur, ResponseUserLogin.class));
        }
    }

    // Vul
    @GetMapping("/")
    public List<UserEntity> getList() {
        try {
            return userService.getAll_vul();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
