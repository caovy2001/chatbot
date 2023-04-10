package com.caovy2001.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("script_intent_mapping")
public class ScriptIntentMappingEntity extends BaseEntity {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("intent_id")
    private String intentId;

    @Field("script_id")
    private String scriptId;
}
