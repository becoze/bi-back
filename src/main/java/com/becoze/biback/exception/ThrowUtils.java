package com.becoze.biback.exception;

import com.becoze.biback.common.ErrorCode;

/**
 * Utils of Exception Throw
 *
 */
public class ThrowUtils {

    /**
     * Exception Throw
     *
     * @param condition boolean
     * @param runtimeException RuntimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * Exception Throw
     *
     * @param condition boolean
     * @param errorCode ErrorCode - custom error codes
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * Exception Throw
     *
     * @param condition boolean
     * @param errorCode ErrorCode - custom error codes
     * @param message String
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
