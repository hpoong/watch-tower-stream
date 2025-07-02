package com.hopoong.core.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    /*
     * yyyy-MM-dd 기본
     */
    public static String getFormattedTimestamp(String timeFormatter) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                timeFormatter != null ? timeFormatter : "yyyy-MM-dd HH:mm:ss"
        );
        return now.format(formatter);
    }

    public static String getFormattedTimestamp() {
        return getFormattedTimestamp(null);
    }

    public static long toMillis(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
