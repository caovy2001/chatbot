package com.caovy2001.chatbot.service.user;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.entity.es.UserEntityES;
import com.caovy2001.chatbot.repository.UserRepository;
import com.caovy2001.chatbot.repository.es.UserRepositoryES;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.ResponseBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.user.command.CommandGetListUser;
import com.caovy2001.chatbot.service.user.command.CommandUserLogin;
import com.caovy2001.chatbot.service.user.command.CommandUserSignUp;
import com.caovy2001.chatbot.service.user.response.ResponseUserLogin;
import com.caovy2001.chatbot.service.user.response.ResponseUserSignUp;
import com.caovy2001.chatbot.utils.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class UserService extends BaseService implements IUserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRepositoryES userRepositoryES;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MongoTemplate  mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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

        // Lưu ES
        UserEntityES userEntityES = objectMapper.readValue(objectMapper.writeValueAsString(userEntity), UserEntityES.class);
        UserEntityES savedUserEntityES = userRepositoryES.save(userEntityES);

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

    @Override
    public List<UserEntity> getList(CommandGetListUser command) {
        if (StringUtils.isBlank(command.getId()) && StringUtils.isBlank(command.getUsername()) && StringUtils.isBlank(command.getSecretKey())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return null;
        }

        List<UserEntity> users = mongoTemplate.find(query, UserEntity.class);
        if (CollectionUtils.isEmpty(users)) {
            return null;
        }

        return users;
    }

    private Query buildQueryGetList(CommandGetListUser command) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = new ArrayList<>();
        List<Criteria> andCriteriaList = new ArrayList<>();

        if (StringUtils.isNotBlank(command.getId())) {
            andCriteriaList.add(Criteria.where("id").is(command.getId()));
        }

        if (StringUtils.isNotBlank(command.getSecretKey())) {
            andCriteriaList.add(Criteria.where("secret_key").is(command.getSecretKey()));
        }

        if (StringUtils.isNotBlank(command.getUsername())) {
            andCriteriaList.add(Criteria.where("username").is(command.getUsername()));
        }

        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList);
        }
        if (CollectionUtils.isNotEmpty(andCriteriaList)) {
            criteria.andOperator(andCriteriaList);
        }

        query.addCriteria(criteria);
        if (CollectionUtils.isNotEmpty(command.getReturnFields())) {
            query.fields().include(Arrays.copyOf(command.getReturnFields().toArray(), command.getReturnFields().size(), String[].class));
        }
        return query;
    }

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(T commandGetListBase) {
        return null;
    }

    @Override
    protected <Entity extends BaseEntity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {

    }
}
