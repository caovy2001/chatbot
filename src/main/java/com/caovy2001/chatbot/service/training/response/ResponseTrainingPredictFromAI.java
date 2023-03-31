package com.caovy2001.chatbot.service.training.response;

import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.service.ResponseBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseTrainingPredictFromAI extends ResponseBase {
    private String accuracy;
    private String intentCode;
    private String intentId;
    private List<EntityEntity> entities;
}
