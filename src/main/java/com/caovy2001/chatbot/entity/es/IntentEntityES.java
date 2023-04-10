package com.caovy2001.chatbot.entity.es;

import com.caovy2001.chatbot.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(indexName = "intent")
public class IntentEntityES extends BaseEntity {
    @Id
    private String id;

    @Field(type = FieldType.Text, name = "code")
    @Indexed
    private String code;

    @Field(type = FieldType.Text, name = "user_id")
    private String userId;

    @Field(type = FieldType.Text, name = "name")
    private String name;

    @Field(type = FieldType.Long, name = "created_date")
    @Builder.Default
    private long createdDate = System.currentTimeMillis();

    @Field(type = FieldType.Long, name = "last_updated_date")
    @Builder.Default
    private long lastUpdatedDate = System.currentTimeMillis();

    @Transient
    private List<PatternEntityES> patterns;

    @Transient
    private List<ConditionMappingEntityES> conditionMapping;

}
