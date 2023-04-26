package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.MessageHistoryGroupEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageHistoryGroupRepository extends MongoRepository<MessageHistoryGroupEntity, String> {
}
