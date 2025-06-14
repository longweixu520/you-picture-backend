package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author longweixu
 * @Description 图片修改请求，一般情况下给普通用户使用，可修改的字段范围小于更新请求
 * @Date 14:57 2025/6/13
 * @Param
 * @return
 **/
@Data
public class PictureEditRequest implements Serializable {
  
    /**  
     * id  
     */  
    private Long id;  
  
    /**  
     * 图片名称  
     */  
    private String name;  
  
    /**  
     * 简介  
     */  
    private String introduction;  
  
    /**  
     * 分类  
     */  
    private String category;  
  
    /**  
     * 标签  
     */  
    private List<String> tags;
  
    private static final long serialVersionUID = 1L;  
}
