package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author longweixu
 * @Description 添加用户
 * @Date 14:41 2025/6/8
 * @Param
 * @return
 **/

@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;



    private static final long serialVersionUID = 1L;
}
