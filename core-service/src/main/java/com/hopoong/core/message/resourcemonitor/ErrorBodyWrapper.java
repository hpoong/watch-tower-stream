package com.hopoong.core.message.resourcemonitor;

public interface ErrorBodyWrapper<T> {
    String errorType();
    T originalMessage();
}
