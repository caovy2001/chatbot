package com.caovy2001.chatbot.service.user.response;

import com.caovy2001.chatbot.service.ResponseBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseUserAdminUpdate extends ResponseBase {
    private String id;
    private String username;
    private String fullname;
    private String zaloGroupLink;
    private String googleMeetLink;
}
