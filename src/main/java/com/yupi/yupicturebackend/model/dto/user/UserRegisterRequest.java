package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName UserRegisterRequest
 * @Description 用户注册请求
 * @Author longweixu
 * @Data 2025/6/6 15:16
 **/
@Data
public class UserRegisterRequest implements Serializable {


    private static final long serialVersionUID = -4736848543355614070L;
    /**
     * 账号
     **/
    private String userAccount;

    /**
     * 密码
     **/
    private String userPassword;

    /**
     * 确认密码
     **/
    private String checkPassword;

}
