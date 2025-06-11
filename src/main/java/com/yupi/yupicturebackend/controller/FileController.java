package com.yupi.yupicturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.model.constant.UserConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 文件上传控制器
 * 处理文件上传相关的HTTP请求
 *
 * @author longweixu
 * @date 2025/6/11
 */
@RestController // 声明为Spring MVC控制器，所有方法返回值直接作为HTTP响应体
@RequestMapping("/file") // 定义基础路径为/file
@Slf4j // 使用Lombok提供的日志功能
public class FileController {

    /**
     * 注入COS文件管理服务
     * 用于实际处理文件上传到腾讯云COS
     */
    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传接口
     * 需要管理员权限才能访问
     *
     * @param multipartFile Spring MVC提供的文件上传对象，通过@RequestPart注解绑定
     * @return BaseResponse<String> 包含上传结果的响应对象
     * @throws BusinessException 当上传失败时抛出系统错误异常
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 权限校验注解，要求管理员角色
    @PostMapping("/test/upload") // 处理POST /file/test/upload请求
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 获取原始文件名
        String filename = multipartFile.getOriginalFilename();
        // 构造文件在COS中的存储路径，格式为/test/文件名
        String filepath = String.format("/test/%s", filename);

        // 声明临时文件变量
        File file = null;
        try {
            // 1. 创建临时文件
            // File.createTempFile会在系统临时目录创建文件
            file = File.createTempFile(filepath, null);

            // 2. 将上传的文件内容传输到临时文件
            multipartFile.transferTo(file);

            // 3. 调用COS管理器上传文件
            cosManager.putObject(filepath, file);

            // 4. 返回成功响应，包含文件路径
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            // 捕获并记录上传过程中的异常
            log.error("file upload error, filepath = " + filepath, e);
            // 抛出业务异常，提示"上传失败"
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 无论成功与否，都尝试删除临时文件
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载接口
     * 该接口用于从COS（腾讯云对象存储）下载指定路径的文件
     * 需要管理员权限才能访问
     *
     * @param filepath 要下载的文件在COS中的完整路径
     * @param response HTTP响应对象，用于返回文件数据
     * @throws IOException 当文件读取或写入响应流时发生错误
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)  // 权限校验，要求用户具有管理员角色
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        // 声明COS对象输入流，用于读取文件内容
        COSObjectInputStream cosObjectInput = null;
        try {
            // 1. 从COS获取文件对象
            COSObject cosObject = cosManager.getObject(filepath);

            // 2. 获取文件内容输入流
            cosObjectInput = cosObject.getObjectContent();

            // 3. 将输入流转换为字节数组
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);

            // 4. 设置响应头信息
            // 设置内容类型为二进制流，强制浏览器下载而不是直接打开
            response.setContentType("application/octet-stream;charset=UTF-8");
            // 设置Content-Disposition头，指定下载文件名
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);

            // 5. 将文件内容写入响应输出流
            response.getOutputStream().write(bytes);
            // 刷新输出流，确保所有数据都已发送
            response.getOutputStream().flush();

        } catch (Exception e) {
            // 捕获并记录异常
            log.error("file download error, filepath = " + filepath, e);
            // 抛出业务异常，返回系统错误信息
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            // 6. 在finally块中确保资源被释放
            if (cosObjectInput != null) {
                try {
                    // 关闭输入流
                    cosObjectInput.close();
                } catch (IOException e) {
                    // 记录但不抛出关闭流时可能出现的异常
                    log.error("Failed to close COS object input stream", e);
                }
            }
        }
    }

}