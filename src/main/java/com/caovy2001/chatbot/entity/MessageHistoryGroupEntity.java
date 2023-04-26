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

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("message_history_group")
public class MessageHistoryGroupEntity {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("script_id")
    private String scriptId;

    @Field("session_id")
    private String sessionId;

    @Field("created_date")
    private Long createdDate;

    @Transient
    private List<MessageEntityHistoryEntity> messageEntityHistories;
}
