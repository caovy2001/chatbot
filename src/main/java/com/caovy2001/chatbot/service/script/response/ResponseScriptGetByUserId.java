package com.caovy2001.chatbot.service.script.response;

import com.caovy2001.chatbot.entity.ScriptEntity;
import com.caovy2001.chatbot.service.ResponseBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseScriptGetByUserId extends ResponseBase {
    private List<ScriptEntity> scripts;
}
