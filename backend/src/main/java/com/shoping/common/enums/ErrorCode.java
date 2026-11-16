package com.shoping.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "成功"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),

    // 业务错误码 10000+
    PRODUCT_NOT_FOUND(10001, "商品不存在"),
    PRODUCT_SOLD_OUT(10002, "商品已售罄"),
    SKU_NOT_FOUND(10003, "商品规格不存在"),
    STOCK_NOT_ENOUGH(10004, "库存不足"),
    MATERIAL_NOT_FOUND(10005, "原料不存在"),
    MATERIAL_NOT_ENOUGH(10006, "原料库存不足"),
    CATEGORY_NOT_FOUND(10007, "分类不存在"),
    STORE_NOT_FOUND(10008, "门店不存在"),
    STORE_CLOSED(10009, "门店暂停营业，暂不接单"),
    CATEGORY_NAME_EXISTS(10010, "同类型下已存在同名分类"),
    CATEGORY_IN_USE(10011, "该分类下还有商品，请先移出商品再删除"),
    SPEC_OPTION_INVALID(10012, "制作选项不合法"),

    USER_NOT_FOUND(11001, "用户不存在"),
    LOGIN_FAILED(11007, "用户名或密码错误"),
    ACCOUNT_DISABLED(11008, "账号已被禁用"),
    ACCOUNT_EXISTS(11013, "该手机号已注册"),
    PHONE_FORMAT_INVALID(11014, "手机号格式不正确"),
    LOGIN_LOCKED(11016, "登录失败次数过多，请稍后再试"),
    OLD_PASSWORD_WRONG(11017, "原密码错误"),
    REFRESH_TOKEN_INVALID(11018, "刷新令牌无效或已过期"),
    MEMBER_NOT_FOUND(11002, "会员账户不存在"),
    POINTS_NOT_ENOUGH(11022, "积分不足"),
    POINTS_DEDUCT_LIMIT(11023, "积分抵扣超过本单上限"),
    BALANCE_ACCOUNT_NOT_FOUND(11003, "储值账户不存在"),
    BALANCE_NOT_ENOUGH(11004, "储值余额不足"),
    COUPON_NOT_FOUND(11005, "优惠券不存在"),
    COUPON_NOT_AVAILABLE(11006, "优惠券不可用"),
    COUPON_ALREADY_RECEIVED(11010, "该优惠券已领取过"),
    COUPON_THRESHOLD_NOT_MET(11011, "未满足优惠券使用门槛"),
    COUPON_SCOPE_NOT_MATCH(11012, "优惠券不适用于该类型商品"),
    BALANCE_AMOUNT_INVALID(11009, "调整后金额不能为负"),
    RECHARGE_AMOUNT_INVALID(11019, "请选择有效的充值档位"),
    RECHARGE_RULE_NOT_FOUND(11020, "充值档位不存在"),
    RECHARGE_RULE_EXISTS(11021, "该充值金额的档位已存在"),

    ORDER_NOT_FOUND(12001, "订单不存在"),
    ORDER_STATUS_ERROR(12002, "订单状态不允许该操作"),
    ORDER_NOT_CANCELABLE(12003, "订单已超过可取消时间"),
    ORDER_ALREADY_PAID(12004, "订单已支付"),
    QUEUE_NOT_FOUND(12005, "排队记录不存在"),

    AFTER_SALE_NOT_FOUND(13001, "售后工单不存在"),
    AFTER_SALE_REFUND_INVALID(13002, "退款金额不合法"),
    AFTER_SALE_REFUND_EXCEED(13003, "退款金额超过可退上限"),

    REVIEW_NOT_FOUND(15001, "评价不存在"),
    REVIEW_ITEM_NOT_FOUND(15002, "订单明细不存在"),
    REVIEW_NOT_ALLOWED(15003, "只能评价自己已完成订单中的商品"),
    REVIEW_ALREADY_EXISTS(15004, "该商品已经评价过了"),

    FILE_EMPTY(14001, "请选择要上传的文件"),
    FILE_TYPE_NOT_ALLOWED(14002, "仅支持 jpg / jpeg / png / gif / webp 图片"),
    FILE_TOO_LARGE(14003, "图片大小不能超过 5MB"),
    FILE_SAVE_FAILED(14004, "图片保存失败");

    private final int code;
    private final String message;
}
