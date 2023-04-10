package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.ScriptIntentMappingEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ScriptIntentMappingRepository extends MongoRepository<ScriptIntentMappingEntity, String> {
    void deleteByUserIdAndScriptId(String userId, String scriptId);
}
