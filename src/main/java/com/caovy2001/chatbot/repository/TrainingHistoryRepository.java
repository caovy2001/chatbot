package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.TrainingHistoryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrainingHistoryRepository extends MongoRepository<TrainingHistoryEntity, String> {
    TrainingHistoryEntity findByIdAndUserId(String id, String userId);
}
