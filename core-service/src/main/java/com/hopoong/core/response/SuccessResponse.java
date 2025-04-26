package com.hopoong.core.response;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuccessResponse<T> extends CommonResponse {
    private T data;

    public SuccessResponse(String type, String code, String message, T data) {
        super(true, type, code, message);
        this.data = data;
    }

    public SuccessResponse(CommonResponseCodeEnum commonResponseCodeEnum, String message) {
        this(commonResponseCodeEnum.getType(), commonResponseCodeEnum.getCode(), message, null);
    }

    public SuccessResponse(CommonResponseCodeEnum commonResponseCodeEnum, T data) {
        this(commonResponseCodeEnum.getType(), commonResponseCodeEnum.getCode(), "Success", data);
    }
}
