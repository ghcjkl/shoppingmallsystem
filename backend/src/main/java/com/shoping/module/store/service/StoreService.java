package com.shoping.module.store.service;

import com.shoping.module.store.dto.StoreUpdateRequest;
import com.shoping.module.store.vo.StoreVO;

public interface StoreService {

    StoreVO getCurrent();

    /** 更新门店基本信息与营业状态，返回更新后的门店 */
    StoreVO update(StoreUpdateRequest request);
}
