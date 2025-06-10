package com.yupi.yupicturebackend.common;


import com.yupi.yupicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局通用的响应类
 * @param <T>
 */

@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data; //不清楚data是什么类型，那么我们用泛型

    private String message;


    public BaseResponse(int code,T data ,String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }


    public BaseResponse(int code, T data) {
        this(code,data,null);
    }


    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(),null,errorCode.getMessage());
    }
}
