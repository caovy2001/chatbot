package com.caovy2001.chatbot.constant;

public class Constant {
    public static final String fileDataPath = "src/main/resources/file_data/";
    public static class JedisPrefix {
        public static final String COLON = ":";
        public static final String userIdPrefix_ = "userIdPrefix_";
        public static final String scriptSessionId_ = "scriptSessionId_";
        public static final String scriptId_ = "scriptId_";
        public static final String secretKeyPrefix_ = "secretKeyPrefix_";

        public static class Pattern {
            public static final String importExcelSessionIdPrefix_ = "importExcelSessionIdPrefix_";
            public static final String exportExcelSessionIdPrefix_ = "exportExcelSessionIdPrefix_";
        }
    }

    public static class Jedis{
        public static final String exportingTrainingDataToExcelStatus = "exportingTrainingDataToExcelStatus";
        public static final String currentExportingTrainingDataToExcelSessionId = "currentExportingTrainingDataToExcelSessionId";
    }

    public static class Pattern {
        public static final String importExcelTemplateDownloadLink = "https://drive.google.com/u/4/uc?id=1Xwos9Y9E-xZywtau9nrbW3N9zOXpkDB9&export=download";
        public static final String exportExcelFileName = "Training_data";

    }

    public static class KafkaTopic {
        public static final String process_save_message_when_predict = "process_save_message_when_predict";
        public static final String process_indexing_intent_es = "process_indexing_intent_es";
        public static final String process_indexing_pattern_es = "process_indexing_pattern_es";
        public static final String process_cache_entity_type_mapping = "process_cache_entity_type_mapping";
        public static final String process_after_cud_intent_pattern_entity_entityType = "process_after_cud_intent_pattern_entity_entityType";
    }
}
