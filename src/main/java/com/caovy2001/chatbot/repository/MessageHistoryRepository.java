package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.MessageHistoryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageHistoryRepository extends MongoRepository<MessageHistoryEntity, String> {
}
