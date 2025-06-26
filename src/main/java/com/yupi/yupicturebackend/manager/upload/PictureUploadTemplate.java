package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 完整的图片上传模板实现
 * 可直接用于生产环境
 */
@Slf4j
@Service
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    @PostConstruct
    public void init() {
        log.info("COS配置初始化完成，Bucket: {}, Host: {}",
                cosClientConfig.getBucket(), cosClientConfig.getHost());
    }

    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 前置检查
        checkPrerequisites(inputSource, uploadPathPrefix);

        // 2. 准备上传参数
        String originalFilename = getOriginFilename(inputSource);
        String fileSuffix = FileUtil.getSuffix(originalFilename);
        String uploadPath = generateUploadPath(uploadPathPrefix, fileSuffix);

        File tempFile = null;
        try {
            // 3. 创建并验证临时文件
            tempFile = createAndProcessTempFile(inputSource, fileSuffix);

            // 4. 执行上传
            log.info("正在上传文件到COS: {} (大小: {}KB)",
                    uploadPath, FileUtil.size(tempFile) / 1024);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);

            // 5. 处理结果
            return processUploadResult(putObjectResult, originalFilename, uploadPath, tempFile);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传过程中出现系统异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传服务暂时不可用");
        } finally {
            cleanTempFile(tempFile);
        }
    }

    private void checkPrerequisites(Object inputSource, String uploadPathPrefix) {
        // 检查输入源
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传内容不能为空");
        }

        // 检查COS配置
        if (cosManager == null || cosClientConfig == null) {
            log.error("COS服务未正确注入");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "存储服务配置错误");
        }

        if (StrUtil.isBlank(cosClientConfig.getBucket())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "存储桶未配置");
        }

        // 检查路径前缀
        if (StrUtil.isBlank(uploadPathPrefix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传路径前缀不能为空");
        }
    }

    private String generateUploadPath(String prefix, String suffix) {
        // 标准化路径前缀
        String normalizedPrefix = prefix.replaceAll("^/|/$", "");
        String dateStr = DateUtil.format(new Date(), "yyyyMMdd");
        String randomStr = RandomUtil.randomString(8);

        return String.format("%s/%s_%s.%s",
                normalizedPrefix, dateStr, randomStr, suffix);
    }

    private File createAndProcessTempFile(Object inputSource, String suffix) {
        File tempFile = null;
        try {
            // 创建临时文件
            tempFile = File.createTempFile("upload_", "." + suffix);
            log.debug("创建临时文件: {}", tempFile.getAbsolutePath());

            // 处理文件内容
            processFile(inputSource, tempFile);

            // 验证文件
            if (!tempFile.exists() || tempFile.length() == 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容为空");
            }

            // 检查文件大小 (2MB限制)
            if (tempFile.length() > 2 * 1024 * 1024) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小超过2MB限制");
            }

            return tempFile;
        } catch (BusinessException e) {
            cleanTempFile(tempFile);
            throw e;
        } catch (Exception e) {
            cleanTempFile(tempFile);
            log.error("处理临时文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件处理失败");
        }
    }

    private UploadPictureResult processUploadResult(
            PutObjectResult putObjectResult,
            String originalFilename,
            String uploadPath,
            File tempFile) {
        try {
            // 尝试获取高级图片信息
            if (putObjectResult.getCiUploadResult() != null &&
                    putObjectResult.getCiUploadResult().getProcessResults() != null) {
                ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
                List<CIObject> objectList = processResults.getObjectList();

                if (CollUtil.isNotEmpty(objectList)) {
                    CIObject compressedObj = objectList.get(0);
                    CIObject thumbnailObj = objectList.size() > 1 ? objectList.get(1) : compressedObj;
                    return buildEnhancedResult(originalFilename, compressedObj, thumbnailObj,
                            putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo());
                }
            }

            // 降级处理：使用基础信息
            return buildBasicResult(originalFilename, uploadPath, tempFile);
        } catch (Exception e) {
            log.warn("获取高级图片信息失败，使用基础信息", e);
            return buildBasicResult(originalFilename, uploadPath, tempFile);
        }
    }

    private UploadPictureResult buildEnhancedResult(
            String originalFilename,
            CIObject compressedObj,
            CIObject thumbnailObj,
            ImageInfo imageInfo) {
        UploadPictureResult result = new UploadPictureResult();

        // 设置基础信息
        result.setUrl(cosClientConfig.getHost() + "/" + compressedObj.getKey());
        result.setPicName(FileUtil.mainName(originalFilename));
        result.setPicSize(compressedObj.getSize().longValue());

        // 设置图片尺寸信息
        int width = compressedObj.getWidth() != null ? compressedObj.getWidth() : 0;
        int height = compressedObj.getHeight() != null ? compressedObj.getHeight() : 0;
        result.setPicWidth(width);
        result.setPicHeight(height);
        result.setPicScale(height > 0 ? NumberUtil.round(width * 1.0 / height, 2).doubleValue() : 0);

        // 设置格式
        result.setPicFormat(compressedObj.getFormat());

        return result;
    }

    private UploadPictureResult buildBasicResult(
            String originalFilename,
            String uploadPath,
            File tempFile) {
        UploadPictureResult result = new UploadPictureResult();

        result.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        result.setPicName(FileUtil.mainName(originalFilename));
        result.setPicSize(FileUtil.size(tempFile));
        result.setPicWidth(0); // 未知尺寸
        result.setPicHeight(0);
        result.setPicScale((double) 0);
        result.setPicFormat(FileUtil.getSuffix(originalFilename));

        return result;
    }

    private void cleanTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                if (!file.delete()) {
                    log.warn("临时文件删除失败: {}", file.getAbsolutePath());
                }
            } catch (SecurityException e) {
                log.warn("删除临时文件时遇到安全异常", e);
            }
        }
    }

    // 抽象方法 - 需要子类实现
    protected abstract void validPicture(Object inputSource);
    protected abstract String getOriginFilename(Object inputSource);
    protected abstract void processFile(Object inputSource, File file) throws Exception;
}