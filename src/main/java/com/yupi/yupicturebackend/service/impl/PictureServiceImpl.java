package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.FileManager;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author longweixu
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-06-12 10:56:50
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwif(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwif(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 上传图片，得到信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwif(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }


    /**
     * 根据图片查询请求构建 QueryWrapper 对象
     * @Author longweixu
     * @Description 构造 QueryWrapper 对象来生成SQL 查询条件，用于图片数据的筛选和排序
     * @Date 22:02 2025/6/13
     * @Param [pictureQueryRequest] 图片查询请求参数对象，包含各种查询条件
     * @return com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.yupi.yupicturebackend.model.entity.Picture>
     **/
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        // 创建空的 QueryWrapper 对象
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        // 如果查询请求对象为空，直接返回空的 QueryWrapper
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }

        // 从查询请求对象中获取各个查询参数
        Long id = pictureQueryRequest.getId();                // 图片ID
        String name = pictureQueryRequest.getName();          // 图片名称
        String introduction = pictureQueryRequest.getIntroduction(); // 图片简介
        String category = pictureQueryRequest.getCategory();  // 图片分类
        List<String> tags = pictureQueryRequest.getTags();    // 图片标签列表
        Long picSize = pictureQueryRequest.getPicSize();      // 图片大小(字节)
        Integer picWidth = pictureQueryRequest.getPicWidth(); // 图片宽度(像素)
        Integer picHeight = pictureQueryRequest.getPicHeight(); // 图片高度(像素)
        Double picScale = pictureQueryRequest.getPicScale();  // 图片宽高比
        String picFormat = pictureQueryRequest.getPicFormat(); // 图片格式
        String searchText = pictureQueryRequest.getSearchText(); // 综合搜索文本
        Long userId = pictureQueryRequest.getUserId();        // 用户ID
        String sortField = pictureQueryRequest.getSortField(); // 排序字段
        String sortOrder = pictureQueryRequest.getSortOrder(); // 排序顺序(ascend/descend)

        // 处理综合搜索条件：在名称和简介字段中模糊搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 使用 and 连接条件，括号内是 or 关系
            queryWrapper.and(qw -> qw.like("name", searchText)  // 名称字段模糊匹配
                    .or()
                    .like("introduction", searchText)           // 或简介字段模糊匹配
            );
        }

        // 添加各个字段的精确查询条件（当值不为空时）
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);               // ID精确匹配
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);   // 用户ID精确匹配
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);       // 名称模糊匹配
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction); // 简介模糊匹配
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat); // 图片格式模糊匹配
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category); // 分类精确匹配

        // 添加数值型字段的精确查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);   // 宽度精确匹配
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight); // 高度精确匹配
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);      // 大小精确匹配
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);   // 宽高比精确匹配

        // 处理标签数组查询：JSON数组字段中查询包含指定标签的记录
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                // 对每个标签，查询JSON数组中包含该标签的记录
                // 添加引号是为了精确匹配标签值，避免部分匹配
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        // 添加排序条件（当排序字段不为空时）
        // sortOrder.equals("ascend") 判断是否为升序，否则为降序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        // 返回构建好的 QueryWrapper 对象
        return queryWrapper;
    }


    /**
     * 获取图片VO对象（单条）
     * @param picture 图片实体
     * @param request HTTP请求（暂未使用）
     * @return 图片VO对象，包含图片信息和关联的用户信息
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 转换为VO对象
        PictureVO pictureVO = PictureVO.objToVo(picture);

        // 处理关联用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            pictureVO.setUser(userService.getUserVO(user));
        }

        return pictureVO;
    }

    /**
     * 分页获取图片VO列表
     * @param picturePage 图片分页实体数据
     * @param request HTTP请求对象
     * @return 分页的图片VO数据，包含关联用户信息
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 1. 初始化返回的分页对象（保持原分页参数）
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());

        // 2. 处理空数据情况
        List<Picture> pictureList = picturePage.getRecords();
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }

        // 3. 批量转换实体为VO对象
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());

        // 4. 批量查询关联用户信息（优化N+1查询问题）
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());

        // 按用户ID分组存储（虽然ID唯一，但保持处理逻辑一致性）
        Map<Long, List<User>> userMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 5. 填充关联数据
        pictureVOList.forEach(pictureVO -> {
            List<User> users = userMap.get(pictureVO.getUserId());
            User user = CollUtil.isNotEmpty(users) ? users.get(0) : null;
            pictureVO.setUser(user != null ? userService.getUserVO(user) : null);
        });

        // 6. 设置结果并返回
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 校验图片参数有效性
     * @param picture 图片实体对象
     */
    @Override
    public void validPicture(Picture picture) {
        // 1. 基础非空校验
        ThrowUtils.throwif(picture == null, ErrorCode.PARAMS_ERROR, "图片不能为空");

        // 2. 获取关键字段
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        // 3. ID必填校验（适用于修改场景）
        ThrowUtils.throwif(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");

        // 4. URL长度校验（非必填字段）
        if (StrUtil.isNotBlank(url)) {
            final int URL_MAX_LENGTH = 1024;
            ThrowUtils.throwif(url.length() > URL_MAX_LENGTH,
                    ErrorCode.PARAMS_ERROR, "URL长度不能超过" + URL_MAX_LENGTH);
        }

        // 5. 简介长度校验（非必填字段）
        if (StrUtil.isNotBlank(introduction)) {
            final int DESC_MAX_LENGTH = 800;
            ThrowUtils.throwif(introduction.length() > DESC_MAX_LENGTH,
                    ErrorCode.PARAMS_ERROR, "简介长度不能超过" + DESC_MAX_LENGTH);
        }
    }


}




