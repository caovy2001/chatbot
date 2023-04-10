package com.caovy2001.chatbot.entity.es;

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

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(indexName = "entity")
public class EntityEntityES {
    @Id
    private String id;

    @Field(type = FieldType.Text, name = "user_id")
    private String userId;

    @Field(type = FieldType.Text, name = "value")
    private String value;

    @Field(type = FieldType.Text, name = "pattern_id")
    private String patternId;

    @Transient
    private String patternUuid;

    @Transient
    private PatternEntityES pattern;

    @Field(type = FieldType.Text, name = "entity_type_id")
    private String entityTypeId;

    @Transient
    private String entityTypeUuid;

    @Transient
    private EntityTypeEntityES entityType;

    @Field(type = FieldType.Integer, name = "start_position")
    private int startPosition;

    @Field(type = FieldType.Integer, name = "end_position")
    private int endPosition;
}
