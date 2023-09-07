package com.discovery.service.exception;

public class RedisSetException extends RuntimeException {
    public RedisSetException(String message) {
        super(message);
    }
}
