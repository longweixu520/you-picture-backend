package com.yupi.yupicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.model.constant.UserConstant;
import com.yupi.yupicturebackend.model.dto.picture.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebackend.model.vo.PictureTagCategory;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 图片管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;


    /**
     * 上传图片接口（支持重新上传）
     *
     * @param multipartFile        上传的图片文件
     * @param pictureUploadRequest 图片上传请求参数
     * @param request              HTTP请求对象
     * @return 包含图片VO的响应结果
     * @permission 仅管理员可操作
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务层上传图片
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片接口
     *
     * @param deleteRequest 删除请求参数（需包含图片ID）
     * @param request       HTTP请求对象
     * @return 操作结果布尔值
     * @permission 图片所有者或管理员可操作
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 参数校验
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误：ID不能为空");
        }

        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();

        // 检查图片是否存在
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwif(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 权限校验：仅本人或管理员可删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无操作权限");
        }

        // 执行删除操作
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwif(!result, ErrorCode.OPERATION_ERROR, "删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 更新图片信息接口
     *
     * @param pictureUpdateRequest 图片更新请求参数
     * @return 操作结果布尔值
     * @permission 仅管理员可操作
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        // 参数校验
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误：ID不能为空");
        }

        // DTO转Entity
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 处理标签列表转JSON字符串
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        // 数据校验
        pictureService.validPicture(picture);

        // 检查图片是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwif(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        //补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);

        // 执行更新操作
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwif(!result, ErrorCode.OPERATION_ERROR, "更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据ID获取图片详情（原始数据）
     *
     * @param id      图片ID
     * @param request HTTP请求对象
     * @return 图片实体数据
     * @permission 仅管理员可查看
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwif(id <= 0, ErrorCode.PARAMS_ERROR, "参数错误：ID必须大于0");

        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwif(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        return ResultUtils.success(picture);
    }

    /**
     * 根据ID获取图片详情（VO封装）
     *
     * @param id      图片ID
     * @param request HTTP请求对象
     * @return 图片VO数据
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwif(id <= 0, ErrorCode.PARAMS_ERROR, "参数错误：ID必须大于0");

        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwif(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 转换为VO对象
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 分页查询图片列表（原始数据）
     *
     * @param pictureQueryRequest 查询条件
     * @return 分页结果
     * @permission 仅管理员可查看
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        // 获取分页参数
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 构建查询条件并执行查询
        Page<Picture> picturePage = pictureService.page(
                new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest)
        );

        return ResultUtils.success(picturePage);
    }

    /**
     * 分页查询图片列表（VO封装）
     *
     * @param pictureQueryRequest 查询条件
     * @param request             HTTP请求对象
     * @return 分页结果（VO）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request) {
        // 参数校验
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwif(size > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");

        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 执行分页查询
        Page<Picture> picturePage = pictureService.page(
                new Page<>(pictureQueryRequest.getCurrent(), size),
                pictureService.getQueryWrapper(pictureQueryRequest)
        );

        // 转换为VO分页
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 编辑图片信息
     *
     * @param pictureEditRequest 编辑请求参数
     * @param request            HTTP请求对象
     * @return 操作结果
     * @permission 图片所有者或管理员可操作
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(
            @RequestBody PictureEditRequest pictureEditRequest,
            HttpServletRequest request) {
        // 参数校验
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误：ID不能为空");
        }

        // DTO转Entity
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 处理标签列表
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());

        // 数据校验
        pictureService.validPicture(picture);

        // 权限校验
        User loginUser = userService.getLoginUser(request);
        Picture oldPicture = pictureService.getById(pictureEditRequest.getId());
        ThrowUtils.throwif(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 仅本人或管理员可编辑
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无编辑权限");
        }

        // 补充审核参数
        pictureService.fillReviewParams(picture, loginUser);

        // 执行更新
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwif(!result, ErrorCode.OPERATION_ERROR, "更新失败");
        return ResultUtils.success(true);
    }



    /**
     * @Author longweixu
     * 获取预置标签和分类
     * @Description 根据需求，要支持用户根据标签和分类搜索图片，我们可以给用户列举一些常用的标签和分类，便于筛选。
     * @Date 23:04 2025/6/13
     * @Param []
     * @return com.yupi.yupicturebackend.common.BaseResponse<com.yupi.yupicturebackend.model.vo.PictureTagCategory>
     **/
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * @Author longweixu
     * @Description 审核图片的接口
     * @Date 15:48 2025/6/18
     * @Param [pictureReviewRequest, request]
     * @return com.yupi.yupicturebackend.common.BaseResponse<java.lang.Boolean>
     **/
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwif(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }


}


