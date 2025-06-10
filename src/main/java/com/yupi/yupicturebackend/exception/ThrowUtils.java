package com.yupi.yupicturebackend.exception;


/**
 * 异常工具类
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param runtimeException 异常
     */
    public static void throwif(boolean condition,RuntimeException runtimeException) {
        if(condition) {
            throw runtimeException;
        }

    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwif(boolean condition,ErrorCode errorCode) {
        throwif(condition,new BusinessException(errorCode));

    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void throwif(boolean condition,ErrorCode errorCode,String message) {
        throwif(condition,new BusinessException(errorCode,message));
    }

}
