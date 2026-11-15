-- =============================================================
-- 商品种子数据迁移脚本（对已存在的库执行一次即可）
-- 作用：给空的 product / product_sku / product_ingredient 表补演示数据
-- 执行方式：DataGrip 选中本文件执行，或命令行 source 本文件
-- =============================================================

USE shoping;

INSERT IGNORE INTO product (id, name, product_type, category_id, cover_img, detail, base_price, status, net_content, shelf_life, allergen, est_make_minutes, calories, sugar_note) VALUES
(1, '原味薯片',   1, 3, NULL, '薄脆原味薯片，追剧必备',     9.90,  1, '75g',   '9个月',  '小麦',       NULL, NULL, NULL),
(2, '夏威夷果',   1, 4, NULL, '奶油味夏威夷果，颗颗饱满',   29.90, 1, '200g',  '12个月', '坚果',       NULL, NULL, NULL),
(3, '榛仁巧克力', 1, 5, NULL, '香浓榛仁裹丝滑牛奶巧克力',   19.90, 1, '100g',  '12个月', '牛奶、大豆', NULL, NULL, NULL),
(4, '珍珠奶茶',   2, 6, NULL, '经典黑糖珍珠，茶香奶香浓郁', 15.00, 1, NULL, NULL, '牛奶',   5,  380, '甜度可选'),
(5, '美式咖啡',   2, 7, NULL, '现磨咖啡豆，醇苦回甘',       18.00, 1, NULL, NULL, '咖啡因', 3,  10,  '可选加糖'),
(6, '满杯水果茶', 2, 8, NULL, '多种鲜果拼配，清爽解腻',     16.00, 1, NULL, NULL, '无',     6,  220, '甜度可选');

INSERT IGNORE INTO product_sku (id, product_id, sku_code, price, stock, status) VALUES
(1, 1, '默认', 9.90,  200, 1),
(2, 2, '默认', 29.90, 150, 1),
(3, 3, '默认', 19.90, 120, 1),
(4, 4, '中杯', 15.00, 100, 1),
(5, 4, '大杯', 18.00, 100, 1),
(6, 5, '中杯', 18.00, 100, 1),
(7, 5, '大杯', 21.00, 100, 1),
(8, 6, '中杯', 16.00, 100, 1),
(9, 6, '大杯', 19.00, 100, 1);

INSERT IGNORE INTO product_ingredient (id, product_id, ingredient_id, price) VALUES
(1, 4, 1, 3),
(2, 4, 2, 2),
(3, 4, 3, 5),
(4, 5, 3, 5),
(5, 6, 1, 3),
(6, 6, 2, 2);
