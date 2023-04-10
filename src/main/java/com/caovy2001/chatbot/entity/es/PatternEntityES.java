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

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(indexName = "pattern")
public class PatternEntityES extends BaseEntity {
    @Id
    private String id;

    @Field(type = FieldType.Text, name = "user_id")
    private String userId;

    @Field(type = FieldType.Text, name = "content")
    private String content;

    @Field(type = FieldType.Text, name = "intent_id")
    private String intentId;

    @Transient
    private String intentCode;

    @Field(type = FieldType.Text, name = "uuid")
    private String uuid;

    @Field(type = FieldType.Long, name = "created_date")
    @Builder.Default
    private long createdDate = System.currentTimeMillis();

    @Field(type = FieldType.Long, name = "last_updated_date")
    @Builder.Default
    private long lastCreatedDate = System.currentTimeMillis();

    @Transient
    private String intentName;

    @Transient
    private List<EntityEntityES> entities;
}
