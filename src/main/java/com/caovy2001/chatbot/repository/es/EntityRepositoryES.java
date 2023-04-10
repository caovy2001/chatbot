package com.caovy2001.chatbot.repository.es;

import com.caovy2001.chatbot.entity.es.EntityEntityES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface EntityRepositoryES extends ElasticsearchRepository<EntityEntityES, String> {
    List<EntityEntityES> findByUserIdAndPatternId(String userId, String patternId);

    long deleteAllByIdIn(List<String> ids);
}
