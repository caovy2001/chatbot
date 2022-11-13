package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.user.IUserService;
import com.caovy2001.chatbot.service.user.command.CommandUserLogin;
import com.caovy2001.chatbot.service.user.command.CommandUserSignUp;
import com.caovy2001.chatbot.service.user.response.ResponseUserLogin;
import com.caovy2001.chatbot.service.user.response.ResponseUserSignUp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserAPI {
    @Autowired
    private IUserService userService;

    @Autowired
    private IBaseService baseService;

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getById(@PathVariable String id) {
        UserEntity userEntity = userService.getById(id);
        return ResponseEntity.ok(userEntity);
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseUserLogin> login(@RequestBody CommandUserLogin command) {
        try {
            ResponseUserLogin responseUserLogin = userService.login(command);
            return ResponseEntity.ok(responseUserLogin);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(ExceptionConstant.error_occur, ResponseUserLogin.class));
        }
    }

        @PostMapping("/sign_up")
    public ResponseEntity<ResponseUserSignUp> signUp(@RequestBody CommandUserSignUp commandUserSignUp) throws Exception {
        try {
            ResponseUserSignUp responseUserSignUp = userService.signUp(commandUserSignUp);
            return ResponseEntity.ok(responseUserSignUp);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(ExceptionConstant.error_occur, ResponseUserSignUp.class));
        }
    }
}
