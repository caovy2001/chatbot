package com.caovy2001.chatbot.entity.es;

import com.caovy2001.chatbot.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(indexName = "condition_mapping")
public class ConditionMappingEntityES extends BaseEntity {
    @Id
    private String id;

    @Field(type = FieldType.Text, name = "condition_mapping_id")
    private String conditionMappingId;

    @Field(type = FieldType.Text, name = "predict_type")
    private EPredictType predictType = EPredictType.INTENT;

    @Field(type = FieldType.Text, name = "intent_id")
    private String intentId;

    @Field(type = FieldType.Text, name = "keyword")
    private String keyword;

    @Field(type = FieldType.Auto, name = "next_node_ids")
    private List<String> next_node_ids;

    @Field(type = FieldType.Text, name = "entities")
    private List<EntityEntityES> entities;

    public ConditionMappingEntityES(){
        id = new ObjectId().toString();
    }

    public enum EPredictType {
        INTENT,
        KEYWORD
    }
}
