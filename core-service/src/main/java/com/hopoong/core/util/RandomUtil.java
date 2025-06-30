package com.hopoong.core.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
        int number = ThreadLocalRandom.current().nextInt(0, 5);
        return String.format("Server-%02d", number);
    }

    // Time
    public static LocalDateTime getCurrentTime() {
        ZonedDateTime seoulTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        ZonedDateTime utcTime = seoulTime.withZoneSameInstant(ZoneOffset.UTC);
        return utcTime.toLocalDateTime();
    }

}
