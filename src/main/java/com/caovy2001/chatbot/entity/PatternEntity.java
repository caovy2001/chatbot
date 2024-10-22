package com.caovy2001.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
@Document("pattern")
public class PatternEntity extends BaseEntity {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("content")
    private String content;

    @Field("intent_id")
    private String intentId;

    @Transient
    private String intentCode;

    @Field("uuid")
    private String uuid;

    @Field("created_date")
    @Builder.Default
    private long createdDate = System.currentTimeMillis();

    @Field("last_updated_date")
    @Builder.Default
    private long lastCreatedDate = System.currentTimeMillis();

    @Transient
    private String intentName;

    @Transient
    private List<EntityEntity> entities;

    public boolean checkIsValid() {
        if (StringUtils.isAnyBlank(this.userId, this.content, this.intentId) ||
                this.createdDate == 0 ||
                this.lastCreatedDate == 0) {
            return false;
        }

        return true;
    }
}
