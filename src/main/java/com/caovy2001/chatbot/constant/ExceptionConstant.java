package com.caovy2001.chatbot.constant;

public class ExceptionConstant {
    public static final String missing_param = "missing_param";
    public static final String username_exists = "username_exists";
    public static final String login_fail = "login_fail";
    public static final String error_occur = "error_occur";
    public static final String item_not_found = "item_not_found";

    public static class User {
        public static final String user_not_found = "user_not_found";
        public static final String user_have_premium_already = "user_have_premium_already";
    }

    public static class Intent {
        public static final String intent_not_found = "intent_not_found";
    }
}
