package com.becoze.biback.common;

/**
 * Custom error codes:
 *
 */
public enum ErrorCode {

    SUCCESS(0, "Success"),
    PARAMS_ERROR(40000, "Invalid request parameters"),
    NOT_LOGIN_ERROR(40100, "Not logged in"),
    NO_AUTH_ERROR(40101, "No permission"),
    NOT_FOUND_ERROR(40400, "Requested data not found"),
    TOO_MANY_REQUEST(42900, "Too many requests, please try again later"),
    FORBIDDEN_ERROR(40300, "Access forbidden"),
    SYSTEM_ERROR(50000, "System error"),
    OPERATION_ERROR(50001, "Operation failed");

    /**
     * Error Code / Status Code
     */
    private final int code;

    /**
     * Message
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
