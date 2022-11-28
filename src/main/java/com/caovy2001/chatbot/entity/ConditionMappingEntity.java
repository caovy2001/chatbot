package com.caovy2001.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("condition_mapping")
public class ConditionMappingEntity extends BaseEntity {
    @Id
    private String id;

    @Field("condition_mapping_id")
    private String conditionMappingId;

    @Field("predict_type")
    private EPredictType predictType = EPredictType.INTENT;

    @Field("intent_id")
    private String intentId;

    @Field("keyword")
    private String keyword;

    @Field("next_node_ids")
    private List<String> next_node_ids;

    public  ConditionMappingEntity(){
        id = new ObjectId().toString();
    }

    public enum EPredictType {
        INTENT,
        KEYWORD
    }
}
