package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.MessageEntityHistoryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageEntityHistoryRepository extends MongoRepository<MessageEntityHistoryEntity, String> {
}
