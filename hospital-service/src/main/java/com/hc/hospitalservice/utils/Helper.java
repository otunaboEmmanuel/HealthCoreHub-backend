package com.hc.hospitalservice.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
