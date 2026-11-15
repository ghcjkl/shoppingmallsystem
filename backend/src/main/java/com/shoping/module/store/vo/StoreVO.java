package com.shoping.module.store.vo;

import com.shoping.module.store.entity.Store;
import lombok.Data;

@Data
public class StoreVO {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String businessHours;
    private Integer status;

    public static StoreVO from(Store store) {
        StoreVO vo = new StoreVO();
        vo.setId(store.getId());
        vo.setName(store.getName());
        vo.setAddress(store.getAddress());
        vo.setPhone(store.getPhone());
        vo.setBusinessHours(store.getBusinessHours());
        vo.setStatus(store.getStatus());
        return vo;
    }
}
