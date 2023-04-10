package com.caovy2001.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateFilter {
    private String fieldName;
    private Long fromDate;
    private Long toDate;
}
