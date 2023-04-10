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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("node")
public class NodeEntity extends BaseEntity {
    @Id
    private String id;

    @Field("node_id")
    private String nodeId;

    @Field("message")
    private String message;

    @Field("script_id")
    private String scriptId;

    @Field("condition_mappings")
    private List<ConditionMappingEntity> conditionMappings;

    @Field("position")
    private List<Double> position;

    @Field("is_first_node")
    @Builder.Default
    private Boolean isFirstNode = false;
}
