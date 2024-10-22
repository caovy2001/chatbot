package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.EntityTypeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EntityTypeRepository extends MongoRepository<EntityTypeEntity, String> {

    List<EntityTypeEntity> findByUserIdAndLowerCaseNameIn(String userId, List<String> lowerCaseName);

    long deleteAllByIdIn(List<String> ids);
}
