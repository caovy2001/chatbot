package com.caovy2001.chatbot.repository.es;

import com.caovy2001.chatbot.entity.es.UserEntityES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserRepositoryES extends ElasticsearchRepository<UserEntityES, String> {

}
