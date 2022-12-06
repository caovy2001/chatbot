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

    @Field("ui_rendering")
    private Map<String, Object> uiRendering;

    @Field("wrong_message")
    private String wrongMessage;

    @Field("end_message")
    private String endMessage;

    @Transient
    private List<NodeEntity> nodes;


}
