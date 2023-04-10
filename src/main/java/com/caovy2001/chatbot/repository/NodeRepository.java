package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.NodeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NodeRepository extends MongoRepository<NodeEntity, String> {
    List<NodeEntity> findByScriptId(String scriptId);
}