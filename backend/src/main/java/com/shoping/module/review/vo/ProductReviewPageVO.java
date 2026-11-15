package com.shoping.module.review.vo;

import com.shoping.common.result.PageResult;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 商品评价页：评分汇总 + 分页列表 */
@Data
public class ProductReviewPageVO {

    /** 平均分，无评价为 0 */
    private BigDecimal avgRating;

    /** 评价总数（仅显示的，全部星级） */
    private long total;

    /** 当前筛选条件下的条数（分页器用） */
    private long filterTotal;

    /** 各星级数量：{"5": 10, "4": 2, ...} */
    private Map<Integer, Long> starDist;

    private List<ProductReviewVO> records;

    private long pageNum;
    private long pageSize;

    public static ProductReviewPageVO of(BigDecimal avgRating, long total, Map<Integer, Long> starDist,
                                         PageResult<ProductReviewVO> page) {
        ProductReviewPageVO vo = new ProductReviewPageVO();
        vo.setAvgRating(avgRating == null ? BigDecimal.ZERO : avgRating);
        vo.setTotal(total);
        vo.setFilterTotal(page.getTotal());
        vo.setStarDist(starDist);
        vo.setRecords(page.getRecords());
        vo.setPageNum(page.getPageNum());
        vo.setPageSize(page.getPageSize());
        return vo;
    }
}
