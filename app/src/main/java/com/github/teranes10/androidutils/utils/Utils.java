package com.github.teranes10.androidutils.utils;

import android.graphics.Color;
import android.location.Location;
import android.text.Editable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.textfield.TextInputEditText;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class Utils {
    private static final String TAG = "Util";

    public static String getText(TextInputEditText editText) {
        Editable val = editText.getText();
        return val != null ? val.toString().trim() : "";
    }

    public static boolean isNullOrEmpty(String string) {
        return !(string != null && !string.trim().isEmpty());
    }

    public static boolean isNullOrEmpty(Supplier<String> supplier) {
        try {
            String string = supplier.get();
            return !(string != null && !string.trim().isEmpty());
        } catch (NullPointerException n) {
            return true;
        }
    }

    public static boolean equal(String string1, String string2) {
        if (isNullOrEmpty(string1) || isNullOrEmpty(string2)) {
            return false;
        } else {
            return string1.equals(string2);
        }
    }

    public static String getOrDefault(String val) {
        return !isNullOrEmpty(val) ? val : "";
    }

    public static Integer getOrDefault(Integer val) {
        return val != null ? val : 0;
    }

    public static Double getOrDefault(Double val) {
        return val != null ? val : 0;
    }

    public static Long getOrDefault(Long val) {
        return val != null ? val : 0;
    }

    public static Boolean getOrDefault(Boolean val) {
        return val != null ? val : false;
    }

    public static <T> T optional(Supplier<T> supplier, T val) {
        try {
            return supplier.get() != null ? supplier.get() : val;
        } catch (Exception n) {
            return val;
        }
    }

    public static <T> boolean isNullOrEqual(Supplier<T> supplier, T val) {
        try {
            return supplier.get().equals(val);
        } catch (NullPointerException n) {
            return true;
        }
    }

    public static <T> boolean equal(Supplier<T> supplier, Supplier<T> supplier2) {
        try {
            return supplier.get().equals(supplier2.get());
        } catch (NullPointerException n) {
            return false;
        }
    }

    public static int getRandomColor() {
        Random rnd = new Random();
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    }

    public static double toFixed(double val) {
        double finalVal = val;
        val = optional(() -> finalVal, 0.0);
        DecimalFormat df = new DecimalFormat("#.00");
        return Double.parseDouble(df.format(val));
    }

    public static double toFixed(double val, int decimals) {
        StringBuilder pattern = new StringBuilder("#.");
        for (int i = 0; i < decimals; i++) {
            pattern.append("0");
        }

        double finalVal = val;
        val = optional(() -> finalVal, 0.0);
        DecimalFormat df = new DecimalFormat(pattern.toString());
        return Double.parseDouble(df.format(val));
    }

    public static String currencyFormat(double money) {
        return "$" + toFixed(money);
    }

    public static String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static String validatePhoneNumber(String phoneNUmber, String code) {
        if (phoneNUmber == null) {
            return "";
        }

        phoneNUmber = phoneNUmber.trim();
        if (phoneNUmber.startsWith("0")) {
            return code + phoneNUmber.substring(1);
        }

        return phoneNUmber;
    }

    public static LocalDateTime toLocalDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    public static double computeDistance(Location from, Location to) {
        LatLng fromLatLng = new LatLng(from.getLatitude(), from.getLongitude());
        LatLng toLatLng = new LatLng(to.getLatitude(), to.getLongitude());

        return SphericalUtil.computeDistanceBetween(fromLatLng, toLatLng);
    }

    public static double computeDurationInSeconds(Location from, Location to) {
        LocalDateTime fromTime = toLocalDateTime(from.getTime());
        LocalDateTime toTime = toLocalDateTime(to.getTime());

        return Duration.between(fromTime, toTime).getSeconds();
    }

    public static CompletableFuture<Void> Delay(long millis) {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
