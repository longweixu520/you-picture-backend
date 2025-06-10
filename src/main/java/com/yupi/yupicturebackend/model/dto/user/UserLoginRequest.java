package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author longweixu
 * @Description 用户登录
 * @Date 16:54 2025/6/6
 * @Param
 * @return
 **/

@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -4736848543355614070L;

    /**
     * 账号
     **/
    private String userAccount;

    /**
     * 密码
     **/
    private String userPassword;
}
