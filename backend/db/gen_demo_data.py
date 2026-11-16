# -*- coding: utf-8 -*-
"""
生成演示数据 SQL（用户/订单/评价/积分/储值/库存/通知/售后），口径与真实代码逻辑一致：
- 积分=成长值=实付取整（MemberServiceImpl.onPaid），退款按 int(refund) 扣回
- 库存流水：下单扣 SKU，"订单 SOxxx 销售出库"，target_name = "商品名 / sku_code"
- 售后退款：全额→订单取消；部分退款→恢复原状态；退款回库存写"售后 #N（订单 SOxxx）退款回滚"
- 通知：现制"饮品已做好，请取餐" / 标品"已备货，请取餐"
产出：db/demo_data.sql + db/demo_data_rollback.sql
"""
import random
from datetime import datetime, timedelta

random.seed(20261115)

OUT = r"E:\编程\java\shoping\db\demo_data.sql"
OUT_RB = r"E:\编程\java\shoping\db\demo_data_rollback.sql"

PWD_HASH = "$2a$10$C.lk9ld1KSoTcpHyGqYEzOxjMfDspwp2.lgOI0Mcu4Upq3dR1e/sG"  # 同 user/123456

# ---------- 基线（当前 DB 实测值） ----------
BASE_STOCK = {
    1:196, 2:150, 3:120, 15:198, 20:149, 21:150, 22:199, 23:177, 24:196, 25:160, 26:140,
    4:101, 5:102, 6:110, 7:100, 8:110, 9:110, 16:110, 17:110, 18:100, 19:100,
    27:120, 28:120, 29:119, 30:120, 31:100, 32:100, 33:100, 34:99, 35:100, 36:99, 37:100, 38:100,
}
MEMBER_USER2 = (158, 158)  # 现有 user(id=2) 的 growth/points

# ---------- SKU 目录: sku_id -> (product_id, 名称, 单价, 类型, sku_code) ----------
SKU = {
    1:(1,"原味薯片",9.90,1,"默认"), 2:(2,"夏威夷果",29.90,1,"默认"), 3:(3,"榛仁巧克力",19.90,1,"默认"),
    15:(10,"海盐芝士坚果",29.90,1,"默认"), 20:(13,"黑巧克力曲奇",15.90,1,"默认"), 21:(14,"蜜汁猪肉脯",32.90,1,"默认"),
    22:(15,"海苔脆片",12.90,1,"默认"), 23:(16,"芒果干",22.90,1,"默认"), 24:(17,"芝士玉米棒",15.90,1,"默认"),
    25:(18,"白巧克力",18.90,1,"默认"), 26:(19,"牛轧糖",19.90,1,"默认"),
    4:(4,"珍珠奶茶",15.00,2,"中杯"), 5:(4,"珍珠奶茶",18.00,2,"大杯"), 6:(5,"美式咖啡",18.00,2,"中杯"),
    7:(5,"美式咖啡",21.00,2,"大杯"), 8:(6,"满杯水果茶",16.00,2,"中杯"), 9:(6,"满杯水果茶",19.00,2,"大杯"),
    16:(11,"抹茶拿铁",19.00,2,"中杯"), 17:(11,"抹茶拿铁",22.00,2,"大杯"), 18:(12,"满杯红西柚",17.00,2,"中杯"),
    19:(12,"满杯红西柚",20.00,2,"大杯"), 27:(20,"芋泥波波奶茶",18.00,2,"中杯"), 28:(20,"芋泥波波奶茶",21.00,2,"大杯"),
    29:(21,"黑糖珍珠鲜奶",16.00,2,"中杯"), 30:(21,"黑糖珍珠鲜奶",19.00,2,"大杯"), 31:(22,"焦糖玛奇朵",22.00,2,"中杯"),
    32:(22,"焦糖玛奇朵",25.00,2,"大杯"), 33:(23,"生椰拿铁",19.00,2,"中杯"), 34:(23,"生椰拿铁",22.00,2,"大杯"),
    35:(24,"青提柠檬茶",17.00,2,"中杯"), 36:(24,"青提柠檬茶",20.00,2,"大杯"), 37:(25,"草莓气泡饮",16.00,2,"中杯"),
    38:(25,"草莓气泡饮",19.00,2,"大杯"),
}
ING = {1:("珍珠",3.00), 2:("椰果",2.00), 3:("芝士奶盖",5.00)}

# ---------- 新用户（id 3-8，密码同 123456） ----------
USERS = [
    (3,"13800000001","chenchen","陈晨",   "2026-09-20 10:02:00"),
    (4,"13800000002","lihao","李昊",      "2026-10-05 12:33:00"),
    (5,"13800000003","wangsiyuan","王思远","2026-10-12 09:15:00"),
    (6,"13800000004","zhaomin","赵敏",    "2026-10-18 16:40:00"),
    (7,"13800000005","liuyang","刘洋",    "2026-11-01 11:08:00"),
    (8,"13800000006","sunyue","孙悦",     "2026-11-08 14:22:00"),
]
U2, UCHEN, ULIHAO, UWANG, UZHAO, ULIU, USUN = 2,3,4,5,6,7,8

def dt(s): return datetime.strptime(s, "%Y-%m-%d %H:%M:%S")

# ---------- 订单计划 ----------
# (order_id, 用户, 时间, 类型, items[(sku,qty,spec,加料)], pay_type, 场景)
PLAN = [
    (100,U2,  "2026-10-17 10:21:00",1,[(1,2,"",[])],1,None),
    (101,ULIHAO,"2026-10-18 20:15:00",2,[(31,1,"中杯/热/半糖/去冰",[])],1,None),
    (102,UCHEN,"2026-10-20 14:12:00",2,[(16,1,"中杯/冰/半糖/少冰",[])],2,None),
    (103,U2,  "2026-10-25 15:47:00",2,[(5,1,"大杯/冰/半糖/少冰",[(1,1)])],1,None),
    (104,UCHEN,"2026-10-26 18:40:00",2,[(29,1,"中杯/冰/全糖/正常冰",[(1,1)])],1,None),
    (105,UWANG,"2026-10-22 08:55:00",2,[(6,1,"中杯/冰/无糖/去冰",[])],1,None),
    (106,UZHAO,"2026-10-29 13:18:00",2,[(35,1,"中杯/冰/无糖/少冰",[])],1,None),
    (107,UCHEN,"2026-11-01 13:05:00",1,[(21,1,"",[]),(22,2,"",[])],1,None),
    (108,ULIHAO,"2026-11-02 12:50:00",1,[(24,3,"",[])],1,None),
    (109,UWANG,"2026-11-04 19:30:00",2,[(33,1,"中杯/冰/半糖/少冰",[])],1,None),
    (110,ULIU, "2026-11-05 10:30:00",2,[(17,1,"大杯/冰/无糖/去冰",[])],2,None),
    (111,U2,  "2026-11-08 19:03:00",2,[(27,1,"中杯/热/无糖/去冰",[])],1,None),
    (112,ULIU, "2026-11-08 18:22:00",2,[(37,1,"中杯/冰/全糖/正常冰",[(2,1)])],1,None),
    (113,UCHEN,"2026-11-09 16:22:00",2,[(4,1,"中杯/冰/无糖/去冰",[(3,1)])],1,None),
    (114,USUN, "2026-11-10 15:33:00",2,[(28,1,"大杯/热/半糖/去冰",[])],1,None),
    (115,ULIHAO,"2026-11-10 21:07:00",2,[(8,1,"中杯/冰/全糖/正常冰",[])],1,None),
    (116,UZHAO,"2026-11-11 14:05:00",1,[(26,1,"",[]),(25,1,"",[])],1,None),
    (117,USUN, "2026-11-12 20:01:00",2,[(32,1,"大杯/热/半糖/去冰",[(3,1)])],1,None),
    (118,UWANG,"2026-11-13 15:20:00",2,[(18,1,"中杯/冰/半糖/正常冰",[])],1,"AS_PENDING"),
    (119,U2,  "2026-11-14 12:30:00",1,[(2,1,"",[]),(23,1,"",[])],1,None),
    (120,UCHEN,"2026-11-14 20:15:00",1,[(2,2,"",[]),(24,2,"",[])],1,None),
    (121,UCHEN,"2026-11-12 11:38:00",1,[(1,1,"",[]),(22,1,"",[])],2,"AS_DONE"),
    (122,UCHEN,"2026-11-15 15:12:00",1,[(23,1,"",[]),(20,1,"",[])],None,"PENDING"),
    (123,ULIHAO,"2026-11-15 15:55:00",1,[(21,1,"",[]),(22,1,"",[])],1,"READY_STD"),
    (124,USUN, "2026-11-15 15:40:00",2,[(27,1,"中杯/热/半糖/去冰",[])],1,"READY_FRESH"),
    (125,UZHAO,"2026-11-15 16:05:00",2,[(5,1,"大杯/冰/半糖/少冰",[(1,1)])],None,"PENDING"),
    (126,ULIU, "2026-11-15 16:10:00",2,[(34,1,"大杯/冰/无糖/少冰",[])],2,"READY_FRESH"),
]

REMARKS = {103:"少冰", 109:"提神，麻烦做快一点", 113:"不要吸管", 107:"打包带走", 114:"第一次喝，求推荐"}

# ---------- 状态常量（与后端一致） ----------
S_PENDING, S_PAID, S_MAKING, S_READY, S_FINISHED, S_CANCELLED, S_AFTERSALE = 1,2,3,4,5,6,7
Q_WAIT, Q_MAKING, Q_READY, Q_DONE = 1,2,3,4

orders_sql, items_sql, queue_sql, notice_sql, review_sql, point_sql, bal_sql, stock_sql, member_sql, user_sql, as_sql = \
    [],[],[],[],[],[],[],[],[],[],[]

# ---- 追踪器 ----
stock_events = []   # (time, sku, delta, reason, operator)
queue_counter = 5   # 现库最大 5
item_id = 1000
notice_id = 1000
review_id = 1000
point_id = 1000
bal_id = 1000
queue_id = 1000
member_sum = {U2:[MEMBER_USER2[0],MEMBER_USER2[1]]}  # user -> [growth, points]
bal_state = {}   # user -> [principal, gift, total_recharge]

def q(s):
    return "'" + s.replace("\\","\\\\").replace("'","''") + "'"

def order_no(t, oid):
    rnd = 1000 + (oid - 100) * 37 % 9000
    return "SO" + t.strftime("%Y%m%d%H%M%S") + f"{rnd:04d}"

def money(x): return f"{x:.2f}"

# ---- 充值记录 ----
def recharge(user, t, principal, gift, remark):
    global bal_id, point_id
    p = bal_state.setdefault(user, [0.0, 0.0, 0.0])
    p[0] += principal; p[1] += gift; p[2] += principal
    bal_sql.append(
        f"({bal_id},{user},1,{money(principal)},{money(gift)},{money(p[0])},{money(p[1])},NULL,{q(remark)},{q(t.strftime('%Y-%m-%d %H:%M:%S'))})")
    bal_id += 1
    m = member_sum.setdefault(user, [0,0])
    m[0] += int(principal); m[1] += int(principal)
    point_sql.append(
        f"({point_id},{user},2,{int(principal)},{int(principal)},NULL,{q('充值得积分')},{q(t.strftime('%Y-%m-%d %H:%M:%S'))})")
    point_id += 1

recharge(UCHEN, dt("2026-10-20 10:05:00"), 500, 100, "充值500送100")
recharge(ULIU, dt("2026-11-05 09:40:00"), 300, 50, "充值300送50")

# ---- 储值消费/退款 ----
def balance_move(user, t, typ, amount, order_id, remark):
    """typ: 2 消费(-principal) 3 退款(+principal)"""
    global bal_id
    p = bal_state[user]
    delta = -amount if typ == 2 else amount
    p[0] = round(p[0] + delta, 2)
    bal_sql.append(
        f"({bal_id},{user},{typ},{money(delta)},0.00,{money(p[0])},{money(p[1])},{order_id},{q(remark)},{q(t.strftime('%Y-%m-%d %H:%M:%S'))})")
    bal_id += 1

# ---------- 生成订单 ----------
# 待取餐场景的取餐码（6 位，互不相同）
OPEN_CODES = {123:"461928", 124:"384216", 126:"592037"}

order_rows = {}
for oid, user, tstr, otype, items, pay_type, scenario in sorted(PLAN, key=lambda x: x[2]):
    t = dt(tstr)
    ono = order_no(t, oid)
    # 明细
    total = 0.0
    item_rows = []
    for sku, qty, spec, ings in items:
        pid, pname, price, _ptype, scode = SKU[sku]
        ing_names, ing_sum = [], 0.0
        for iid, _ in ings:
            ing_names.append(ING[iid][0]); ing_sum += ING[iid][1]
        unit = round(price + ing_sum, 2)
        amt = round(unit * qty, 2)
        total = round(total + amt, 2)
        item_rows.append((item_id, oid, pid, sku, pname, spec, "、".join(ing_names), unit, qty, amt))
        item_id += 1
        # 下单即扣 SKU + 流水（含未支付单）
        stock_events.append((t, sku, -qty, f"订单 {ono} 销售出库", user))
    order_rows[oid] = dict(ono=ono, user=user, t=t, otype=otype, total=total, items=item_rows,
                           pay_type=pay_type, scenario=scenario)

# ---- 逐单决定状态/时间线 ----
pickup_codes_used = set()
for oid, o in order_rows.items():
    t, otype, sc = o["t"], o["otype"], o["scenario"]
    ono = o["ono"]
    pay_t = None; finish_t = None; pickup = None; qno = None
    status = S_FINISHED
    if sc == "PENDING":
        status = S_PENDING
        pay_t = None
    else:
        pay_t = t + timedelta(seconds=random.randint(30, 90))
        if otype == 2:
            # 现制：入队
            queue_counter += 1; qno = queue_counter
        if sc == "READY_FRESH":
            status = S_READY
            pickup = OPEN_CODES[oid]
            mk_s = pay_t + timedelta(minutes=2)
            mk_f = pay_t + timedelta(minutes=8)
            queue_sql.append((queue_id, oid, 1, qno, Q_READY,
                              q(mk_s.strftime('%Y-%m-%d %H:%M:%S')), q(mk_f.strftime('%Y-%m-%d %H:%M:%S')),
                              8, q(pay_t.strftime('%Y-%m-%d %H:%M:%S')), q(mk_f.strftime('%Y-%m-%d %H:%M:%S'))))
            queue_id += 1
        elif sc == "READY_STD":
            status = S_READY
            pickup = OPEN_CODES[oid]
        elif sc == "AS_PENDING":
            status = S_AFTERSALE
            pickup = f"{random.randint(1000,9999)}"
            mk_s = pay_t + timedelta(minutes=2); mk_f = pay_t + timedelta(minutes=9)
            finish_t = mk_f + timedelta(minutes=12)
            queue_sql.append((queue_id, oid, 1, qno, Q_DONE,
                              q(mk_s.strftime('%Y-%m-%d %H:%M:%S')), q(mk_f.strftime('%Y-%m-%d %H:%M:%S')),
                              9, q(pay_t.strftime('%Y-%m-%d %H:%M:%S')), q(mk_f.strftime('%Y-%m-%d %H:%M:%S'))))
            queue_id += 1
        else:
            # 历史完结单
            pickup = f"{random.randint(1000,9999)}"
            while pickup in pickup_codes_used: pickup = f"{random.randint(1000,9999)}"
            pickup_codes_used.add(pickup)
            if otype == 2:
                mk_s = pay_t + timedelta(minutes=random.randint(1, 4))
                mk_f = pay_t + timedelta(minutes=random.randint(6, 14))
                finish_t = mk_f + timedelta(minutes=random.randint(5, 25))
                queue_sql.append((queue_id, oid, 1, qno, Q_DONE,
                                  q(mk_s.strftime('%Y-%m-%d %H:%M:%S')), q(mk_f.strftime('%Y-%m-%d %H:%M:%S')),
                                  10, q(pay_t.strftime('%Y-%m-%d %H:%M:%S')), q(mk_f.strftime('%Y-%m-%d %H:%M:%S'))))
                queue_id += 1
                o["mk_f"] = mk_f
            else:
                finish_t = pay_t + timedelta(minutes=random.randint(15, 45))
                o["mk_f"] = pay_t + timedelta(minutes=5)

    # 支付金额
    balance_paid = cash_paid = 0.0
    pay_type_val = "NULL"
    if status != S_PENDING:
        balance_paid = o["total"] if o["pay_type"] == 2 else 0.0
        cash_paid = 0.0 if o["pay_type"] == 2 else o["total"]
        pay_type_val = str(o["pay_type"])
    remark = REMARKS.get(oid, None)

    orders_sql.append(
        f"({oid},{q(ono)},{o['user']},1,{otype},{status},{money(o['total'])},0.00,{money(o['total'])},"
        f"{pay_type_val},{money(balance_paid)},{money(cash_paid)},"
        f"{q(pickup) if pickup else 'NULL'},{qno if qno else 'NULL'},"
        f"{q(remark) if remark else 'NULL'},"
        f"{q(pay_t.strftime('%Y-%m-%d %H:%M:%S')) if pay_t else 'NULL'},"
        f"{q(finish_t.strftime('%Y-%m-%d %H:%M:%S')) if finish_t else 'NULL'},NULL,"
        f"0,{q(t.strftime('%Y-%m-%d %H:%M:%S'))},{q(t.strftime('%Y-%m-%d %H:%M:%S'))})")

    # 明细行
    for row in o["items"]:
        iid, _oid, pid, sku, pname, spec, ings, unit, qty, amt = row
        items_sql.append(
            f"({iid},{oid},{pid},{sku},{q(pname)},{q(spec)},{q(ings)},{money(unit)},{qty},{money(amt)})")

    # 支付后副作用
    if status != S_PENDING:
        # 积分
        pts = int(o["total"])
        m = member_sum.setdefault(o["user"], [0,0])
        m[0] += pts; m[1] += pts
        point_sql.append(
            f"({point_id},{o['user']},1,{pts},{pts},{oid},{q('消费得积分')},{q(pay_t.strftime('%Y-%m-%d %H:%M:%S'))})")
        point_id += 1
        # 储值
        if o["pay_type"] == 2:
            balance_move(o["user"], pay_t, 2, o["total"], oid, "订单消费")
        # 取餐通知
        fresh = otype == 2
        title = "饮品已做好，请取餐" if fresh else "已备货，请取餐"
        content = f"取餐码 {pickup}"
        if qno: content += f" · 排队号 #{qno}"
        content += "，到店出示取餐码即可"
        read_flag = 0 if sc in ("READY_FRESH", "READY_STD") else 1
        ntime = o.get("mk_f", pay_t + timedelta(minutes=5)).strftime('%Y-%m-%d %H:%M:%S')
        notice_sql.append(f"({notice_id},{o['user']},1,{q(title)},{q(content)},{oid},{read_flag},{q(ntime)})")
        notice_id += 1

# ---- 售后两单 ----
# AS_DONE：订单 121（chenchen 储值支付 22.80，部分退款 9.90 → 订单恢复已完成）
o121 = order_rows[121]
refund_amt = 9.90
t_apply = dt("2026-11-13 09:12:00")
t_done = dt("2026-11-13 14:30:00")
as_sql.append(f"(1,121,{o121['items'][0][0]},5,{UCHEN},1,{q('薯片包装破损，申请退款')},NULL,2,{money(refund_amt)},{q('已退款至储值余额，感谢反馈')},{q(t_apply.strftime('%Y-%m-%d %H:%M:%S'))},{q(t_done.strftime('%Y-%m-%d %H:%M:%S'))})")
# 退款副作用：储值退回、扣回积分、库存回补
balance_move(UCHEN, t_done, 3, refund_amt, 121, "售后退款退回")
member_sum[UCHEN][0] -= 9; member_sum[UCHEN][1] -= 9
point_sql.append(f"({point_id},{UCHEN},5,-9,-9,121,{q('退款扣回积分')},{q(t_done.strftime('%Y-%m-%d %H:%M:%S'))})")
point_id += 1
stock_events.append((t_done, 1, +1, f"售后 #1（订单 {order_rows[121]['ono']}）退款回滚", 1))

# AS_PENDING：订单 118（wangsiyuan，待处理，订单转售后中 7）
t_apply2 = dt("2026-11-15 10:28:00")
as_sql.append(f"(2,118,{order_rows[118]['items'][0][0]},5,{UWANG},1,{q('饮品口味偏甜，与备注不符，申请部分退款')},NULL,0,NULL,NULL,{q(t_apply2.strftime('%Y-%m-%d %H:%M:%S'))},{q(t_apply2.strftime('%Y-%m-%d %H:%M:%S'))})")

# ---- 评价 ----
REVIEWS = [
    (103,5,"珍珠很Q弹，甜度刚好，已经回购好几次了",0,None),
    (111,5,"芋泥绵密，热的冬天喝很舒服",0,"感谢支持，欢迎常来～"),
    (102,4,"抹茶味很正，就是冰有点多",0,None),
    (104,5,"黑糖挂壁好看又好喝，性价比高",1,None),
    (113,4,"无糖选项很贴心，奶味很浓",1,None),
    (101,5,"焦糖香浓，出餐也快",0,None),
    (108,3,"味道可以，但等了快二十分钟",0,"抱歉让您久等，高峰期已加派人手，下次到店送您小食一份"),
    (115,5,"水果很多，很清爽，解腻",0,None),
    (105,4,"咖啡豆品质不错，下次试试大杯",0,None),
    (109,5,"椰香和咖啡很搭，连喝三天了",1,None),
    (117,4,"芝士奶盖加得很值，厚实",0,None),
    (106,4,"酸甜解腻，冰多了一点",0,None),
    (110,5,"大杯更过瘾，赶走了整个下午的困意",0,None),
    (112,5,"颜值高，气泡足，女生很喜欢",1,None),
    (114,5,"芋泥波波yyds，热饮首选",0,None),
    (120,5,"坚果很新鲜，家里囤货必买",0,None),
    (116,5,"牛轧糖不齁甜，老人家也爱吃",0,None),
]
for oid, rating, content, anon, reply in REVIEWS:
    iid = order_rows[oid]["items"][0][0]
    pid = order_rows[oid]["items"][0][2]
    user = order_rows[oid]["user"]
    rt = order_rows[oid]["t"] + timedelta(hours=random.randint(3, 30))
    reply_sql = "NULL"
    reply_time = "NULL"
    if reply:
        reply_sql = q(reply)
        reply_time = q((rt + timedelta(days=1)).strftime('%Y-%m-%d %H:%M:%S'))
    review_sql.append(
        f"({review_id},{oid},{iid},{pid},{user},{rating},{q(content)},{anon},1,{reply_sql},{reply_time},"
        f"{q(rt.strftime('%Y-%m-%d %H:%M:%S'))})")
    review_id += 1

# ---- 库存事件 → 流水（按时间排序，起点=当前库存-净变动） ----
net = {}
for _t, sku, delta, _r, _op in stock_events:
    net[sku] = net.get(sku, 0) + delta
running = {sku: BASE_STOCK[sku] - net.get(sku, 0) for sku in BASE_STOCK}
for ev in sorted(stock_events, key=lambda e: e[0]):
    t, sku, delta, reason, op = ev
    pid, pname, price, _pt, scode = SKU[sku]
    before = running[sku]
    after = before + delta
    running[sku] = after
    stock_sql.append(
        f"({1000+len(stock_sql)},1,{sku},{q(pname + ' / ' + scode)},{1 if delta>0 else 2},"
        f"{money(abs(delta))},{money(before)},{money(after)},{q(reason)},{op},"
        f"{q(t.strftime('%Y-%m-%d %H:%M:%S'))})")

# ---- 会员汇总 ----
def level(g):
    return 5 if g>=6000 else 4 if g>=3000 else 3 if g>=1500 else 2 if g>=500 else 1
LEVEL_T = {U2:3, UCHEN:4, ULIHAO:5, UWANG:6, UZHAO:7, ULIU:8, USUN:9}  # member 表新行 id
mid = LEVEL_T[U2]
m = member_sum[U2]
member_sql.append(f"UPDATE member SET growth_value={m[0]}, points={m[1]}, level={level(m[0])} WHERE user_id={U2};")
for u, mid in LEVEL_T.items():
    if u == U2: continue
    g, p = member_sum.get(u, [0,0])
    first_order = min((o["t"] for o in order_rows.values() if o["user"] == u), default=dt("2026-10-01 00:00:00"))
    member_sql.append(
        f"INSERT INTO member (id,user_id,level,growth_value,points,create_time,update_time) VALUES "
        f"({mid},{u},{level(g)},{g},{p},{q(first_order.strftime('%Y-%m-%d %H:%M:%S'))},{q('2026-11-15 16:00:00')});")

# ---- 用户 ----
last_login = {}
for o in order_rows.values():
    u = o["user"]
    if u >= 3 and (u not in last_login or o["t"] > last_login[u]):
        last_login[u] = o["t"]
user_sql.append(
    "INSERT INTO user (id,phone,username,password,role,nickname,status,create_time,update_time,last_login_time) VALUES\n  " +
    ",\n  ".join(
        f"({uid},{q(ph)},{q(un)},{q(PWD_HASH)},0,{q(nk)},1,{q(ct)},{q(ct)},{q(last_login.get(uid,'2026-11-15 12:00:00').strftime('%Y-%m-%d %H:%M:%S')) if last_login.get(uid) else 'NULL'})"
        for uid, ph, un, nk, ct in USERS) + ";")

# ---- 储值账户 ----
bal_rows = []
for u, p in bal_state.items():
    bal_rows.append(f"({u+1},{u},{money(p[0])},{money(p[1])},{money(p[2])},{q('2026-11-15 16:00:00')},{q('2026-11-15 16:00:00')})")
    # id：chenchen(3)->4? 现有 balance_account id 1,2 → 新 id 3,4
# 修正 id：直接用 3,4
bal_rows = []
for i, (u, p) in enumerate(bal_state.items()):
    bal_rows.append(f"({i+3},{u},{money(p[0])},{money(p[1])},{money(p[2])},{q('2026-11-15 16:00:00')},{q('2026-11-15 16:00:00')})")

# ---- 最终库存 UPDATE ----
stock_updates = "\n".join(
    f"UPDATE product_sku SET stock = {running[sku]} WHERE id = {sku};" for sku in sorted(running))

# ---------- 输出 SQL ----------
def val(rows, cols):
    return ",\n  ".join("(" + ",".join(str(c) if not isinstance(c, str) or c.startswith("'") else c for c in r) + ")" for r in rows)

sql = []
sql.append("-- 演示数据（生成于 2026-11-15）：6 个顾客 + 27 笔订单 + 评价/积分/储值/库存/通知/售后");
sql.append("-- 可重复执行前请先运行 demo_data_rollback.sql 回滚");
sql.append("START TRANSACTION;");
sql.append(user_sql[0])
sql.append("INSERT INTO member_point_record (id,user_id,type,points,growth,order_id,remark,create_time) VALUES\n  " +
           ",\n  ".join(point_sql) + ";")
sql.append("INSERT INTO balance_transaction (id,user_id,type,principal_amount,gift_amount,balance_principal,balance_gift,order_id,remark,create_time) VALUES\n  " +
           ",\n  ".join(bal_sql) + ";")
sql.append("INSERT INTO balance_account (id,user_id,principal,gift,total_recharge,create_time,update_time) VALUES\n  " +
           ",\n  ".join(bal_rows) + ";")
sql.append("\n".join(member_sql))
sql.append("INSERT INTO orders (id,order_no,user_id,store_id,order_type,status,total_amount,discount_amount,pay_amount,pay_type,balance_paid,cash_paid,pickup_code,queue_no,remark,pay_time,finish_time,cancel_time,deleted,create_time,update_time) VALUES\n  " +
           ",\n  ".join(orders_sql) + ";")
sql.append("INSERT INTO order_item (id,order_id,product_id,sku_id,product_name,sku_spec,ingredients,price,quantity,amount) VALUES\n  " +
           ",\n  ".join(items_sql) + ";")
sql.append("INSERT INTO order_queue (id,order_id,store_id,queue_no,status,make_start_time,make_finish_time,estimate_minutes,create_time,update_time) VALUES\n  " +
           ",\n  ".join(f"({r[0]},{r[1]},{r[2]},{r[3]},{r[4]},{r[5]},{r[6]},{r[7]},{r[8]},{r[9]})" for r in queue_sql) + ";")
sql.append("INSERT INTO user_notice (id,user_id,type,title,content,order_id,read_flag,create_time) VALUES\n  " +
           ",\n  ".join(notice_sql) + ";")
sql.append("INSERT INTO product_review (id,order_id,order_item_id,product_id,user_id,rating,content,anonymous,status,reply,reply_time,create_time) VALUES\n  " +
           ",\n  ".join(review_sql) + ";")
sql.append("INSERT INTO after_sale (id,order_id,order_item_id,prev_order_status,user_id,type,reason,image_urls,status,refund_amount,result,create_time,update_time) VALUES\n  " +
           ",\n  ".join(as_sql) + ";")
sql.append("INSERT INTO stock_record (id,biz_type,target_id,target_name,change_type,change_qty,before_qty,after_qty,reason,operator_id,create_time) VALUES\n  " +
           ",\n  ".join(stock_sql) + ";")
sql.append(stock_updates)
sql.append("COMMIT;")
sql.append("")
sql.append("-- ============ 一致性核对 ============")
sql.append("SELECT 'orders' k, COUNT(*) v FROM orders WHERE id BETWEEN 100 AND 126")
sql.append("UNION ALL SELECT 'items', COUNT(*) FROM order_item WHERE id >= 1000")
sql.append("UNION ALL SELECT 'reviews', COUNT(*) FROM product_review WHERE id >= 1000")
sql.append("UNION ALL SELECT 'notices', COUNT(*) FROM user_notice WHERE id >= 1000")
sql.append("UNION ALL SELECT 'queues', COUNT(*) FROM order_queue WHERE id >= 1000")
sql.append("UNION ALL SELECT 'stock_records', COUNT(*) FROM stock_record WHERE id >= 1000")
sql.append("UNION ALL SELECT 'point_records', COUNT(*) FROM member_point_record WHERE id >= 1000")
sql.append("UNION ALL SELECT 'users_new', COUNT(*) FROM user WHERE id BETWEEN 3 AND 8;")
sql.append("SELECT o.id, o.order_no, o.status, o.pay_type, o.pay_amount, o.pickup_code, o.queue_no FROM orders o WHERE o.id BETWEEN 100 AND 126 ORDER BY o.id;")
sql.append("SELECT user_id, level, growth_value, points FROM member ORDER BY user_id;")
sql.append("SELECT user_id, principal, gift, total_recharge FROM balance_account ORDER BY user_id;")
sql.append("SELECT m.user_id, SUM(r.points) growth_calc, mm.growth_value FROM member_point_record r JOIN member m ON m.user_id=r.user_id JOIN member mm ON mm.user_id=r.user_id GROUP BY m.user_id, mm.growth_value;")

with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(sql) + "\n")

# ---------- 回滚 SQL ----------
rb = ["-- 演示数据回滚：删除新增行并把库存/会员恢复基线", "START TRANSACTION;"]
rb.append("DELETE FROM orders WHERE id BETWEEN 100 AND 126;")
rb.append("DELETE FROM order_item WHERE order_id BETWEEN 100 AND 126;")
rb.append("DELETE FROM order_queue WHERE order_id BETWEEN 100 AND 126;")
rb.append("DELETE FROM user_notice WHERE order_id BETWEEN 100 AND 126;")
rb.append("DELETE FROM product_review WHERE order_id BETWEEN 100 AND 126;")
rb.append("DELETE FROM after_sale WHERE order_id BETWEEN 100 AND 126;")
rb.append("DELETE FROM stock_record WHERE id >= 1000;")
rb.append("DELETE FROM member_point_record WHERE id >= 1000;")
rb.append("DELETE FROM balance_transaction WHERE id >= 1000;")
rb.append("DELETE FROM balance_account WHERE user_id BETWEEN 3 AND 8;")
rb.append("DELETE FROM member WHERE user_id BETWEEN 3 AND 8;")
rb.append("DELETE FROM user WHERE id BETWEEN 3 AND 8;")
rb.append(f"UPDATE member SET growth_value={MEMBER_USER2[0]}, points={MEMBER_USER2[1]}, level=1 WHERE user_id={U2};")
rb.append("\n".join(f"UPDATE product_sku SET stock = {BASE_STOCK[sku]} WHERE id = {sku};" for sku in sorted(BASE_STOCK)))
rb.append("COMMIT;")
with open(OUT_RB, "w", encoding="utf-8") as f:
    f.write("\n".join(rb) + "\n")

print(f"orders={len(orders_sql)} items={len(items_sql)} queues={len(queue_sql)} notices={len(notice_sql)} "
      f"reviews={len(review_sql)} points={len(point_sql)} balance_txn={len(bal_sql)} stock_records={len(stock_sql)}")
print(f"member summary: {member_sum}")
print(f"final balance: {bal_state}")
print(f"stock delta: { {k: net.get(k,0) for k in sorted(net) if net.get(k,0)} }")
