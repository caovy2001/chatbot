package com.caovy2001.chatbot.utils;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;

@NoArgsConstructor
public class ExcelUtil {
    private static ExcelUtil instance;

    public static ExcelUtil getInstance() {
        if (instance == null) {
            instance = new ExcelUtil();
        }
        return instance;
    }

    public boolean isBlankLine(Row currentRow, int numColCheck) {
        for (int i = 0; i < numColCheck; i++) {
            if (currentRow.getCell(i) != null && StringUtils.isNotBlank(currentRow.getCell(i).getStringCellValue())) {
                return false;
            }
        }

        return true;
    }
}
