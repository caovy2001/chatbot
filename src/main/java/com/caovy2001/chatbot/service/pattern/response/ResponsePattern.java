package com.caovy2001.chatbot.service.pattern.response;

import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.service.ResponseBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponsePattern extends ResponseBase {
    List<PatternEntity> patterns;
    PatternEntity pattern;
}
