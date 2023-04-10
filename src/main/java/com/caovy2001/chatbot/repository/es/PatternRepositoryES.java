package com.caovy2001.chatbot.repository.es;

import com.caovy2001.chatbot.entity.es.PatternEntityES;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PatternRepositoryES extends ElasticsearchRepository<PatternEntityES, String> {
    List<PatternEntityES> findByIntentIdInAndUserId(List<String> intentIds, String userId);

    List<PatternEntityES> findByIntentIdInAndUserId(String intentIds, String userId);

    List<PatternEntityES> findAllByUserId(String userId);

    PatternEntityES findByIdAndUserId(String id, String userId);

    void deleteByIntentIdAndUserId(String intentId, String userId);

    long countByUserId(String userId);

    long countByIntentId(String intentId);

    List<PatternEntityES> findByUserId(String userId, PageRequest pageRequest);

    List<PatternEntityES> findByIntentId(String intentId, PageRequest pageRequest);
}
