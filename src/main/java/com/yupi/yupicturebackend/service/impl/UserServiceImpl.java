package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.user.UserQueryRequest;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.UserRoleEnum;
import com.yupi.yupicturebackend.model.vo.LoginUserVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.UserService;
import com.yupi.yupicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.yupi.yupicturebackend.model.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author longweixu
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-06-06 11:11:27
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    /**
     *对传进去的密码进行加密（登录/注册）
     * @param userPassword
     * @return
     */
    public String getEncryptPassword(String userPassword) {
        // 加盐，混淆；
        final String SALT = "LONGWEIXU";
        // 用Hutool工具类的工具
        return DigestUtils.md5DigestAsHex((SALT+userPassword).getBytes());
    }

    /**
     * @Author longweixu
     * @Description 获取当前登录用户
     * @Date 09:10 2025/6/7
     * @Param [request]
     * @return com.yupi.yupicturebackend.model.entity.User
     **/
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }


    /**
     * 用户注册
     * @param userAccount 账号
     * @param userPassword 密码
     * @param checkPassword 校验密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1、校验参数
        if(StrUtil.hasBlank(userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数存在为空");
        }
        if(userAccount.length()<6){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        if(userPassword.length()<8||checkPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        if(!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入的密码不一致");
        }


        // 2、检测用户账号和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwif(count > 0,ErrorCode.PARAMS_ERROR,"该账号已存在");

        // 3、密码一定要加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 4、把数据插入到数据库中
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if(!saveResult){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败，数据库错误");
        }
        return user.getId();
    }



    /**
     * @Author longweixu
     * @Description 用户登录
     * @Date 17:27 2025/6/6
     * @Param [userAccount, userPassword, request] 账号密码请求
     * @return com.yupi.yupicturebackend.model.vo.LoginUserVO
     **/

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            // log.info 这里看日志信息一定要有@Slf4j注解
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }


    /**
     * @Author longweixu
     * @Description 获取脱敏类的用户信息
     * @Date 17:27 2025/6/6
     * @Param [user] 用户
     * @return com.yupi.yupicturebackend.model.vo.LoginUserVO 脱敏后的用户信息
     **/
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }

        // 创建要返回的登录用户VO对象
        LoginUserVO loginUserVO = new LoginUserVO();

        // 使用Hutool工具的BeanUtil.copyProperties方法
        // 将源对象(user)的属性值复制到目标对象(loginUserVO)中
        // 作用：自动将user对象中与loginUserVO相同名称的属性值进行拷贝
        // 避免了手动逐个属性set的繁琐操作
        BeanUtil.copyProperties(user, loginUserVO);

        // 返回包含用户信息的VO对象
        return loginUserVO;
    }

    /**
     * @Author longweixu
     * @Description 用户注销
     * @Date 09:39 2025/6/7
     * @Param [request]
     * @return boolean
     **/
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * @Author longweixu
     * @Description 获取脱敏后的单个用户信息
     * @Date 14:52 2025/6/8
     * @Param [user]
     * @return com.yupi.yupicturebackend.model.vo.UserVO
     **/
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * @Author longweixu
     * @Description 获取脱敏后的用户列表
     * @Date 14:52 2025/6/8
     * @Param [userList]
     * @return java.util.List<com.yupi.yupicturebackend.model.vo.UserVO>
     **/
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }



    /**
     * 根据用户查询请求构建MyBatis-Plus的QueryWrapper条件构造器
     * @param userQueryRequest 用户查询请求对象，包含各种查询条件
     * @return 构建好的QueryWrapper<User>对象
     * @throws BusinessException 当请求参数为空时抛出
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 1. 参数校验
        if (userQueryRequest == null) {
            // 如果查询请求对象为空，抛出业务异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 2. 从请求对象中提取各种查询条件
        Long id = userQueryRequest.getId();               // 用户ID
        String userAccount = userQueryRequest.getUserAccount(); // 用户账号
        String userName = userQueryRequest.getUserName(); // 用户名
        String userProfile = userQueryRequest.getUserProfile(); // 用户简介
        String userRole = userQueryRequest.getUserRole(); // 用户角色
        String sortField = userQueryRequest.getSortField(); // 排序字段
        String sortOrder = userQueryRequest.getSortOrder(); // 排序方式

        // 3. 创建QueryWrapper对象用于构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        // 4. 构建查询条件（只有参数不为空时才添加对应条件）
        // 4.1 ID精确匹配（当id不为null时添加条件）
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        // 4.2 用户角色精确匹配（当userRole不为空字符串时添加条件）
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        // 4.3 用户账号模糊查询（当userAccount不为空字符串时添加条件）
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        // 4.4 用户名模糊查询（当userName不为空字符串时添加条件）
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        // 4.5 用户简介模糊查询（当userProfile不为空字符串时添加条件）
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);

        // 5. 构建排序条件（当排序字段不为空时添加排序）
        // 参数说明：1. 是否添加排序条件 2. 是否升序 3. 排序字段
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        // 6. 返回构建好的查询条件包装器
        return queryWrapper;
    }

    /**
     * @Author longweixu
     * @Description //判断是不是管理员
     * @Date 14:59 2025/6/13
     * @Param
     * @return
     **/
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }


}




