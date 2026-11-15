package com.shoping.module.review.vo;

import lombok.Data;

/** 某星级的评价数量 */
@Data
public class StarCountVO {

    private Integer rating;

    private Long cnt;
}
