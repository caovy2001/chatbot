package com.caovy2001.chatbot.entity;

import com.caovy2001.chatbot.service.entity_type.IEntityTypeService;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandGetListPattern;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
@Document("entity")
public class EntityEntity extends BaseEntity {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("value")
    private String value;

    @Field("pattern_id")
    private String patternId;

    @Transient
    private String patternUuid;

    @Transient
    @Setter(AccessLevel.PRIVATE)
    private PatternEntity pattern;

    @Field("entity_type_id")
    private String entityTypeId;

    @Transient
    private String entityTypeUuid;

    @Transient
    @Setter(AccessLevel.PRIVATE)
    private EntityTypeEntity entityType;

    @Field("start_position")
    private int startPosition;

    @Field("end_position")
    private int endPosition;

    @Field("created_date")
    @Builder.Default
    private long createdDate = System.currentTimeMillis();

    @Field("last_updated_date")
    @Builder.Default
    private long lastUpdatedDate = System.currentTimeMillis();


    //region Behavior

    //region Pattern
    @Deprecated
    public PatternEntity getPattern() {
        return this.pattern;
    }

    public PatternEntity getPattern(@NonNull IPatternService patternService) {
        return this.patternMapping(patternService);
    }

    public PatternEntity patternMapping(@NonNull IPatternService patternService) {
        if (this.pattern != null) {
            return this.pattern;
        }

        if (StringUtils.isAnyBlank(this.patternId, this.userId)) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], "patternId or userId is null");
            return null;
        }

        List<PatternEntity> patterns = patternService.getList(CommandGetListPattern.builder()
                .id(this.patternId)
                .userId(this.userId)
                .build(), PatternEntity.class);
        if (CollectionUtils.isNotEmpty(patterns)) {
            this.pattern = patterns.get(0);
        }

        return this.pattern;
    }

    public PatternEntity patternMapping(@NonNull Map<String, PatternEntity> patternById) {
        if (this.pattern != null) {
            return this.pattern;
        }

        if (StringUtils.isBlank(this.patternId)) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], "patternId null");
            return null;
        }
        if (patternById.isEmpty()) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], "patternById map is empty");
            return null;
        }

        this.pattern = patternById.get(this.patternId);
        return this.pattern;
    }
    //endregion

    //region Entity type
    public EntityTypeEntity getEntityType() {
        return this.entityType;
    }

    public EntityTypeEntity getEntityType(IEntityTypeService entityTypeService) {
        return this.entityTypeMapping(entityTypeService);
    }

    public EntityTypeEntity entityTypeMapping(@NonNull IEntityTypeService entityTypeService) {
        if (this.entityType != null) {
            return this.entityType;
        }

        if (StringUtils.isAnyBlank(this.entityTypeId, this.userId)) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], "entityTypeId or userId is null");
            return null;
        }

        List<EntityTypeEntity> entityTypes = entityTypeService.getList(CommandGetListEntityType.builder()
                .userId(this.userId)
                .id(this.entityTypeId)
                .build(), EntityTypeEntity.class);
        if (CollectionUtils.isNotEmpty(entityTypes)) {
            this.entityType = entityTypes.get(0);
        }

        return this.entityType;
    }

    public EntityTypeEntity entityTypeMapping(@NonNull Map<String, EntityTypeEntity> entityTypeById) {
        if (StringUtils.isBlank(this.entityTypeId)) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], "entityTypeId null");
            return null;
        }
        if (entityTypeById.isEmpty()) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], "entityTypeById map is empty");
            return null;
        }

        this.entityType = entityTypeById.get(this.entityTypeId);
        return this.entityType;
    }
    //endregion

    public boolean checkIsValid() {
        if (StringUtils.isAnyBlank(this.userId, this.value, this.getPatternId(), this.entityTypeId) ||
                this.startPosition < 0 ||
                this.endPosition < 0 ||
                this.startPosition < this.endPosition ||
                this.createdDate == 0 ||
                this.lastUpdatedDate == 0) {
            return false;
        }

        return true;
    }

    //endregion
}
