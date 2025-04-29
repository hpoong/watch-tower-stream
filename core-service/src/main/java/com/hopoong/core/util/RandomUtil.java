package com.hopoong.core.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil {

    // userId
    public static long getRandomUserId() {
        return ThreadLocalRandom.current().nextLong(1, 11);
    }

    // Ip Address
    public static String getRandomIpAddress() {
        int randomThirdOctet = ThreadLocalRandom.current().nextInt(0, 256);
        return String.format("192.168.%d.204", randomThirdOctet);
    }

    // Server Name
    public static String getRandomServerName() {
        int number = ThreadLocalRandom.current().nextInt(0, 100);
        return String.format("Server-%02d", number);
    }

    // Time
    public static String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

}
