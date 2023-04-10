package com.caovy2001.chatbot.entity;

import com.caovy2001.chatbot.enumeration.EMessageHistoryFrom;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("message_history")
public class MessageHistoryEntity {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("script_id")
    private String scriptId;

    @Field("node_id")
    private String nodeId;

    @Field("session_id")
    private String sessionId;

    @Field("message")
    private String message;

    @Field("from")
    @Builder.Default
    private EMessageHistoryFrom from = EMessageHistoryFrom.BOT;

    @Field("entities")
    private List<org.bson.Document> entities; // Entity Entity

    @Field("created_date")
    private Long createdDate = System.currentTimeMillis();
}
