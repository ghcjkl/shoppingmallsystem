package com.shoping.module.stats.controller;

import com.shoping.common.result.Result;
import com.shoping.module.stats.service.StatsService;
import com.shoping.module.stats.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端销售看板
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 销售看板：days 支持 7/14/30（默认 7） */
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(@RequestParam(defaultValue = "1") Long storeId,
                                         @RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.dashboard(storeId, days));
    }
}
