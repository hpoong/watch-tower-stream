package com.hopoong.core.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonResponseCodeEnum {

    // ************ T1 : **
    USER("T1", "C01"),


    // ************ T9 : Server
    SERVER("T9", "C01");

    private final String type;
    private final String code;
}
