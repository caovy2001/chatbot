package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.IntentEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IntentRepository extends MongoRepository<IntentEntity, String> {
    Optional<IntentEntity> findByCodeAndUserId(String code, String userId);

    List<IntentEntity> findByUserId(String userId);

    List<IntentEntity> findByUserId(String userId, PageRequest pageRequest);

    long countByUserId(String userId);
}
