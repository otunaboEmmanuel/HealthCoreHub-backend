package com.hc.hospitalservice.utils;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Helper {
    public static Long random() {
        return ThreadLocalRandom.current()
                .nextLong(1_000_000_000L, 10_000_000_000L); // 10 billion is exclusive
    }
    public static String generateRandomString() {
        return String.valueOf(
                ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L)
        );
    }
}
