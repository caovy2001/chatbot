package com.caovy2001.chatbot.repository;

import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.ScriptEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.script.response.ResponseScriptGetByUserId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ScriptRepository extends MongoRepository<ScriptEntity,String> {
    List<ScriptEntity> findByUserId(String userId);
    UserEntity findById(Optional<String> id);
}
