package com.caovy2001.chatbot.service.pattern.response;

import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.service.ResponseBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ResponsePattern extends ResponseBase {
    List<PatternEntity> patterns;
    PatternEntity pattern;
    List<EntityEntity> entities;

}
