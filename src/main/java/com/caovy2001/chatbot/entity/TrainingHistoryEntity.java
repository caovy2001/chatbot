package com.caovy2001.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("training_history")
public class TrainingHistoryEntity {
    @Id
    private String id;

    @Field("user_id")
    @Indexed
    private String userId;

    @Field("username")
    private String username;

    @Field("status")
    @Builder.Default
    private EStatus status = EStatus.TRAINING;

    public enum EStatus {
        TRAINING,
        SUCCESS,
        FAILURE
    }
}
