package com.shoping.module.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.shoping.common.cache.CacheKeys;
import com.shoping.common.cache.CacheService;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.store.dto.StoreUpdateRequest;
import com.shoping.module.store.entity.Store;
import com.shoping.module.store.mapper.StoreMapper;
import com.shoping.module.store.service.StoreService;
import com.shoping.module.store.vo.StoreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreMapper storeMapper;
    private final CacheService cacheService;

    @Override
    public StoreVO getCurrent() {
        return cacheService.remember(CacheKeys.STORE_CURRENT, CacheKeys.TTL_STORE,
                new TypeReference<StoreVO>() {
                }, () -> StoreVO.from(firstStore()));
    }

    @Override
    public StoreVO update(StoreUpdateRequest request) {
        Store current = firstStore();
        Store patch = new Store();
        patch.setId(current.getId());
        patch.setName(request.getName().trim());
        // 这几个字段允许被清空，用空串落库（MyBatis-Plus 默认忽略 null，不会覆盖）
        patch.setAddress(request.getAddress() == null ? "" : request.getAddress().trim());
        patch.setPhone(request.getPhone() == null ? "" : request.getPhone().trim());
        patch.setBusinessHours(request.getBusinessHours() == null ? "" : request.getBusinessHours().trim());
        patch.setStatus(request.getStatus() == null ? current.getStatus() : request.getStatus());
        storeMapper.updateById(patch);
        // 门店名/营业时间/营业状态都变了，前台缓存的整条门店信息立即失效
        cacheService.evict(CacheKeys.STORE_CURRENT);
        return StoreVO.from(firstStore());
    }

    private Store firstStore() {
        List<Store> stores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>().orderByAsc(Store::getId).last("limit 1"));
        if (stores.isEmpty()) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        return stores.get(0);
    }
}
