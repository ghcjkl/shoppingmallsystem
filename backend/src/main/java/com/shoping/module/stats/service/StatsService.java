package com.shoping.module.stats.service;

import com.shoping.module.stats.vo.DashboardVO;

public interface StatsService {

    /** 销售看板：days 为统计天数（7/14/30，其他值会被收敛） */
    DashboardVO dashboard(Long storeId, int days);
}
