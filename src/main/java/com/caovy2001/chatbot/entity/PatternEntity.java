package com.caovy2001.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("pattern")
public class PatternEntity extends BaseEntity {
    @Id
    private String id;
    @Field("user_id")
    private String userId;

    @Field("content")
    private String content;

    @Field("intent_id")
    private String intentId;

    @Transient
    private String intentCode;

    @Field("uuid")
    private String uuid;

    @Transient
    private String intentName;

}
