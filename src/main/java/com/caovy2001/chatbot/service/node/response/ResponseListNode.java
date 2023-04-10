package com.caovy2001.chatbot.service.node.response;

import com.caovy2001.chatbot.entity.NodeEntity;
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
public class ResponseListNode extends ResponseBase {
    List<NodeEntity> nodes;
}
