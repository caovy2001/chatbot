package com.caovy2001.chatbot.utils;

import org.apache.commons.lang3.StringUtils;

public class ChatbotStringUtils {
    public static String stripAccents(String str) {
        return StringUtils.stripAccents(str).replace("đ", "d");
    }
}
