package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.EntityEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EntityRepository extends MongoRepository<EntityEntity, String> {
    List<EntityEntity> findByUserIdAndPatternId(String userId, String patternId);
    long deleteAllByIdIn(List<String> ids);
}
