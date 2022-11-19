package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserEntity, String> {

    Optional<UserEntity> findByUsernameAndPassword(String username, String password);

    Optional<UserEntity> findByUsername(String username);

    long countByUsername(String username);

    Optional<UserEntity> findByToken(String token);

    Optional<UserEntity> findBySecretKey(String secretKey);

}
