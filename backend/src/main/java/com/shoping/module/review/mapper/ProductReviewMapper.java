package com.shoping.module.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shoping.module.review.entity.ProductReview;
import com.shoping.module.review.vo.ProductRatingVO;
import com.shoping.module.review.vo.ProductReviewVO;
import com.shoping.module.review.vo.StarCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    /** 公共查询列：昵称取 user.nickname，商品名取 product.name（商品被删时为空） */
    String COLS = "r.id, r.order_id AS orderId, r.order_item_id AS orderItemId, "
            + "r.product_id AS productId, r.user_id AS userId, r.rating, r.content, "
            + "r.anonymous, r.status, r.reply, r.reply_time AS replyTime, r.create_time AS createTime, "
            + "p.name AS productName, u.nickname AS nickname ";

    String JOINS = "FROM product_review r "
            + "LEFT JOIN user u ON u.id = r.user_id "
            + "LEFT JOIN product p ON p.id = r.product_id ";

    /** 商品页评价分页（只看显示中的，可按星级筛选） */
    @Select("<script>SELECT " + COLS + JOINS
            + "WHERE r.product_id = #{productId} AND r.status = 1 "
            + "<if test='rating != null'>AND r.rating = #{rating} </if>"
            + "ORDER BY r.id DESC</script>")
    IPage<ProductReviewVO> pageByProduct(IPage<ProductReviewVO> page,
                                         @Param("productId") Long productId,
                                         @Param("rating") Integer rating);

    /** 管理端评价分页（含隐藏，支持商品名/内容关键词） */
    @Select("<script>SELECT " + COLS + JOINS
            + "WHERE 1 = 1 "
            + "<if test='productId != null'>AND r.product_id = #{productId} </if>"
            + "<if test='status != null'>AND r.status = #{status} </if>"
            + "<if test='rating != null'>AND r.rating = #{rating} </if>"
            + "<if test='keyword != null and keyword != \"\"'>AND (p.name LIKE CONCAT('%', #{keyword}, '%') "
            + "OR r.content LIKE CONCAT('%', #{keyword}, '%')) </if>"
            + "ORDER BY r.id DESC</script>")
    IPage<ProductReviewVO> pageAdmin(IPage<ProductReviewVO> page,
                                     @Param("productId") Long productId,
                                     @Param("status") Integer status,
                                     @Param("rating") Integer rating,
                                     @Param("keyword") String keyword);

    /** 某商品的评分汇总（仅显示中的评价） */
    @Select("SELECT product_id AS productId, ROUND(AVG(rating), 1) AS rating, COUNT(*) AS reviewCount "
            + "FROM product_review WHERE product_id = #{productId} AND status = 1 GROUP BY product_id")
    ProductRatingVO summary(@Param("productId") Long productId);

    /** 各星级数量 */
    @Select("SELECT rating AS rating, COUNT(*) AS cnt FROM product_review "
            + "WHERE product_id = #{productId} AND status = 1 GROUP BY rating")
    List<StarCountVO> starDist(@Param("productId") Long productId);

    /** 批量评分：商品列表页一次查完，避免 N+1 */
    @Select("<script>SELECT product_id AS productId, ROUND(AVG(rating), 1) AS rating, COUNT(*) AS reviewCount "
            + "FROM product_review WHERE status = 1 AND product_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY product_id</script>")
    List<ProductRatingVO> batchRatings(@Param("ids") List<Long> ids);

    /** 某订单下的全部评价（含隐藏，用于订单详情判断「已评价」） */
    @Select("SELECT " + COLS + JOINS + "WHERE r.order_id = #{orderId} AND r.user_id = #{userId} ORDER BY r.id DESC")
    List<ProductReviewVO> listByOrder(@Param("orderId") Long orderId, @Param("userId") Long userId);
}
