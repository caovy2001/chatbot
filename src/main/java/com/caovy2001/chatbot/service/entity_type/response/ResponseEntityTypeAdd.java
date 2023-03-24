package com.caovy2001.chatbot.service.entity_type.response;


import com.caovy2001.chatbot.service.ResponseBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseEntityTypeAdd extends ResponseBase {
    private String id;
    private String name;
    private String userId;
}
