package com.shoping.common.file;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传结果
 */
@Data
public class UploadResult implements Serializable {

    /** 可直接入库/展示的相对访问地址，如 /uploads/product/202611/xxx.png */
    private String url;

    private String originalName;

    private long size;
}
