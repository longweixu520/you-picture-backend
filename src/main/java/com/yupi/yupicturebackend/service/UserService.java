package com.yupi.yupicturebackend.service;

import cn.hutool.core.annotation.Link;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupicturebackend.model.dto.user.UserQueryRequest;
import com.yupi.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.enums.UserRoleEnum;
import com.yupi.yupicturebackend.model.vo.LoginUserVO;
import com.yupi.yupicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author longweixu
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-06-06 11:11:27
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    long userRegister(String userAccount,String userPassword,String checkPassword);

    /**
     * 对传进去的密码进行加密
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);


    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);




    /**
     * @Author longweixu
     * @Description 用户登录
     * @Date 16:59 2025/6/6
     * @Param [userAccount, userPassword, request]
     * @return 脱敏后的用户信息
     **/
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);


    /**
     * @Author longweixu
     * @Description 获取脱敏类的用户信息
     * @Date 17:26 2025/6/6
     * @Param [user]
     * @return com.yupi.yupicturebackend.model.vo.LoginUserVO
     **/
    LoginUserVO getLoginUserVO(User user);


    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);


    /**
     * @Author longweixu
     * @Description 获取脱敏后的单个用户信息
     * @Date 14:51 2025/6/8
     * @Param [user]
     * @return com.yupi.yupicturebackend.model.vo.UserVO
     **/
    UserVO getUserVO(User user);


    /**
     * @Author longweixu
     * @Description 获取脱敏后的用户列表
     * @Date 14:51 2025/6/8
     * @Param [users]
     * @return java.util.List<com.yupi.yupicturebackend.model.vo.UserVO>
     **/

    List<UserVO> getUserVOList(List<User> users);


    /**
     * @Author longweixu
     * @Description 查询
     * @Date 14:43 2025/6/10
     * @Param [userQueryRequest]
     * @return com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.yupi.yupicturebackend.model.entity.User>
     **/

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
