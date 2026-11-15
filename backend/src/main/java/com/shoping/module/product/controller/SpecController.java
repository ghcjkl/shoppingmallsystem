package com.shoping.module.product.controller;

import com.shoping.common.result.Result;
import com.shoping.module.product.service.SpecService;
import com.shoping.module.product.vo.SpecGroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/spec")
@RequiredArgsConstructor
public class SpecController {

    private final SpecService specService;

    @GetMapping("/list")
    public Result<List<SpecGroupVO>> list(@RequestParam(required = false) Integer productType) {
        return Result.ok(specService.listGroups(productType));
    }
}
