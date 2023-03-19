package com.caovy2001.chatbot.service.user;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.repository.UserRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.ResponseBase;
import com.caovy2001.chatbot.service.user.command.CommandUserLogin;
import com.caovy2001.chatbot.service.user.command.CommandUserSignUp;
import com.caovy2001.chatbot.service.user.response.ResponseUserLogin;
import com.caovy2001.chatbot.service.user.response.ResponseUserSignUp;
import com.caovy2001.chatbot.utils.JWTUtil;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class UserService extends BaseService implements IUserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private RestTemplate restTemplate;

    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("custom");

    @Override
    public ResponseUserLogin login(CommandUserLogin command) {
        UserEntity userEntity = userRepository.findByUsernameAndPassword(command.getUsername(), command.getPassword()).orElse(null);
        if (userEntity == null) {
            return returnException(ExceptionConstant.login_fail, ResponseUserLogin.class);
        }

        return ResponseUserLogin.builder()
                .userId(userEntity.getId())
                .username(userEntity.getUsername())
                .token(userEntity.getToken())
                .secretKey(userEntity.getSecretKey())
                .build();
    }

    @Override
    public UserEntity getById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public ResponseUserSignUp signUp(CommandUserSignUp commandUserSignUp) throws Exception {
        if (commandUserSignUp == null ||
                StringUtils.isAnyBlank(commandUserSignUp.getUsername(), commandUserSignUp.getPassword(), commandUserSignUp.getFullname())) {
            return this.returnException(ExceptionConstant.missing_param, ResponseUserSignUp.class);
        }

        // Check xem username này đã tồn tại hay chưa
        if (userRepository.countByUsername(commandUserSignUp.getUsername()) > 0) {
            return returnException(ExceptionConstant.username_exists, ResponseUserSignUp.class);
        }

        // Thực hiện đăng ký
        UserEntity userEntity = UserEntity.builder()
                .username(commandUserSignUp.getUsername())
                .fullname(commandUserSignUp.getFullname())
                .token(String.valueOf(System.currentTimeMillis()))
                .build();

        // Khởi tạo token
        userEntity.setToken(JWTUtil.generateToken(userEntity));

        userEntity.setPassword(commandUserSignUp.getPassword());
        userEntity.setSecretKey(UUID.randomUUID().toString().toUpperCase());

        // Lưu user
        UserEntity savedUserEntity = userRepository.insert(userEntity);

        return ResponseUserSignUp.builder()
                .userId(savedUserEntity.getId())
                .username(savedUserEntity.getUsername())
                .token(savedUserEntity.getToken())
                .secretKey(savedUserEntity.getSecretKey())
                .build();
    }

    @Override
    public UserEntity getBySecretKey(String secretKey) {
        return userRepository.findBySecretKey(secretKey).orElse(null);
    }

    @Override
    public ResponseBase loginFromDataEverywhere(@NonNull CommandUserLogin command) {
        if (StringUtils.isAnyBlank(command.getUsername(), command.getPassword())) {
            return returnException(ExceptionConstant.missing_param, ResponseBase.class);
        }

        UserEntity userEntity = userRepository.findByUsernameAndPassword(command.getUsername(), command.getPassword()).orElse(null);
        if (userEntity == null) {
            return returnException(ExceptionConstant.login_fail, ResponseUserLogin.class);
        }

        String url = resourceBundle.getString("data_everywhere.server") + "/api/user/log_in_from_third_party";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> status = new HashMap<>();
        status.put("http_status", "OK");
        body.put("secret_key", resourceBundle.getString("data_everywhere.secret_login_key"));
        body.put("status", status);
        body.put("user", ResponseUserLogin.builder()
                .username(userEntity.getUsername())
                .fullName(userEntity.getFullname())
                .token(userEntity.getToken())
                .build());

        restTemplate.postForLocation(url, new HttpEntity<>(body, headers));

        return ResponseUserLogin.builder()
                .userId(userEntity.getId())
                .username(userEntity.getUsername())
                .token(userEntity.getToken())
                .build();
    }

}
