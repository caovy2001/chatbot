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
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("entity_type")
public class EntityTypeEntity extends BaseEntity {
    @Id
    private String id;

    @Field("uuid")
    private String uuid;

    @Field("user_id")
    private String userId;

    @Field("name")
    private String name;

    @Field("lower_case_name")
    private String lowerCaseName; // để check trùng name

    @Field("searchable_name")
    private String searchableName;
}
