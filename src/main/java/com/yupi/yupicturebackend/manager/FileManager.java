package com.yupi.yupicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 文件管理服务类，负责处理文件上传、校验等操作
 */
@Service
@Slf4j
public class FileManager {

    // COS客户端配置
    @Resource
    private CosClientConfig cosClientConfig;

    // COS管理服务
    @Resource
    private CosManager cosManager;

    /**
     * 上传图片到对象存储
     *
     * @param multipartFile    上传的图片文件
     * @param uploadPathPrefix 上传路径前缀，用于组织存储结构
     * @return UploadPictureResult 包含图片信息的返回结果
     * @throws BusinessException 如果上传过程中出现错误
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 1. 校验图片合法性
        validPicture(multipartFile);

        // 2. 生成唯一的文件名和存储路径
        String uuid = RandomUtil.randomString(16); // 生成16位随机字符串作为唯一标识
        String originFilename = multipartFile.getOriginalFilename(); // 获取原始文件名
        // 构造上传文件名：日期_uuid.后缀
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        // 构造完整上传路径：/前缀/上传文件名
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 将上传的文件内容传输到临时文件
            multipartFile.transferTo(file);

            // 4. 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片元数据信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 5. 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth(); // 图片宽度
            int picHeight = imageInfo.getHeight(); // 图片高度
            // 计算并保留两位小数的宽高比
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

            // 设置返回结果的各种属性
            uploadPictureResult.setPicName(FileUtil.mainName(originFilename)); // 原始文件名（不含后缀）
            uploadPictureResult.setPicWidth(picWidth); // 图片宽度
            uploadPictureResult.setPicHeight(picHeight); // 图片高度
            uploadPictureResult.setPicScale(picScale); // 宽高比
            uploadPictureResult.setPicFormat(imageInfo.getFormat()); // 图片格式
            uploadPictureResult.setPicSize(FileUtil.size(file)); // 图片大小
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath); // 访问URL

            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 无论成功与否，都删除临时文件
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验图片文件的合法性
     *
     * @param multipartFile 待校验的图片文件
     * @throws BusinessException 如果文件不符合要求
     */
    public void validPicture(MultipartFile multipartFile) {
        // 检查文件是否为空
        ThrowUtils.throwif(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 1. 校验文件大小（不超过2MB）
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L; // 1MB的字节数
        ThrowUtils.throwif(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");

        // 2. 校验文件后缀（只允许特定格式）
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwif(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    /**
     * 删除临时文件
     *
     * @param file 要删除的临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 尝试删除文件并记录结果
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}