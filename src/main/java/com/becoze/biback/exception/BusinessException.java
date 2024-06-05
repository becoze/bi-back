package com.becoze.biback.exception;

import com.becoze.biback.common.ErrorCode;

/**
 * Custom exception class
 *
 */
public class BusinessException extends RuntimeException {

    /**
     * Error codes
     */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
