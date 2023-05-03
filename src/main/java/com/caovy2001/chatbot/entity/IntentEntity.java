package com.caovy2001.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("intent")
public class IntentEntity extends BaseEntity{
    @Id
    private String id;

    @Field("code")
    @Indexed
    private String code;

    @Field("user_id")
    private String userId;

    @Field("name")
    private String name;

    @Field("created_date")
    @Builder.Default
    private long createdDate = System.currentTimeMillis();

    @Field("last_updated_date")
    @Builder.Default
    private long lastUpdatedDate = System.currentTimeMillis();

    @Transient
    private List<PatternEntity> patterns;

    @Transient
    private List<ConditionMappingEntity> conditionMapping;
}
