package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.NodeEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConditionMapping extends MongoRepository<IntentEntity, String> {
    List<NodeEntity> findByUserId(String intentIds, String userId);
}