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

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(indexName = "entity_type")
public class EntityTypeEntityES extends BaseEntity {
    @Id
    private String id;
    @Field(type = FieldType.Text, name = "uuid")
    private String uuid;

    @Field(type = FieldType.Text, name = "user_id")
    private String userId;

    @Field(type = FieldType.Text, name = "name")
    private String name;

    @Field(type = FieldType.Text, name = "lower_case_name")
    private String lowerCaseName; // để check trùng name

    @Field(type = FieldType.Text, name = "searchable_name")
    private String searchableName;

    @Transient
    private List<EntityEntityES> entities;
}
