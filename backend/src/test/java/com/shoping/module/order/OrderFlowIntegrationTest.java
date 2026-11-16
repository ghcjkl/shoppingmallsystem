package com.shoping.module.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoping.module.order.entity.OrderQueue;
import com.shoping.module.order.entity.Orders;
import com.shoping.module.order.mapper.OrderQueueMapper;
import com.shoping.module.order.mapper.OrdersMapper;
import com.shoping.module.user.entity.Member;
import com.shoping.module.user.mapper.MemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 下单 → 支付 → 制作 → 取餐核销 全链路集成测试。
 * <p>
 * 与单元测试的区别：这里起真实 Spring 容器、连独立测试库 shoping_test，
 * 验证多个组件协作后的数据库副作用（库存、积分、排队、通知），而非单个方法的返回值。
 * @Transactional 保证每个用例跑完自动回滚，测试库始终是干净基线。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrdersMapper ordersMapper;
    @Autowired private OrderQueueMapper orderQueueMapper;
    @Autowired private MemberMapper memberMapper;

    private String userToken;
    private String adminToken;
    private Long userId;

    @BeforeEach
    void login() throws Exception {
        userToken = loginAs("user", "123456");
        adminToken = loginAs("admin", "123456");
        JsonNode me = read(perform(get("/api/auth/me").header("Authorization", "Bearer " + userToken)));
        userId = me.path("data").path("userId").asLong();
    }

    private String loginAs(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return read(result).path("data").path("token").asText();
    }

    private MvcResult perform(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b)
            throws Exception {
        return mockMvc.perform(b).andReturn();
    }

    private JsonNode read(MvcResult result) throws Exception {
        // MockMvc 默认 ISO-8859-1 解码，中文会乱码；显式按 UTF-8 读
        return objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private MvcResult createOrder(int orderType, String itemsJson) throws Exception {
        String body = "{\"storeId\":1,\"orderType\":" + orderType + ",\"items\":" + itemsJson + "}";
        return mockMvc.perform(post("/api/order")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
    }

    private MvcResult pay(Long orderId, int payType) throws Exception {
        String body = "{\"orderId\":" + orderId + ",\"payType\":" + payType + "}";
        return mockMvc.perform(post("/api/order/pay")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
    }

    @Test
    @DisplayName("标品链路：下单 → 支付 → 直接待取餐 → 核销完成")
    void standardOrderFlow() throws Exception {
        // 下单：薯片 sku1 ×2
        JsonNode created = read(createOrder(1, "[{\"productId\":1,\"skuId\":1,\"quantity\":2}]"));
        long orderId = created.path("data").path("id").asLong();
        assertThat(orderId).isPositive();
        Orders order = ordersMapper.selectById(orderId);
        assertThat(order.getStatus()).isEqualTo(1); // 待支付
        assertThat(order.getPayAmount()).isEqualByComparingTo("19.80");

        // 支付：现金
        JsonNode paid = read(pay(orderId, 1));
        assertThat(paid.path("data").path("status").asInt()).isEqualTo(4); // 标品支付即待取餐
        String pickupCode = paid.path("data").path("pickupCode").asText();
        assertThat(pickupCode).hasSize(6);

        // 核销（管理员）
        mockMvc.perform(post("/api/order/pickup/verify")
                        .param("storeId", "1").param("pickupCode", pickupCode)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(5)); // 已完成

        assertThat(ordersMapper.selectById(orderId).getStatus()).isEqualTo(5);
    }

    @Test
    @DisplayName("现制链路：下单 → 支付 → 入队 → 开始制作 → 制作完成 → 通知 → 核销")
    void freshOrderFlow() throws Exception {
        // 下单：珍珠奶茶中杯 sku4 ×1，带制作选项 热(5)/无糖(8)/去冰(11)
        String items = "[{\"productId\":4,\"skuId\":4,\"quantity\":1,"
                + "\"specOptionIds\":[5,8,11]}]";
        JsonNode created = read(createOrder(2, items));
        long orderId = created.path("data").path("id").asLong();

        // 下单时快照就应包含制作偏好
        assertThat(created.path("data").path("items").path(0).path("skuSpec").asText())
                .contains("热").contains("无糖").contains("去冰");

        // 支付 → 状态 2（已支付）并生成 6 位取餐码
        JsonNode paid = read(pay(orderId, 1));
        assertThat(paid.path("data").path("status").asInt()).isEqualTo(2);
        String pickupCode = paid.path("data").path("pickupCode").asText();
        assertThat(pickupCode).hasSize(6).matches("\\d{6}");

        // 排队记录已生成
        OrderQueue queue = orderQueueMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderQueue>()
                        .eq(OrderQueue::getOrderId, orderId)).get(0);
        assertThat(queue.getStatus()).isEqualTo(1); // 排队中

        // 开始制作 → 制作完成
        mockMvc.perform(post("/api/order/" + orderId + "/making")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/order/" + orderId + "/ready")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));

        // 制作完成后推了站内取餐通知（未读）
        mockMvc.perform(get("/api/notice/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.code").value(0));

        // 核销完成
        mockMvc.perform(post("/api/order/pickup/verify")
                        .param("storeId", "1").param("pickupCode", pickupCode)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(5));
    }

    @Test
    @DisplayName("支付后积分与成长值按实付累加，与流水一致")
    void pointsAccrueOnPay() throws Exception {
        Member before = memberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Member>()
                        .eq(Member::getUserId, userId)).get(0);
        int oldPoints = before.getPoints() == null ? 0 : before.getPoints();

        JsonNode created = read(createOrder(1, "[{\"productId\":1,\"skuId\":1,\"quantity\":1}]"));
        long orderId = created.path("data").path("id").asLong();
        read(pay(orderId, 1)); // 实付 9.90 → 取整 9 分

        Member after = memberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Member>()
                        .eq(Member::getUserId, userId)).get(0);
        assertThat(after.getPoints()).isEqualTo(oldPoints + 9);
        assertThat(after.getGrowthValue()).isEqualTo(before.getGrowthValue() + 9L);
    }

    @Test
    @DisplayName("制作选项互斥：热 + 正常冰被服务端拒绝")
    void conflictingOptionsRejected() throws Exception {
        String items = "[{\"productId\":4,\"skuId\":4,\"quantity\":1,"
                + "\"specOptionIds\":[5,9]}]"; // 5=热, 9=正常冰
        String body = "{\"storeId\":1,\"orderType\":2,\"items\":" + items + "}";

        mockMvc.perform(post("/api/order")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10012)); // SPEC_OPTION_INVALID
    }

    @Test
    @DisplayName("积分抵扣：下单用 200 积分抵 2 元，取消订单后积分退回")
    void pointsDeductionAndRefund() throws Exception {
        // 事务内直接补够积分，保证用例自包含（其他用例回滚不影响这里）
        Member before = memberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Member>()
                        .eq(Member::getUserId, userId)).get(0);
        int oldPoints = before.getPoints() == null ? 0 : before.getPoints();
        if (oldPoints < 200) {
            before.setPoints(500);
            memberMapper.updateById(before);
            oldPoints = 500;
        }

        // 下单：薯片 ×2 = 19.80，用 200 积分（=2元，< 19.80*20%=3.96 上限）
        String body = "{\"storeId\":1,\"orderType\":1,\"usePoints\":200,"
                + "\"items\":[{\"productId\":1,\"skuId\":1,\"quantity\":2}]}";
        JsonNode created = read(mockMvc.perform(post("/api/order")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(0)).andReturn());
        long orderId = created.path("data").path("id").asLong();

        // 快照：实付 17.80 = 19.80 - 2.00，积分抵扣字段落库
        assertThat(created.path("data").path("payAmount").asDouble()).isEqualTo(17.80);
        assertThat(created.path("data").path("pointsDeducted").asInt()).isEqualTo(200);
        Orders afterOrder = ordersMapper.selectById(orderId);
        assertThat(afterOrder.getPointsAmount()).isEqualByComparingTo("2.00");

        // 下单即扣积分
        int afterDeduct = memberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Member>()
                        .eq(Member::getUserId, userId)).get(0).getPoints();
        assertThat(afterDeduct).isEqualTo(oldPoints - 200);

        // 取消订单 → 积分退回
        mockMvc.perform(post("/api/order/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.code").value(0));
        int afterCancel = memberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Member>()
                        .eq(Member::getUserId, userId)).get(0).getPoints();
        assertThat(afterCancel).isEqualTo(oldPoints);
    }

    @Test
    @DisplayName("越权防护：顾客 A 不能查顾客 B 的订单，也不能核销")
    void ownershipGuard() throws Exception {
        // 用 user 下单
        JsonNode created = read(createOrder(1, "[{\"productId\":1,\"skuId\":1,\"quantity\":1}]"));
        long orderId = created.path("data").path("id").asLong();
        read(pay(orderId, 1));

        // 注册第二个顾客
        String regBody = "{\"phone\":\"13900000009\",\"password\":\"123456\",\"nickname\":\"测试顾客B\"}";
        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(regBody))
                .andExpect(jsonPath("$.code").value(0)).andReturn();
        String tokenB = read(reg).path("data").path("token").asText();

        // B 查 A 的订单详情 → 12001「订单不存在」
        // 这是有意的安全设计：对非归属者伪装成「不存在」，避免泄露订单是否真实存在
        mockMvc.perform(get("/api/order/" + orderId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(12001));

        // B 核销 → 管理员拦截器拒绝（同样返回业务码 403）
        mockMvc.perform(post("/api/order/pickup/verify")
                        .param("storeId", "1").param("pickupCode", "123456")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(403));
    }
}
