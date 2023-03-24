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

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("entity")
public class EntityEntity {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("value")
    private String value;

    @Field("pattern_id")
    private String patternId;

    @Transient
    private String patternUuid;

    @Field("entity_type_id")
    private String entityTypeId;

    @Transient
    private String entityTypeUuid;

    @Field("start_position")
    private int startPosition;

    @Field("end_position")
    private int endPosition;
}
