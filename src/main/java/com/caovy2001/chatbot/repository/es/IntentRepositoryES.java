package com.caovy2001.chatbot.repository.es;

import com.caovy2001.chatbot.entity.es.IntentEntityES;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;

public interface IntentRepositoryES extends ElasticsearchRepository<IntentEntityES, String> {
    Optional<IntentEntityES> findByCodeAndUserId(String code, String userId);

    List<IntentEntityES> findByUserId(String userId);

    List<IntentEntityES> findByUserId(String userId, PageRequest pageRequest);

    long countByUserId(String userId);
}
