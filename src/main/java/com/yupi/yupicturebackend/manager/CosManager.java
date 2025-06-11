package com.yupi.yupicturebackend.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.yupi.yupicturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

/**
 * 腾讯云COS(对象存储)管理类
 * 用于处理与腾讯云COS相关的文件上传操作
 */
@Component // 将该类声明为Spring的组件，使其可以被Spring容器管理
public class CosManager {

    /**
     * COS客户端配置信息
     * 通过@Resource注解从Spring容器中注入配置信息
     */
    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 腾讯云COS客户端实例
     * 通过@Resource注解从Spring容器中注入已配置好的COS客户端
     */
    @Resource
    private COSClient cosClient;


    /**
     * 上传文件到腾讯云COS
     * @param key 文件在COS中的唯一标识(包含路径的文件名)
     *            例如: "images/avatar/user123.jpg"
     * @param file 要上传的本地文件对象
     * @return PutObjectResult 上传结果对象，包含ETag、请求ID等信息
     * @throws com.qcloud.cos.exception.CosClientException 客户端异常
     * @throws com.qcloud.cos.exception.CosServiceException 服务端异常
     */
    public PutObjectResult putObject(String key, File file) {
        // 创建上传请求对象
        // 参数说明:
        // 1. bucket名称 - 从配置中获取
        // 2. key - 文件在COS中的唯一键
        // 3. file - 要上传的本地文件
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosClientConfig.getBucket(), // 存储桶名称
                key,                       // 文件键
                file                       // 要上传的文件
        );

        // 执行上传操作并返回结果
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 从腾讯云COS下载文件对象
     * @param key 文件在COS中的唯一标识(包含路径的文件名)
     *            例如: "images/avatar/user123.jpg"
     * @return COSObject 包含文件内容和元数据的对象，使用时需要注意以下几点:
     *         - 必须调用close()方法释放资源
     *         - 通过getObjectContent()获取文件输入流
     *         - 通过getObjectMetadata()获取文件元信息
     */
    public COSObject getObject(String key) {
        // 创建下载请求对象
        // 参数说明:
        // 1. bucket名称 - 从配置中获取
        // 2. key - 文件在COS中的唯一键
        GetObjectRequest getObjectRequest = new GetObjectRequest(
                cosClientConfig.getBucket(), // 存储桶名称
                key                        // 文件键
        );

        // 执行下载操作并返回结果对象
        return cosClient.getObject(getObjectRequest);
    }

}