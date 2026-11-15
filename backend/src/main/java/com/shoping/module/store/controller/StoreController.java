package com.shoping.module.store.controller;

import com.shoping.common.result.Result;
import com.shoping.module.store.dto.StoreUpdateRequest;
import com.shoping.module.store.service.StoreService;
import com.shoping.module.store.vo.StoreVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public Result<StoreVO> getCurrent() {
        return Result.ok(storeService.getCurrent());
    }

    /** 管理端：修改门店信息与营业状态（写操作，需管理员令牌） */
    @PutMapping
    public Result<StoreVO> update(@Valid @RequestBody StoreUpdateRequest request) {
        return Result.ok(storeService.update(request));
    }
}
