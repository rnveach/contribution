package com.github.checkstyle.ci.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class LogManager {
    private LogManager() {
    }

    public static final int MAX_MESSAGES = EnvironmentUtil.getEnvironmentVariable(
            "MAX_LOG_MESSAGES", 500);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

    private static final List<LogMessage> LOG = new ArrayList<LogMessage>(MAX_MESSAGES + 2);

    // ///////////////////////////////////////////////////////////////////////////////////////

    private static void logMessage(String message, boolean error) {
        LOG.add(new LogMessage(message, error));

        while (LOG.size() > MAX_MESSAGES) {
            LOG.remove(0);
        }
    }

    private static void logException(Throwable t, boolean causedBy) {
        logMessage((causedBy ? "Caused by " : "") + t.getClass().getName() + ": " + t.getMessage(),
                true);

        final StackTraceElement[] trace = t.getStackTrace();

        for (final StackTraceElement el : trace) {
            logMessage("\t" + el.toString(), true);
        }

        for (final Throwable tt : t.getSuppressed()) {
            logException(tt, true);
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    public static void copyLogsTo(List<LogMessage> outLog) {
        if (outLog == null) {
            return;
        }

        outLog.clear();
        outLog.addAll(LogManager.LOG);
    }

    public static void printDisplay(String thread, String message) {
        final String msg = "[" + SDF.format(new Date()) + "] {" + thread + "}: " + message;

        System.out.println(msg);
        logMessage(msg, false);
    }

    public static void printError(String thread, String message) {
        printError(thread, message, null);
    }

    public static void printError(String thread, String message, Exception e) {
        final String msg = "[" + SDF.format(new Date()) + "] {" + thread + "}: "
                + (message == null ? "Exception" : message);

        System.err.println(msg);
        logMessage(msg, true);

        if (e != null) {
            e.printStackTrace();

            logException(e, false);
        }
    }
}
