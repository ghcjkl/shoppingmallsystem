package com.shoping.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.common.context.AuthContext;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.notice.service.NoticeService;
import com.shoping.module.order.dto.OrderCreateRequest;
import com.shoping.module.order.dto.OrderItemRequest;
import com.shoping.module.order.entity.OrderItem;
import com.shoping.module.order.entity.OrderQueue;
import com.shoping.module.order.entity.Orders;
import com.shoping.module.order.mapper.OrderItemMapper;
import com.shoping.module.order.mapper.OrderQueueMapper;
import com.shoping.module.order.mapper.OrdersMapper;
import com.shoping.module.order.service.OrderService;
import com.shoping.module.order.vo.OrderItemVO;
import com.shoping.module.order.vo.OrderVO;
import com.shoping.module.order.vo.QueueVO;
import com.shoping.module.product.entity.Ingredient;
import com.shoping.module.product.entity.Material;
import com.shoping.module.product.entity.Product;
import com.shoping.module.product.entity.ProductMaterial;
import com.shoping.module.product.entity.ProductSku;
import com.shoping.module.product.entity.SpecGroup;
import com.shoping.module.product.entity.SkuSpecOption;
import com.shoping.module.product.entity.SpecOption;
import com.shoping.module.product.mapper.IngredientMapper;
import com.shoping.module.product.mapper.MaterialMapper;
import com.shoping.module.product.mapper.ProductMapper;
import com.shoping.module.product.mapper.ProductMaterialMapper;
import com.shoping.module.product.mapper.ProductSkuMapper;
import com.shoping.module.product.mapper.SkuSpecOptionMapper;
import com.shoping.module.product.mapper.SpecGroupMapper;
import com.shoping.module.product.mapper.SpecOptionMapper;
import com.shoping.module.store.entity.Store;
import com.shoping.module.store.mapper.StoreMapper;
import com.shoping.module.stock.entity.StockRecord;
import com.shoping.module.stock.mapper.StockRecordMapper;
import com.shoping.module.stock.service.StockRollbackService;
import com.shoping.module.user.entity.BalanceAccount;
import com.shoping.module.user.entity.Coupon;
import com.shoping.module.user.entity.UserCoupon;
import com.shoping.module.user.mapper.CouponMapper;
import com.shoping.module.user.mapper.UserCouponMapper;
import com.shoping.module.user.service.BalanceService;
import com.shoping.module.user.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** 订单状态 */
    private static final int ORDER_PENDING_PAY = 1;
    private static final int ORDER_PAID = 2;
    private static final int ORDER_MAKING = 3;
    private static final int ORDER_READY = 4;
    private static final int ORDER_FINISHED = 5;
    private static final int ORDER_CANCELLED = 6;
    /** 售后中（与 AfterSaleServiceImpl 的定义一致） */
    private static final int ORDER_AFTER_SALE = 7;

    /** 排队状态 */
    private static final int QUEUE_WAITING = 1;
    private static final int QUEUE_MAKING = 2;
    private static final int QUEUE_READY = 3;
    private static final int QUEUE_FINISHED = 4;
    private static final int QUEUE_CANCELLED = 5;

    /** 订单类型 */
    private static final int TYPE_STANDARD = 1;
    private static final int TYPE_FRESH = 2;

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderQueueMapper orderQueueMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final SkuSpecOptionMapper skuSpecOptionMapper;
    private final SpecOptionMapper specOptionMapper;
    private final SpecGroupMapper specGroupMapper;
    private final IngredientMapper ingredientMapper;
    private final ProductMaterialMapper productMaterialMapper;
    private final MaterialMapper materialMapper;
    private final StoreMapper storeMapper;
    private final BalanceService balanceService;
    private final MemberService memberService;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final StockRecordMapper stockRecordMapper;
    private final StockRollbackService stockRollbackService;
    private final NoticeService noticeService;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public OrderVO create(OrderCreateRequest request) {
        Store store = storeMapper.selectById(request.getStoreId());
        if (store == null) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        // 门店暂停营业时不接单（后台「门店设置」可切换营业状态）
        if (store.getStatus() != null && store.getStatus() == 0) {
            throw new BusinessException(ErrorCode.STORE_CLOSED);
        }
        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setStoreId(request.getStoreId());
        order.setOrderType(request.getOrderType());
        order.setStatus(ORDER_PENDING_PAY);
        order.setRemark(request.getRemark());
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setBalancePaid(BigDecimal.ZERO);
        order.setCashPaid(BigDecimal.ZERO);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = buildOrderItem(order, itemReq);
            total = total.add(item.getAmount());
            orderItems.add(item);
        }
        order.setTotalAmount(total);

        // 校验券并预计算优惠金额（先不落库，拿到订单 id 后再绑定）
        BigDecimal discount = computeCouponDiscount(request, total);
        order.setDiscountAmount(discount);
        order.setPayAmount(total.subtract(discount));

        ordersMapper.insert(order);

        // 订单 id 生成后，正式核销券并绑定订单号
        if (request.getUserCouponId() != null) {
            UserCoupon userCoupon = userCouponMapper.selectById(request.getUserCouponId());
            userCoupon.setStatus(1);
            userCoupon.setOrderId(order.getId());
            userCouponMapper.updateById(userCoupon);
        }

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        return buildDetail(order.getId());
    }

    /**
     * 校验并计算优惠券减免金额（不落库）
     */
    private BigDecimal computeCouponDiscount(OrderCreateRequest request, BigDecimal total) {
        if (request.getUserCouponId() == null) {
            return BigDecimal.ZERO;
        }
        UserCoupon userCoupon = userCouponMapper.selectById(request.getUserCouponId());
        if (userCoupon == null || !userCoupon.getUserId().equals(request.getUserId())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }
        if (userCoupon.getStatus() == null || userCoupon.getStatus() != 0) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null || coupon.getStatus() == null || coupon.getStatus() != 1) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (coupon.getEndTime() != null && LocalDateTime.now().isAfter(coupon.getEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        // 适用范围：0 全场，1 标品，2 现制
        if (coupon.getScope() != null && coupon.getScope() != 0
                && !coupon.getScope().equals(request.getOrderType())) {
            throw new BusinessException(ErrorCode.COUPON_SCOPE_NOT_MATCH);
        }
        // 满减门槛
        BigDecimal threshold = coupon.getThreshold() == null ? BigDecimal.ZERO : coupon.getThreshold();
        if (total.compareTo(threshold) < 0) {
            throw new BusinessException(ErrorCode.COUPON_THRESHOLD_NOT_MET);
        }
        // 计算优惠金额：type 1 满减，type 2 折扣，type 4 立减
        BigDecimal discount;
        if (coupon.getType() != null && coupon.getType() == 2) {
            BigDecimal rate = coupon.getAmount() == null ? BigDecimal.ONE : coupon.getAmount();
            discount = total.multiply(BigDecimal.ONE.subtract(rate));
        } else {
            discount = coupon.getAmount() == null ? BigDecimal.ZERO : coupon.getAmount();
        }
        if (discount.compareTo(total) > 0) {
            discount = total;
        }
        return discount;
    }

    /**
     * 解析下单用的 SKU：
     * <ul>
     *   <li>传了 skuId：必须存在、属于该商品、且处于启用状态（防止拿别的商品的规格/低价规格下单）</li>
     *   <li>没传 skuId（首页卡片快捷加购）：取该商品第一个启用规格，标品与现制一致</li>
     * </ul>
     */
    private ProductSku resolveSku(Product product, Long skuId) {
        if (skuId != null) {
            ProductSku sku = productSkuMapper.selectById(skuId);
            if (sku == null) {
                throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
            }
            if (!product.getId().equals(sku.getProductId())
                    || sku.getStatus() == null || sku.getStatus() != 1) {
                throw new BusinessException(ErrorCode.SKU_NOT_FOUND, "商品规格不存在或已下架，请重新选择");
            }
            return sku;
        }
        List<ProductSku> enabled = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, product.getId())
                .eq(ProductSku::getStatus, 1)
                .orderByAsc(ProductSku::getId)
                .last("limit 1"));
        if (enabled.isEmpty()) {
            throw new BusinessException(ErrorCode.SKU_NOT_FOUND, "商品暂无可售规格");
        }
        return enabled.get(0);
    }

    private OrderItem buildOrderItem(Orders order, OrderItemRequest itemReq) {
        Product product = productMapper.selectById(itemReq.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        ProductSku sku = resolveSku(product, itemReq.getSkuId());
        BigDecimal unitPrice = sku.getPrice() != null ? sku.getPrice() : product.getBasePrice();
        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
        }

        // 下单锁库存：条件更新（stock >= qty）由数据库行锁保证原子性，避免并发超卖
        int qty = itemReq.getQuantity();
        if (productSkuMapper.deductStock(sku.getId(), qty) == 0) {
            // 扣不动有两种原因：规格刚被下架/删除，或者库存不足
            ProductSku latest = productSkuMapper.selectById(sku.getId());
            if (latest == null || latest.getStatus() == null || latest.getStatus() != 1) {
                throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        // 以扣减后的真实库存写流水：并发下「扣前值」已不可靠，用 扣后+本次 反推
        ProductSku deducted = productSkuMapper.selectById(sku.getId());
        int afterStock = deducted == null || deducted.getStock() == null ? 0 : deducted.getStock();
        sku.setStock(afterStock);
        writeStockRecord(sku.getId(), product.getName(), sku.getSkuCode(), 2, qty,
                afterStock + qty, afterStock,
                "订单 " + order.getOrderNo() + " 销售出库");

        String skuSpec = buildSkuSpec(sku);
        // 制作选项（温度/甜度/冰量等）：客户端只传选项 ID，名称与加价一律由服务端读库，
        // 校验通过后拼进规格快照，订单详情/排队看板无需改动即可展示给店员。
        List<SpecOption> makeOptions = resolveMakeOptions(product, itemReq.getSpecOptionIds());
        if (!makeOptions.isEmpty()) {
            String makeSpec = makeOptions.stream().map(SpecOption::getName).collect(Collectors.joining("/"));
            skuSpec = skuSpec.isEmpty() ? makeSpec : skuSpec + "/" + makeSpec;
            for (SpecOption option : makeOptions) {
                unitPrice = unitPrice.add(option.getPriceDelta() == null ? BigDecimal.ZERO : option.getPriceDelta());
            }
        }
        String ingredientSnapshot = "";
        if (itemReq.getIngredientIds() != null && !itemReq.getIngredientIds().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Long ingredientId : itemReq.getIngredientIds()) {
                Ingredient ingredient = ingredientMapper.selectById(ingredientId);
                if (ingredient == null) {
                    continue;
                }
                names.add(ingredient.getName());
                unitPrice = unitPrice.add(ingredient.getPrice() == null ? BigDecimal.ZERO : ingredient.getPrice());
            }
            ingredientSnapshot = String.join("/", names);
        }

        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setSkuId(sku != null ? sku.getId() : null);
        item.setProductName(product.getName());
        item.setSkuSpec(skuSpec);
        item.setIngredients(ingredientSnapshot);
        item.setPrice(unitPrice);
        item.setQuantity(itemReq.getQuantity());
        item.setAmount(unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        return item;
    }

    /**
     * 校验并解析制作选项（温度/甜度/冰量等）。
     * 规则：仅现制饮品可用；同一规格组至多选一项；选项必须存在且属于适用组；
     * SKU 维度组（如「杯型」）必须由 SKU 决定，不能通过制作选项绕过价格与库存。
     * 返回按「组排序 → 选项排序」排序的列表，保证订单快照顺序稳定。
     */
    private List<SpecOption> resolveMakeOptions(Product product, List<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            return List.of();
        }
        if (product.getProductType() == null || product.getProductType() != 2) {
            throw new BusinessException(ErrorCode.SPEC_OPTION_INVALID, "标品不支持制作选项");
        }
        if (optionIds.size() > 10) {
            throw new BusinessException(ErrorCode.SPEC_OPTION_INVALID, "制作选项过多");
        }
        Set<Long> distinct = new HashSet<>(optionIds);
        if (distinct.size() != optionIds.size()) {
            throw new BusinessException(ErrorCode.SPEC_OPTION_INVALID, "制作选项不能重复");
        }
        List<SpecOption> options = new ArrayList<>(specOptionMapper.selectBatchIds(distinct));
        if (options.size() != distinct.size()) {
            throw new BusinessException(ErrorCode.SPEC_OPTION_INVALID, "存在无效的制作选项");
        }

        Map<Long, SpecGroup> groupCache = new HashMap<>();
        Set<Long> usedGroups = new HashSet<>();
        for (SpecOption option : options) {
            Long groupId = option.getGroupId();
            if (!usedGroups.add(groupId)) {
                throw new BusinessException(ErrorCode.SPEC_OPTION_INVALID, "同一规格组只能选择一项");
            }
            SpecGroup group = groupCache.computeIfAbsent(groupId, specGroupMapper::selectById);
            if (group == null || (group.getProductType() != null && group.getProductType() != 2)) {
                throw new BusinessException(ErrorCode.SPEC_OPTION_INVALID, "该规格组不适用于现制饮品");
            }
            List<Long> siblingIds = specOptionMapper.selectList(new LambdaQueryWrapper<SpecOption>()
                            .eq(SpecOption::getGroupId, groupId)).stream()
                    .map(SpecOption::getId).toList();
            Long linked = siblingIds.isEmpty() ? 0L : skuSpecOptionMapper.selectCount(
                    new LambdaQueryWrapper<SkuSpecOption>().in(SkuSpecOption::getSpecOptionId, siblingIds));
            if (linked != null && linked > 0) {
                throw new BusinessException(ErrorCode.SPEC_OPTION_INVALID,
                        "「" + group.getName() + "」请在商品规格中选择");
            }
        }
        options.sort(Comparator
                .comparingInt((SpecOption o) -> {
                    SpecGroup g = groupCache.get(o.getGroupId());
                    return g == null || g.getSort() == null ? 0 : g.getSort();
                })
                .thenComparingInt(o -> o.getSort() == null ? 0 : o.getSort()));
        return options;
    }

    private String buildSkuSpec(ProductSku sku) {
        if (sku == null) {
            return "";
        }
        List<SkuSpecOption> links = skuSpecOptionMapper.selectList(
                new LambdaQueryWrapper<SkuSpecOption>().eq(SkuSpecOption::getSkuId, sku.getId()));
        return links.stream()
                .map(link -> specOptionMapper.selectById(link.getSpecOptionId()))
                .filter(java.util.Objects::nonNull)
                .map(SpecOption::getName)
                .collect(Collectors.joining("/"));
    }

    @Override
    @Transactional
    public OrderVO pay(Long orderId, Integer payType, Long operatorId, boolean admin) {
        Orders order = getOrder(orderId);
        // 归属校验：顾客只能付自己的单，避免拿到订单号就扣他人储值余额
        assertOwner(order, operatorId, admin);
        if (order.getStatus() != ORDER_PENDING_PAY) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }
        BigDecimal payAmount = order.getPayAmount();
        BigDecimal balancePaid = BigDecimal.ZERO;
        BigDecimal cashPaid = BigDecimal.ZERO;

        if (payType == 2) {
            balancePaid = payAmount;
        } else if (payType == 3) {
            BalanceAccount account = balanceService.getAccount(order.getUserId());
            BigDecimal available = account.getPrincipal().add(account.getGift());
            balancePaid = available.min(payAmount);
            cashPaid = payAmount.subtract(balancePaid);
        } else {
            cashPaid = payAmount;
        }

        if (balancePaid.compareTo(BigDecimal.ZERO) > 0) {
            balanceService.consume(order.getUserId(), order.getId(), balancePaid);
        }

        order.setStatus(order.getOrderType() == TYPE_FRESH ? ORDER_PAID : ORDER_READY);
        order.setPayType(payType);
        order.setBalancePaid(balancePaid);
        order.setCashPaid(cashPaid);
        order.setPayTime(LocalDateTime.now());
        order.setPickupCode(generatePickupCode(order.getStoreId()));

        // 现制：扣原料并进入排队；标品无需制作，支付后直接置为「待取餐」，店员凭取餐码核销完结
        if (order.getOrderType() == TYPE_FRESH) {
            deductMaterials(order.getId());
            enqueue(order);
        }
        ordersMapper.updateById(order);

        // 支付成功：按实付金额累计积分与成长值
        memberService.onPaid(order.getUserId(), order.getId(), payAmount);

        // 标品无需制作，支付即成可取餐状态 → 推一条站内取餐提醒（现制等制作完成再提醒）
        if (order.getOrderType() != TYPE_FRESH) {
            noticeService.pushPickupReady(order);
        }

        return buildDetail(order.getId());
    }

    private void enqueue(Orders order) {
        List<OrderQueue> queues = orderQueueMapper.selectList(
                new LambdaQueryWrapper<OrderQueue>().eq(OrderQueue::getStoreId, order.getStoreId()));
        int maxNo = queues.stream().mapToInt(q -> q.getQueueNo() == null ? 0 : q.getQueueNo()).max().orElse(0);
        int queueNo = maxNo + 1;

        OrderQueue queue = new OrderQueue();
        queue.setOrderId(order.getId());
        queue.setStoreId(order.getStoreId());
        queue.setQueueNo(queueNo);
        queue.setStatus(QUEUE_WAITING);
        queue.setEstimateMinutes(estimateMinutes(order.getId()));
        orderQueueMapper.insert(queue);

        order.setQueueNo(queueNo);
    }

    private int estimateMinutes(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        int max = 0;
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null && product.getEstMakeMinutes() != null) {
                max = Math.max(max, product.getEstMakeMinutes());
            }
        }
        return max > 0 ? max : 15;
    }

    private void deductMaterials(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            List<ProductMaterial> boms = productMaterialMapper.selectList(
                    new LambdaQueryWrapper<ProductMaterial>()
                            .eq(ProductMaterial::getProductId, item.getProductId())
                            .and(w -> w.eq(ProductMaterial::getSkuId, item.getSkuId())
                                    .or().isNull(ProductMaterial::getSkuId)));
            for (ProductMaterial bom : boms) {
                Material material = materialMapper.selectById(bom.getMaterialId());
                if (material == null) {
                    continue;
                }
                BigDecimal used = bom.getQuantity().multiply(BigDecimal.valueOf(item.getQuantity()));
                // 条件更新：余量不够时影响行数为 0，避免并发下单把原料扣成负数
                if (materialMapper.deductStock(material.getId(), used) == 0) {
                    throw new BusinessException(ErrorCode.MATERIAL_NOT_ENOUGH,
                            "原料「" + material.getName() + "」库存不足");
                }
            }
        }
    }

    private void returnMaterials(Orders order) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        stockRollbackService.rollbackMaterials(items, "订单 " + order.getOrderNo() + " 取消回滚");
    }

    @Override
    @Transactional
    public OrderVO cancel(Long orderId, Long userId) {
        Orders order = getOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (order.getStatus() == ORDER_PENDING_PAY) {
            releaseSkuStock(order);
            return markCancelled(order);
        }
        // 标品支付后直接是「待取餐」：未取货前允许取消，退库存 + 退储值
        if (order.getStatus() == ORDER_READY && order.getOrderType() == TYPE_STANDARD) {
            releaseSkuStock(order);
            refundIfNeeded(order);
            return markCancelled(order);
        }
        if (order.getStatus() == ORDER_PAID) {
            if (order.getOrderType() == TYPE_FRESH) {
                OrderQueue queue = findQueue(orderId);
                if (queue == null || queue.getStatus() != QUEUE_WAITING) {
                    throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE);
                }
                // 支付后 60 秒内且未开始制作才可自助取消
                if (order.getPayTime() != null
                        && LocalDateTime.now().isAfter(order.getPayTime().plusSeconds(60))) {
                    throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE);
                }
                queue.setStatus(QUEUE_CANCELLED);
                orderQueueMapper.updateById(queue);
                returnMaterials(order);
            }
            // 下单时已锁 SKU 库存，取消统一回滚（标品 / 现制都适用）
            releaseSkuStock(order);
            refundIfNeeded(order);
            return markCancelled(order);
        }
        throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE);
    }

    /** 回滚下单时锁定的 SKU 库存（标品 / 现制通用），并写库存流水 */
    private void releaseSkuStock(Orders order) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        stockRollbackService.rollbackSkuStock(items, "订单 " + order.getOrderNo() + " 取消回滚");
    }

    /** 库存流水（bizType 1=商品SKU，changeType 1=入库 2=出库） */
    private void writeStockRecord(Long skuId, String productName, String skuSpec, int changeType,
                                  int qty, int beforeStock, int afterStock, String reason) {
        StockRecord record = new StockRecord();
        record.setBizType(1);
        record.setTargetId(skuId);
        String name = productName == null ? "商品" : productName;
        if (skuSpec != null && !skuSpec.isBlank()) {
            name = name + " / " + skuSpec;
        }
        record.setTargetName(name.length() > 150 ? name.substring(0, 150) : name);
        record.setChangeType(changeType);
        record.setChangeQty(BigDecimal.valueOf(changeType == 1 ? qty : -qty));
        record.setBeforeQty(BigDecimal.valueOf(beforeStock));
        record.setAfterQty(BigDecimal.valueOf(afterStock));
        record.setReason(reason);
        record.setOperatorId(AuthContext.userId());
        stockRecordMapper.insert(record);
    }

    private void refundIfNeeded(Orders order) {
        if (order.getBalancePaid() != null && order.getBalancePaid().compareTo(BigDecimal.ZERO) > 0) {
            balanceService.refund(order.getUserId(), order.getId(), order.getBalancePaid());
        }
    }

    private OrderVO markCancelled(Orders order) {
        order.setStatus(ORDER_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        ordersMapper.updateById(order);
        returnCoupon(order.getId());
        return buildDetail(order.getId());
    }

    /** 取消订单时退回已核销的优惠券 */
    private void returnCoupon(Long orderId) {
        UserCoupon userCoupon = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getOrderId, orderId)
                .last("limit 1"));
        if (userCoupon != null) {
            userCoupon.setStatus(0);
            userCoupon.setOrderId(null);
            userCouponMapper.updateById(userCoupon);
        }
    }

    @Override
    public OrderVO detail(Long orderId, Long viewerId, boolean admin) {
        Orders order = getOrder(orderId);
        assertOwner(order, viewerId, admin);
        return buildDetail(order);
    }

    /**
     * 订单归属校验：顾客只能访问自己的订单，管理员放行。
     * 越权时统一抛「订单不存在」，不区分「不存在」与「不是你的」，
     * 避免用订单号逐个探测他人订单。
     */
    private void assertOwner(Orders order, Long viewerId, boolean admin) {
        if (admin) {
            return;
        }
        if (viewerId == null || !viewerId.equals(order.getUserId())) {
            log.warn("订单越权访问已拦截：orderId={}, ownerId={}, viewerId={}",
                    order.getId(), order.getUserId(), viewerId);
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
    }

    /** 组装订单详情（不做归属校验，仅服务内部与已校验入口调用） */
    private OrderVO buildDetail(Long orderId) {
        return buildDetail(getOrder(orderId));
    }

    private OrderVO buildDetail(Orders order) {
        OrderVO vo = OrderVO.from(order);
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        vo.setItems(items.stream().map(OrderItemVO::from).toList());
        OrderQueue queue = findQueue(order.getId());
        if (queue != null) {
            vo.setQueue(QueueVO.from(queue));
        }
        return vo;
    }

    @Override
    public List<OrderVO> listByUser(Long userId) {
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .orderByDesc(Orders::getId));
        return orders.stream().map(o -> buildDetail(o.getId())).toList();
    }

    @Override
    public List<OrderVO> listByStore(Long storeId, Integer status, Integer orderType) {
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(storeId != null, Orders::getStoreId, storeId)
                .eq(status != null, Orders::getStatus, status)
                .eq(orderType != null, Orders::getOrderType, orderType)
                .orderByDesc(Orders::getId));
        return orders.stream().map(o -> buildDetail(o.getId())).toList();
    }

    @Override
    public List<QueueVO> queueList(Long storeId) {
        List<OrderQueue> queues = orderQueueMapper.selectList(new LambdaQueryWrapper<OrderQueue>()
                .eq(OrderQueue::getStoreId, storeId)
                .in(OrderQueue::getStatus, QUEUE_WAITING, QUEUE_MAKING, QUEUE_READY)
                .orderByAsc(OrderQueue::getQueueNo));
        if (queues.isEmpty()) {
            return List.of();
        }
        // 一次查完订单与明细，避免看板上每单都单独查库（N+1）
        List<Long> orderIds = queues.stream().map(OrderQueue::getOrderId).distinct().toList();
        Map<Long, Orders> orderMap = ordersMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Orders::getId, o -> o));
        Map<Long, List<OrderItem>> itemsByOrder = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        return queues.stream().map(queue -> {
            QueueVO vo = QueueVO.from(queue);
            Orders order = orderMap.get(queue.getOrderId());
            if (order != null) {
                OrderVO detail = OrderVO.from(order);
                detail.setItems(itemsByOrder.getOrDefault(order.getId(), List.of()).stream()
                        .map(OrderItemVO::from).toList());
                // 注意不要 detail.setQueue(vo)：会形成 order.queue.order... 循环引用
                vo.setOrder(detail);
            }
            return vo;
        }).toList();
    }

    @Override
    @Transactional
    public void startMaking(Long orderId) {
        Orders order = getOrder(orderId);
        if (order.getOrderType() != TYPE_FRESH || order.getStatus() != ORDER_PAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        OrderQueue queue = requireQueue(orderId);
        if (queue.getStatus() != QUEUE_WAITING) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(ORDER_MAKING);
        ordersMapper.updateById(order);
        queue.setStatus(QUEUE_MAKING);
        queue.setMakeStartTime(LocalDateTime.now());
        orderQueueMapper.updateById(queue);
    }

    @Override
    @Transactional
    public void finishMaking(Long orderId) {
        Orders order = getOrder(orderId);
        if (order.getOrderType() != TYPE_FRESH || order.getStatus() != ORDER_MAKING) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        OrderQueue queue = requireQueue(orderId);
        order.setStatus(ORDER_READY);
        ordersMapper.updateById(order);
        queue.setStatus(QUEUE_READY);
        queue.setMakeFinishTime(LocalDateTime.now());
        orderQueueMapper.updateById(queue);
        // 制作完成 → 站内取餐提醒
        noticeService.pushPickupReady(order);
    }

    @Override
    @Transactional
    public OrderVO complete(Long orderId) {
        Orders order = getOrder(orderId);
        if (order.getStatus() != ORDER_READY) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(ORDER_FINISHED);
        order.setFinishTime(LocalDateTime.now());
        ordersMapper.updateById(order);
        OrderQueue queue = findQueue(orderId);
        if (queue != null && queue.getStatus() == QUEUE_READY) {
            queue.setStatus(QUEUE_FINISHED);
            orderQueueMapper.updateById(queue);
        }
        return buildDetail(orderId);
    }

    @Override
    @Transactional
    public OrderVO verifyPickup(Long storeId, String pickupCode) {
        // 容错：扫码枪/复制粘贴可能带空格或首尾空白，统一去掉再匹配
        String code = pickupCode == null ? "" : pickupCode.replaceAll("\\s+", "");
        if (code.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入取餐码");
        }
        Orders order = ordersMapper.selectOne(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStoreId, storeId)
                .eq(Orders::getPickupCode, code)
                .last("limit 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != ORDER_READY) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(ORDER_FINISHED);
        order.setFinishTime(LocalDateTime.now());
        ordersMapper.updateById(order);
        OrderQueue queue = findQueue(order.getId());
        if (queue != null && queue.getStatus() == QUEUE_READY) {
            queue.setStatus(QUEUE_FINISHED);
            orderQueueMapper.updateById(queue);
        }
        return buildDetail(order.getId());
    }

    @Override
    public int cancelTimeoutOrders(int minutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(Math.max(1, minutes));
        List<Orders> timeoutOrders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, ORDER_PENDING_PAY)
                .lt(Orders::getCreateTime, deadline));
        int cancelled = 0;
        for (Orders order : timeoutOrders) {
            try {
                Boolean done = transactionTemplate.execute(status -> {
                    Orders current = getOrder(order.getId());
                    if (current.getStatus() != ORDER_PENDING_PAY) {
                        return false;
                    }
                    releaseSkuStock(current);
                    markCancelled(current);
                    return true;
                });
                if (Boolean.TRUE.equals(done)) {
                    cancelled++;
                }
            } catch (Exception e) {
                log.warn("自动取消超时订单失败: orderId={}, error={}", order.getId(), e.getMessage());
            }
        }
        return cancelled;
    }

    private Orders getOrder(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private OrderQueue requireQueue(Long orderId) {
        OrderQueue queue = findQueue(orderId);
        if (queue == null) {
            throw new BusinessException(ErrorCode.QUEUE_NOT_FOUND);
        }
        return queue;
    }

    private OrderQueue findQueue(Long orderId) {
        return orderQueueMapper.selectOne(new LambdaQueryWrapper<OrderQueue>()
                .eq(OrderQueue::getOrderId, orderId));
    }

    private String generateOrderNo() {
        return "SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 生成取餐码：同店「未完结」订单范围内不重复。
     * 否则两家顾客拿到同一个码，核销时只会命中先查到的那一单。
     * 6 位（10 万空间）既降低同店撞码概率，也避免 4 位被顾客/店员手滑输错仍命中他人订单。
     */
    private String generatePickupCode(Long storeId) {
        for (int i = 0; i < 30; i++) {
            String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
            Long used = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getStoreId, storeId)
                    .eq(Orders::getPickupCode, code)
                    .in(Orders::getStatus, ORDER_PENDING_PAY, ORDER_PAID,
                            ORDER_MAKING, ORDER_READY, ORDER_AFTER_SALE));
            if (used == null || used == 0) {
                return code;
            }
        }
        // 极端情况（未完结订单接近 90 万单）退回随机值，门店仍可按订单号核销
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
