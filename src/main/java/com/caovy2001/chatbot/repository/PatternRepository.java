package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.PatternEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PatternRepository extends MongoRepository<PatternEntity, String> {
    List<PatternEntity> findByIntentIdInAndUserId(List<String> intentIds, String userId);

    List<PatternEntity> findByIntentIdInAndUserId(String intentIds, String userId);

    List<PatternEntity> findAllByUserId(String userId);

    PatternEntity findByIdAndUserId(String id, String userId);

    void deleteByIntentIdAndUserId(String intentId, String userId);

    long countByUserId(String userId);
    long countByIntentId(String intentId);

    List<PatternEntity> findByUserId(String userId, PageRequest pageRequest);
    List<PatternEntity> findByIntentId(String intentId, PageRequest pageRequest);
}
