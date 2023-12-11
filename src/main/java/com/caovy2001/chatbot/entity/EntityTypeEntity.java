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

import java.util.Arrays;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("entity_type")
public class EntityTypeEntity extends BaseEntity {
    @Id
    private String id;

    @Field("uuid")
    private String uuid;

    @Field("user_id")
    private String userId;

    @Field("name")
    private String name;

    @Field("lower_case_name")
    private String lowerCaseName; // để check trùng name

    @Field("searchable_name")
    private String searchableName;

    @Transient
    private List<EntityEntity> entities;

    @Field("in_message_history_group_list_title")
    private Boolean inMessageHistoryGroupListTitle = false;

    @Field("created_date")
    @Builder.Default
    private long createdDate = System.currentTimeMillis();

    @Field("last_updated_date")
    @Builder.Default
    private long lastUpdatedDate = System.currentTimeMillis();

    @Field("is_hided")
    @Builder.Default
    private Boolean isHided = false;
}
