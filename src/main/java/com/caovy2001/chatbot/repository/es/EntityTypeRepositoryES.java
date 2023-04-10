package com.caovy2001.chatbot.repository.es;

import com.caovy2001.chatbot.entity.es.EntityTypeEntityES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface EntityTypeRepositoryES extends ElasticsearchRepository<EntityTypeEntityES, String> {

    List<EntityTypeEntityES> findByUserIdAndLowerCaseName(String userId, String toLowerCase);

    List<EntityTypeEntityES> findByUserIdAndLowerCaseNameIn(String userId, List<String> lowerCaseName);

    long deleteAllByIdIn(List<String> ids);
}
