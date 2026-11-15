package com.shoping.module.store.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 门店设置提交参数（管理端）
 */
@Data
public class StoreUpdateRequest {

    @NotBlank(message = "门店名称不能为空")
    @Size(max = 64, message = "门店名称不能超过 64 个字符")
    private String name;

    @Size(max = 255, message = "门店地址不能超过 255 个字符")
    private String address;

    @Size(max = 32, message = "联系电话不能超过 32 个字符")
    private String phone;

    @Size(max = 128, message = "营业时间不能超过 128 个字符")
    private String businessHours;

    /** 1 营业 / 0 停业 */
    @Min(value = 0, message = "营业状态取值 0 或 1")
    @Max(value = 1, message = "营业状态取值 0 或 1")
    private Integer status;
}
