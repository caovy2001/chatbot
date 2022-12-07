package com.caovy2001.chatbot.service.user;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.repository.UserRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.user.command.CommandUserLogin;
import com.caovy2001.chatbot.service.user.command.CommandUserSignUp;
import com.caovy2001.chatbot.service.user.response.ResponseUserLogin;
import com.caovy2001.chatbot.service.user.response.ResponseUserSignUp;
import com.caovy2001.chatbot.utils.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService extends BaseService implements IUserService{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTUtil jwtUtil;

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

}
