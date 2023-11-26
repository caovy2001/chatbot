package com.caovy2001.chatbot.service.user;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.entity.es.UserEntityES;
import com.caovy2001.chatbot.repository.UserRepository;
import com.caovy2001.chatbot.repository.es.UserRepositoryES;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.ResponseBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.intent.command.CommandGetListIntent;
import com.caovy2001.chatbot.service.intent.command.CommandIndexingIntentES;
import com.caovy2001.chatbot.service.intent.command.CommandIntentUpdate;
import com.caovy2001.chatbot.service.pattern.command.CommandProcessAfterCUDIntentPatternEntityEntityType;
import com.caovy2001.chatbot.service.user.command.CommandGetListUser;
import com.caovy2001.chatbot.service.user.command.CommandUserLogin;
import com.caovy2001.chatbot.service.user.command.CommandUserSignUp;
import com.caovy2001.chatbot.service.user.command.CommandUserUpdate;
import com.caovy2001.chatbot.service.user.enumeration.UserRole;
import com.caovy2001.chatbot.service.user.response.ResponseUserLogin;
import com.caovy2001.chatbot.service.user.response.ResponseUserSignUp;
import com.caovy2001.chatbot.utils.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("application");

    @Override
    public ResponseUserLogin login(CommandUserLogin command) {
        UserEntity userEntity = userRepository.findByUsernameAndPassword(command.getUsername(), command.getPassword()).orElse(null);
        if (userEntity == null) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.login_fail);
            return returnException(ExceptionConstant.login_fail, ResponseUserLogin.class);
        }

        return ResponseUserLogin.builder()
                .userId(userEntity.getId())
                .username(userEntity.getUsername())
                .token(userEntity.getToken())
                .secretKey(userEntity.getSecretKey())
                .role(userEntity.getRole())
                .currentServicePack(userEntity.getCurrentServicePack())
                .build();
    }

    @Override
    public UserEntity getById(String id) {
        UserEntity userEntity = userRepository.findById(id).orElse(null);
        if (userEntity == null) {
            return null;
        }

        // Vul
//        userEntity.setPassword(Base64.getEncoder().encodeToString(userEntity.getPassword().getBytes()));
        userEntity.setPassword(null);
        userEntity.setSecretKey(null);
        userEntity.setToken(null);
        return userEntity;
    }

    private String convertStringToHash(String input) {
        try {
            // Create a MessageDigest object with the desired algorithm
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convert the input string to bytes
            byte[] hashBytes = digest.digest(input.getBytes());

            // Convert the byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ResponseUserSignUp signUp(CommandUserSignUp commandUserSignUp) throws Exception {
        if (commandUserSignUp == null ||
                StringUtils.isAnyBlank(commandUserSignUp.getUsername(), commandUserSignUp.getPassword(), commandUserSignUp.getFullname())) {
            return this.returnException(ExceptionConstant.missing_param, ResponseUserSignUp.class);
        }

//        if (!this.validatePassword(commandUserSignUp.getPassword())) {
//            return returnException("wrong_password_format", ResponseUserSignUp.class);
//        }

        // Check xem username này đã tồn tại hay chưa
        if (userRepository.countByUsername(commandUserSignUp.getUsername()) > 0) {
            return returnException(ExceptionConstant.username_exists, ResponseUserSignUp.class);
        }

        // Thực hiện đăng ký
        Long createdDate = System.currentTimeMillis();
        UserEntity userEntity = UserEntity.builder()
                .username(commandUserSignUp.getUsername())
                .fullname(commandUserSignUp.getFullname())
                .token(String.valueOf(createdDate))
                .build();

        // Khởi tạo token
        userEntity.setToken(JWTUtil.generateToken(userEntity));

        userEntity.setPassword(commandUserSignUp.getPassword());
        userEntity.setSecretKey(UUID.randomUUID().toString().toUpperCase());
        userEntity.setCreatedDate(createdDate);

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

    private boolean validatePassword(String password) {
        // Check length
        if (password.length() < 8 || password.length() > 20) {
            return false;
        }

        // Chỉ chứa các kí tự chữ (kí tự chữ hoa, kí tự chữ thường, kí tự số)
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(password);
        if (m.find()) {
            return false;
        }

        // Có tối thiểu 1 kí tự chữ hoa, 1 kí tự chữ thường và 1 kí tự số
        p = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");
        m = p.matcher(password);
        if (!m.find()) {
            return false;
        }

        return true;
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
                .currentServicePack(userEntity.getCurrentServicePack())
                .build());

        restTemplate.postForLocation(url, new HttpEntity<>(body, headers));

        return ResponseUserLogin.builder()
                .userId(userEntity.getId())
                .username(userEntity.getUsername())
                .token(userEntity.getToken())
                .currentServicePack(userEntity.getCurrentServicePack())
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

    @Override
    public List<UserEntity> getAll_vul() {
        List<UserEntity> users = userRepository.findAll();
        users.forEach(u -> {
            u.setPassword(null);
            u.setToken(null);
            u.setSecretKey(null);
        });

        return users;
    }

    private Query buildQueryGetList(CommandGetListUser command) {
        Query query = new Query();
        Criteria criteria = new Criteria();
//        List<Criteria> orCriteriaList = new ArrayList<>();
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

        if (StringUtils.isNotBlank(command.getKeyword())) {
            List<Criteria> keywordOrCriteriaList = new ArrayList<>();
            keywordOrCriteriaList.add(Criteria.where("username").regex(".*" + command.getKeyword() + ".*", "i"));
            keywordOrCriteriaList.add(Criteria.where("fullname").regex(".*" + command.getKeyword() + ".*", "i"));
            keywordOrCriteriaList.add(Criteria.where("username").regex(".*" + command.getKeyword() + ".*", "i"));
            keywordOrCriteriaList.add(Criteria.where("id").regex(".*" + command.getKeyword() + ".*", "i"));
            Criteria keywordCriteria = new Criteria();
            keywordCriteria.orOperator(keywordOrCriteriaList);
            andCriteriaList.add(keywordCriteria);
        }

        if (command.getRole() != null) {
            if (command.getRole() == UserRole.USER) {
                List<Criteria> roleOrCriteriaList = new ArrayList<>();
                roleOrCriteriaList.add(Criteria.where("role").is(UserRole.USER));
                roleOrCriteriaList.add(Criteria.where("role").is(null));
                Criteria keywordCriteria = new Criteria();
                keywordCriteria.orOperator(roleOrCriteriaList);
                andCriteriaList.add(keywordCriteria);
            } else if (command.getRole() == UserRole.ADMIN) {
                andCriteriaList.add(Criteria.where("role").is(UserRole.ADMIN));
            }
        }

//        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
//            criteria.orOperator(orCriteriaList);
//        }
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
    protected <T extends CommandGetListBase> Query buildQueryGetList(@NonNull T commandGetListBase) {
        return this.buildQueryGetList((CommandGetListUser) commandGetListBase);
    }

    @Override
    protected <Entity extends BaseEntity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {

    }

    @Override
    public <Entity extends BaseEntity, CommandUpdate extends CommandUpdateBase> Entity update(CommandUpdate commandUpdateBase) throws Exception {
        CommandUserUpdate command = (CommandUserUpdate) commandUpdateBase;

        if (StringUtils.isBlank(command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        List<UserEntity> users = this.getList(CommandGetListUser.builder()
                .id(command.getUserId())
                .userId(command.getUserId())
                .build(), UserEntity.class);
        if (CollectionUtils.isEmpty(users)) {
            throw new Exception("user_null");
        }

        UserEntity user = users.get(0);
        if (StringUtils.isNotBlank(command.getZaloGroupLink())) {
            user.setZaloGroupLink(command.getZaloGroupLink());
        }
        if (StringUtils.isNotBlank(command.getGoogleMeetLink())) {
            user.setGoogleMeetLink(command.getGoogleMeetLink());
        }
        UserEntity updatedUser = userRepository.save(user);
        return (Entity) updatedUser;
    }
}
