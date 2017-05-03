package com.github.checkstyle.ci.utils;

public final class EnvironmentUtil {
    private EnvironmentUtil() {
    }

    private static final String APP_NAME = "CSCI__";

    private static String getEnvironmentVariable(String name) {
        String result = System.getProperty(APP_NAME + name);

        if (result == null) {
            result = System.getenv(APP_NAME + name);
        }

        return result;
    }

    public static String getEnvironmentVariable(String name, String defaultValue) {
        String value = getEnvironmentVariable(name);

        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    public static int getEnvironmentVariable(String name, int defaultValue) {
        final int result;
        final String value = getEnvironmentVariable(name);

        if (value == null) {
            result = defaultValue;
        } else {
            result = Integer.parseInt(value);
        }

        return result;
    }
}
