package com.yupi.yupicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * NAME: 图片标签列表分类视图
 * USER: longweixu
 * DATA: 2025/6/13
 * PROJECT_NAME yu-picture-backend
 **/
@Data
public class PictureTagCategory {
    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;
}
