package com.hopoong.core.util;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil {

    // userId
    public static long getRandomUserId() {
        return ThreadLocalRandom.current().nextLong(1, 11);
    }

}
