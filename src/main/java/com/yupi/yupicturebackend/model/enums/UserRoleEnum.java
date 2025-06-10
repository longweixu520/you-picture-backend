package com.yupi.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @Author longweixu
 * @Description //用户角色枚举
 * @Date 14:57 2025/6/6
 * @Param
 * @return
 **/
@Getter
public enum UserRoleEnum {


    USER("用户","user"),
    ADMIN("管理员","admin");


    private final String text;

    private final String value;


    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
    /**
     * @Author longweixu
     * @Description 根据值查询用户
     * @Date 15:06 2025/6/6
     * @Param [value]
     * @return com.yupi.yupicturebackend.model.enums.UserRoleEnum
     **/

    public static UserRoleEnum getEnumByValue(String value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }

        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if(userRoleEnum.getValue().equals(value)){
                return userRoleEnum;
            }
        }
        return null;
    }

}
