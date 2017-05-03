package com.github.checkstyle.ci.utils;

public final class LogMessage {
    private boolean error;
    private String message;

    public LogMessage(String message, boolean error) {
        this.message = message;
        this.error = error;
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
