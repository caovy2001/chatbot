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

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("script")
public class ScriptEntity extends BaseEntity {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Field("ui_rendering")
    private Map<String, Object> uiRendering;

    @Field("wrong_message")
    private String wrongMessage;

    @Field("end_message")
    private String endMessage;

    @Field("last_updated_date")
    @Builder.Default
    private long lastUpdatedDate = System.currentTimeMillis();

    @Field("created_date")
    @Builder.Default
    private long createdDate = System.currentTimeMillis();

    @Transient
    private List<NodeEntity> nodes;
}
