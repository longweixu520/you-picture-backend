package com.yupi.yupicturebackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @Author longweixu
 * Spring MVC JSON 序列化配置类
 * @Description 解决前端JavaScript处理长整型(Long)数字时的精度丢失问题
 * @Date 16:21 2025/6/8
 * @Param
 * @return
 **/

@JsonComponent
public class JsonConfig {

    /**
     * 自定义Jackson ObjectMapper Bean
     *
     * 配置说明：
     * 1. 注册Long类型序列化器，将Long值转为字符串
     * 2. 同时处理Long.class和long.class两种类型
     *
     * @param builder Spring提供的Jackson构建器
     * @return 配置好的ObjectMapper对象
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // 1. 使用构建器创建基本的ObjectMapper实例
        // createXmlMapper(false)表示不启用XML映射功能
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // 2. 创建自定义序列化模块
        SimpleModule module = new SimpleModule();

        // 3. 添加Long类型的序列化规则：
        // - 使用ToStringSerializer将Long值序列化为字符串
        // - 处理包装类型Long.class和基本类型long.class
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 4. 将自定义模块注册到ObjectMapper
        objectMapper.registerModule(module);

        // 5. 返回配置好的ObjectMapper实例
        return objectMapper;
    }
}