package com.caovy2001.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("condition_mapping")
public class ConditionMappingEntity extends BaseEntity {
    @Id
    private String id;

    @Field("intent_id")
    private String intentId;

    @Field("next_node_ids")
    private List<String> nextNodeList;
}
