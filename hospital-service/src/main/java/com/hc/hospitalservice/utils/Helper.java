package com.hc.hospitalservice.utils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;


public class Helper {
    private static final AtomicLong counter = new AtomicLong(0);
    public static Long random() {
        return ThreadLocalRandom.current()
                .nextLong(1_000_000_000L, 10_000_000_000L); // 10 billion is exclusive
    }
    public static String generateRandomString() {
        // Combine: timestamp (ms) + counter + random = virtually no collisions
        long timestamp = System.currentTimeMillis() % 1000000; // 6 digits
        long count = counter.incrementAndGet() % 100; // 2 digits
        int random = ThreadLocalRandom.current().nextInt(10, 100); // 2 digits

        // Format: TTTTTTCCR (10 digits total)
        return String.format("%06d%02d%02d", timestamp, count, random);
    }


}
