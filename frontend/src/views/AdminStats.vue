<template>
  <div class="admin-stats" v-loading="loading">
    <!-- 顶部：时间范围 -->
    <div class="head">
      <div class="head-left">
        <span class="head-sub">统计口径：已支付订单按支付时间归集，未支付 / 已取消不计入销售额</span>
      </div>
      <div class="head-right">
        <el-radio-group v-model="days" size="default" @change="load">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="14">近 14 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <!-- KPI -->
    <div class="kpi">
      <div class="kpi-card">
        <span class="kpi-label">今日销售额</span>
        <b class="kpi-value">¥{{ fmtMoney(data.todaySales) }}</b>
        <span class="kpi-rate" :class="rateClass(data.salesRate)">
          {{ rateText(data.salesRate) }} <small>较昨日</small>
        </span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">今日支付订单</span>
        <b class="kpi-value">{{ data.todayOrders }}<small> 笔</small></b>
        <span class="kpi-rate" :class="rateClass(data.ordersRate)">
          {{ rateText(data.ordersRate) }} <small>较昨日</small>
        </span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">客单价</span>
        <b class="kpi-value">¥{{ fmtMoney(data.todayAvgTicket) }}</b>
        <span class="kpi-sub">昨日 ¥{{ fmtMoney(avgYesterday) }}</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">今日新建订单</span>
        <b class="kpi-value">{{ data.todayCreated }}<small> 笔</small></b>
        <span class="kpi-sub">含待支付 / 已取消</span>
      </div>
    </div>

    <!-- 区间经营概览 -->
    <div class="kpi biz">
      <div v-for="m in bizMetrics" :key="m.label" class="kpi-card">
        <span class="kpi-label">{{ m.label }}</span>
        <b class="kpi-value" :class="m.cls">{{ m.text }}</b>
        <span class="kpi-sub">{{ m.sub }}</span>
      </div>
    </div>

    <!-- 趋势 -->
    <el-card shadow="never" class="block">
      <template #header>
        <div class="block-head">
          <b>销售趋势</b>
          <span class="block-sub">
            近 {{ data.days || days }} 天合计 ¥{{ fmtMoney(data.periodSales) }} · {{ data.periodOrders }} 笔
          </span>
        </div>
      </template>

      <div class="trend-wrap" v-if="points.length">
        <svg class="trend-svg" viewBox="0 0 760 260" preserveAspectRatio="none">
          <defs>
            <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#2f6bff" stop-opacity="0.34" />
              <stop offset="100%" stop-color="#2f6bff" stop-opacity="0.02" />
            </linearGradient>
          </defs>

          <!-- 网格与 Y 轴 -->
          <g v-for="t in yTicks" :key="'grid' + t.value">
            <line :x1="pad.l" :x2="760 - pad.r" :y1="t.y" :y2="t.y" stroke="#e6ecf9" stroke-width="1" />
            <text :x="pad.l - 8" :y="t.y + 4" text-anchor="end" class="axis-text">
              {{ t.label }}
            </text>
          </g>

          <!-- 面积 + 折线 -->
          <path :d="areaPath" fill="url(#areaFill)" />
          <path :d="linePath" fill="none" stroke="#2f6bff" stroke-width="2.4"
                stroke-linejoin="round" stroke-linecap="round" />

          <!-- 数据点 + 悬浮热区 -->
          <g v-for="(p, i) in points" :key="'pt' + i">
            <circle :cx="p.x" :cy="p.y" :r="hoverIndex === i ? 5 : 3"
                    :fill="hoverIndex === i ? '#1e4fd6' : '#fff'"
                    stroke="#2f6bff" stroke-width="2" />
            <rect :x="p.x - step / 2" :y="pad.t" :width="step" :height="chartH"
                  fill="transparent" @mouseenter="hoverIndex = i" @mouseleave="hoverIndex = -1" />
          </g>

          <!-- X 轴日期 -->
          <text v-for="i in xTickIndexes" :key="'xt' + i"
                :x="points[i].x" :y="260 - 8" text-anchor="middle" class="axis-text">
            {{ shortDate(points[i].date) }}
          </text>
        </svg>

        <div v-if="hoverPoint" class="chart-tip"
             :style="{ left: tipLeft }">
          <div class="tip-date">{{ hoverPoint.date }}</div>
          <div class="tip-row"><span>销售额</span><b>¥{{ fmtMoney(hoverPoint.sales) }}</b></div>
          <div class="tip-row"><span>订单</span><b>{{ hoverPoint.orders }} 笔</b></div>
        </div>
      </div>
      <el-empty v-else description="暂无销售数据" :image-size="80" />
    </el-card>

    <!-- 时段分布 -->
    <el-card shadow="never" class="block">
      <template #header>
        <div class="block-head">
          <b>时段分布</b>
          <span class="block-sub">
            按支付时间统计 · {{ peakHour === null ? '区间内暂无支付' : `高峰 ${String(peakHour).padStart(2, '0')}:00-${String((peakHour + 1) % 24).padStart(2, '0')}:00` }}
          </span>
        </div>
      </template>
      <div v-if="hourTotalOrders" class="hour-wrap">
        <svg class="hour-svg" viewBox="0 0 760 210" preserveAspectRatio="none">
          <g v-for="t in hourTicks" :key="'hg' + t.value">
            <line :x1="hourPad.l" :x2="760 - hourPad.r" :y1="t.y" :y2="t.y" stroke="#e6ecf9" stroke-width="1" />
            <text :x="hourPad.l - 8" :y="t.y + 4" text-anchor="end" class="axis-text">{{ t.label }}</text>
          </g>

          <g v-for="(b, i) in hourBars" :key="'hb' + i">
            <rect :x="b.x" :y="b.y" :width="b.w" :height="b.h" rx="4"
                  :fill="hoverHour === b.hour ? '#1e4fd6' : (b.peak ? '#2f6bff' : '#a9c1ff')"
                  @mouseenter="hoverHour = b.hour" @mouseleave="hoverHour = -1" />
            <text v-if="b.hour % 3 === 0" :x="b.x + b.w / 2" :y="210 - 8"
                  text-anchor="middle" class="axis-text">{{ b.hour }}</text>
          </g>
        </svg>
        <div v-if="hoverHourInfo" class="chart-tip" :style="{ left: hourTipLeft }">
          <div class="tip-date">{{ hoverHourInfo.label }}</div>
          <div class="tip-row"><span>销售额</span><b>¥{{ fmtMoney(hoverHourInfo.sales) }}</b></div>
          <div class="tip-row"><span>订单</span><b>{{ hoverHourInfo.orders }} 笔</b></div>
        </div>
      </div>
      <el-empty v-else description="区间内暂无支付记录" :image-size="80" />
    </el-card>

    <!-- 支付方式 + 订单类型 -->
    <div class="two-col">
      <el-card shadow="never" class="block">
        <template #header><b>支付方式占比</b></template>
        <div v-if="paySegments.length" class="donut-wrap">
          <svg viewBox="0 0 200 200" class="donut">
            <circle cx="100" cy="100" :r="radius" fill="none" stroke="#eef2fb" :stroke-width="strokeW" />
            <circle v-for="(s, i) in paySegments" :key="'seg' + i"
                    cx="100" cy="100" :r="radius" fill="none"
                    :stroke="s.color" :stroke-width="strokeW"
                    :stroke-dasharray="s.dash" :stroke-dashoffset="s.offset"
                    transform="rotate(-90 100 100)" stroke-linecap="butt" />
            <text x="100" y="94" text-anchor="middle" class="donut-label">区间销售额</text>
            <text x="100" y="118" text-anchor="middle" class="donut-value">¥{{ compactMoney(periodPayTotal) }}</text>
          </svg>
          <div class="legend">
            <div v-for="(s, i) in paySegments" :key="'lg' + i" class="legend-row">
              <span class="dot" :style="{ background: s.color }" />
              <span class="lg-name">{{ s.label }}</span>
              <span class="lg-amount">¥{{ fmtMoney(s.amount) }}</span>
              <span class="lg-pct">{{ s.pct }}%</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="区间内暂无支付记录" :image-size="70" />
      </el-card>

      <el-card shadow="never" class="block">
        <template #header><b>订单类型对比</b></template>
        <div class="type-list" v-if="orderTypeRows.length">
          <div v-for="t in orderTypeRows" :key="t.key" class="type-row">
            <div class="type-head">
              <span class="type-name">{{ t.label }}</span>
              <span class="type-num">{{ t.orders }} 笔 · ¥{{ fmtMoney(t.amount) }}</span>
            </div>
            <div class="type-bar">
              <span :style="{ width: t.pct + '%', background: t.color }" />
            </div>
            <div class="type-pct">{{ t.pct }}%</div>
          </div>
        </div>
        <el-empty v-else description="区间内暂无订单" :image-size="70" />
      </el-card>
    </div>

    <!-- 热销榜 + 分类结构 -->
    <div class="two-col">
      <el-card shadow="never" class="block">
        <template #header>
          <div class="block-head">
            <b>热销商品 TOP 10</b>
            <span class="block-sub">按销量排序</span>
          </div>
        </template>
        <div v-if="topList.length" class="top-list">
          <div v-for="(p, i) in topList" :key="p.name + i" class="top-row">
            <span class="rank" :class="{ hot: i < 3 }">{{ i + 1 }}</span>
            <div class="top-main">
              <div class="top-line">
                <span class="top-name">{{ p.name }}</span>
                <span class="top-num">{{ p.qty }} 件 · ¥{{ fmtMoney(p.amount) }}</span>
              </div>
              <div class="top-bar"><span :style="{ width: p.width + '%' }" /></div>
            </div>
          </div>
        </div>
        <el-empty v-else description="区间内暂无销量" :image-size="70" />
      </el-card>

      <el-card shadow="never" class="block">
        <template #header>
          <div class="block-head">
            <b>分类销售结构</b>
            <span class="block-sub">按金额占比</span>
          </div>
        </template>
        <div v-if="categoryRows.length" class="type-list cat-list">
          <div v-for="c in categoryRows" :key="c.name" class="type-row">
            <div class="type-head">
              <span class="type-name">{{ c.name }}</span>
              <span class="type-num">{{ c.qty }} 件 · ¥{{ fmtMoney(c.amount) }}</span>
            </div>
            <div class="type-bar">
              <span :style="{ width: c.pct + '%', background: c.color }" />
            </div>
            <div class="type-pct">{{ c.pct }}%</div>
          </div>
        </div>
        <el-empty v-else description="区间内暂无销量" :image-size="70" />
      </el-card>
    </div>

    <!-- 会员与售后 + 状态分布 -->
    <div class="two-col">
      <el-card shadow="never" class="block">
        <template #header>
          <div class="block-head">
            <b>会员与售后</b>
            <span class="block-sub">近 {{ data.days || days }} 天</span>
          </div>
        </template>

        <div class="member-grid">
          <div class="mg-item">
            <span>消费会员</span>
            <b>{{ data.periodBuyers }} <small>人</small></b>
          </div>
          <div class="mg-item">
            <span>人均消费</span>
            <b>¥{{ fmtMoney(data.periodAvgPerBuyer) }}</b>
          </div>
          <div class="mg-item">
            <span>新客</span>
            <b>{{ data.periodNewBuyers }} <small>人</small></b>
          </div>
          <div class="mg-item">
            <span>退款金额</span>
            <b>¥{{ fmtMoney(data.periodRefundAmount) }}</b>
          </div>
        </div>

        <div class="newold">
          <div class="no-head">
            <span>新客占比</span>
            <b>{{ newPct }}%</b>
          </div>
          <div class="no-bar">
            <span class="no-new" :style="{ width: newPct + '%' }" />
            <span class="no-old" :style="{ width: 100 - newPct + '%' }" />
          </div>
          <div class="no-legend">
            <span><i class="dot new" />新客 {{ data.periodNewBuyers }} 人</span>
            <span><i class="dot old" />老客 {{ repeatBuyers }} 人</span>
          </div>
        </div>

        <div class="refund-line">
          退款率 <b>{{ Number(data.periodRefundRate || 0).toFixed(1) }}%</b>
          <span>退款 ¥{{ fmtMoney(data.periodRefundAmount) }} / 毛销售额 ¥{{ fmtMoney(data.periodGrossSales) }}
            · 完成售后 {{ data.periodRefundOrders }} 单</span>
        </div>
      </el-card>

      <el-card shadow="never" class="block">
        <template #header>
          <div class="block-head">
            <b>订单状态分布</b>
            <span class="block-sub">近 {{ data.days || days }} 天创建</span>
          </div>
        </template>
        <div v-if="statusRows.length" class="status-list">
          <div v-for="s in statusRows" :key="s.status" class="status-row">
            <span class="st-name">{{ s.label }}</span>
            <div class="st-bar"><span :style="{ width: s.pct + '%', background: s.color }" /></div>
            <span class="st-num">{{ s.orders }}</span>
          </div>
        </div>
        <el-empty v-else description="区间内暂无订单" :image-size="70" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getSalesDashboard } from '../api/admin'
import { useAppStore } from '../stores/app'
import { fmtMoney } from '../utils/format'
import { ORDER_STATUS, PAY_TYPE } from '../utils/dict'

const app = useAppStore()
const days = ref(7)
const loading = ref(false)
const data = ref({
  days: 7,
  todaySales: 0, todayOrders: 0, todayAvgTicket: 0, todayCreated: 0,
  yesterdaySales: 0, yesterdayOrders: 0, salesRate: 0, ordersRate: 0,
  periodSales: 0, periodOrders: 0,
  periodBuyers: 0, periodNewBuyers: 0, periodAvgPerBuyer: 0,
  periodGrossSales: 0, periodRefundAmount: 0, periodRefundRate: 0, periodRefundOrders: 0,
  trend: [], hourDist: [], categoryStats: [], topProducts: [],
  payTypes: [], orderTypes: [], statusDist: []
})

const PAD = { l: 52, r: 18, t: 18, b: 32 }
const W = 760
const H = 260
const pad = PAD
const chartH = H - PAD.t - PAD.b

const avgYesterday = computed(() => {
  const n = Number(data.value.yesterdayOrders || 0)
  return n > 0 ? Number(data.value.yesterdaySales || 0) / n : 0
})

const rateText = (r) => {
  const v = Number(r || 0)
  if (v === 0) return '持平'
  return `${v > 0 ? '↑' : '↓'} ${Math.abs(v).toFixed(1)}%`
}
const rateClass = (r) => {
  const v = Number(r || 0)
  return v > 0 ? 'up' : v < 0 ? 'down' : 'flat'
}

const compactMoney = (v) => {
  const n = Number(v || 0)
  if (n >= 10000) return (n / 10000).toFixed(1) + ' 万'
  return n.toFixed(0)
}

const shortDate = (d) => (d ? String(d).slice(5) : '')

/** Y 轴取一个略高于峰值的整数上限（1/1.2/1.5/2/2.5/3/4/5/6/8/10 × 10^n） */
const niceCeil = (raw) => {
  if (!raw || raw <= 0) return 1
  const pow = Math.pow(10, Math.floor(Math.log10(raw)))
  const unit = raw / pow
  const steps = [1, 1.2, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10]
  const step = steps.find((s) => s >= unit - 1e-9) || 10
  return step * pow
}

// ===== 区间经营指标 =====
const avgTicket = computed(() => {
  const orders = Number(data.value.periodOrders || 0)
  return orders > 0 ? Number(data.value.periodSales || 0) / orders : 0
})

const newPct = computed(() => {
  const buyers = Number(data.value.periodBuyers || 0)
  if (!buyers) return 0
  return Math.min(100, Math.round((Number(data.value.periodNewBuyers || 0) / buyers) * 100))
})
const repeatBuyers = computed(() =>
  Math.max(0, Number(data.value.periodBuyers || 0) - Number(data.value.periodNewBuyers || 0))
)

const bizMetrics = computed(() => {
  const d = data.value
  return [
    {
      label: '区间净销售额',
      text: `¥${fmtMoney(d.periodSales)}`,
      sub: `毛销售 ¥${fmtMoney(d.periodGrossSales)}`
    },
    {
      label: '支付订单',
      text: `${d.periodOrders} 笔`,
      sub: `客单价 ¥${fmtMoney(avgTicket.value)}`
    },
    {
      label: '消费会员',
      text: `${d.periodBuyers} 人`,
      sub: `人均 ¥${fmtMoney(d.periodAvgPerBuyer)}`
    },
    {
      label: '新客占比',
      text: `${newPct.value}%`,
      sub: `新客 ${d.periodNewBuyers} 人 · 老客 ${repeatBuyers.value} 人`
    },
    {
      label: '退款率',
      text: `${Number(d.periodRefundRate || 0).toFixed(1)}%`,
      sub: `¥${fmtMoney(d.periodRefundAmount)} / ${d.periodRefundOrders} 单`,
      cls: Number(d.periodRefundRate || 0) >= 5 ? 'warn' : ''
    }
  ]
})

// ===== 时段分布 =====
const hourPad = { l: 44, r: 12, t: 16, b: 28 }
const hourChartH = 210 - hourPad.t - hourPad.b
const hoverHour = ref(-1)

const hours = computed(() => data.value.hourDist || [])
const hourTotalOrders = computed(() => hours.value.reduce((s, h) => s + Number(h.orders || 0), 0))
const hourMaxSales = computed(() => Math.max(1, ...hours.value.map((h) => Number(h.sales || 0))))
const peakHour = computed(() => {
  const top = hours.value.reduce((best, h) =>
    Number(h.sales || 0) > Number(best?.sales || 0) ? h : best, null)
  return top && Number(top.sales || 0) > 0 ? top.statHour : null
})

/** Y 轴取整上限 */
const hourNiceMax = computed(() => niceCeil(hourMaxSales.value))

const hourBars = computed(() => {
  const usable = 760 - hourPad.l - hourPad.r
  const slot = usable / 24
  const w = Math.max(6, slot * 0.56)
  const max = hourNiceMax.value
  return hours.value.map((h, i) => {
    const sales = Number(h.sales || 0)
    const ratio = Math.min(1, sales / max)
    const barH = sales > 0 ? Math.max(3, ratio * hourChartH) : 0
    return {
      hour: h.statHour,
      orders: Number(h.orders || 0),
      sales,
      x: hourPad.l + slot * i + (slot - w) / 2,
      y: hourPad.t + hourChartH - barH,
      w,
      h: barH,
      peak: h.statHour === peakHour.value
    }
  })
})

const hourTicks = computed(() => {
  const max = hourNiceMax.value
  const count = 3
  return Array.from({ length: count + 1 }, (_, i) => {
    const value = (max / count) * i
    return {
      value,
      y: hourPad.t + hourChartH - (value / max) * hourChartH,
      label: value >= 10000 ? (value / 10000).toFixed(1) + '万' : String(Math.round(value))
    }
  })
})

const hoverHourInfo = computed(() => {
  if (hoverHour.value < 0) return null
  const h = hours.value.find((x) => x.statHour === hoverHour.value)
  if (!h) return null
  return {
    label: `${String(h.statHour).padStart(2, '0')}:00 - ${String((h.statHour + 1) % 24).padStart(2, '0')}:00`,
    orders: Number(h.orders || 0),
    sales: Number(h.sales || 0)
  }
})

const hourTipLeft = computed(() => {
  const i = hoverHour.value
  if (i < 0) return '0%'
  const usable = 760 - hourPad.l - hourPad.r
  const pct = ((hourPad.l + (usable / 24) * (i + 0.5)) / 760) * 100
  return `${Math.min(92, Math.max(8, pct))}%`
})

// ===== 分类结构 =====
const CAT_COLORS = ['#2f6bff', '#5b8def', '#3f9d63', '#c77c12', '#8b5cf6', '#9aa8c2', '#d5485f']
const categoryRows = computed(() => {
  const list = data.value.categoryStats || []
  const total = list.reduce((s, c) => s + Number(c.amount || 0), 0) || 1
  return list.slice(0, 7).map((c, i) => ({
    name: c.name || '未分类',
    qty: c.qty,
    amount: Number(c.amount || 0),
    pct: ((Number(c.amount || 0) / total) * 100).toFixed(1),
    color: CAT_COLORS[i % CAT_COLORS.length]
  }))
})

// ===== 趋势图 =====
const maxSales = computed(() => {
  const list = data.value.trend || []
  return Math.max(1, ...list.map((p) => Number(p.sales || 0)))
})

/** 取一个好看的 Y 轴上限 */
const niceMax = computed(() => niceCeil(maxSales.value))

const step = computed(() => {
  const n = points.value.length
  return n > 1 ? (W - PAD.l - PAD.r) / (n - 1) : W - PAD.l - PAD.r
})

const points = computed(() => {
  const list = data.value.trend || []
  const n = list.length
  const max = niceMax.value
  const usableW = W - PAD.l - PAD.r
  return list.map((p, i) => {
    const x = n > 1 ? PAD.l + (usableW / (n - 1)) * i : PAD.l + usableW / 2
    const ratio = Math.min(1, Number(p.sales || 0) / max)
    const y = PAD.t + chartH - ratio * chartH
    return { x, y, date: p.statDate, sales: Number(p.sales || 0), orders: Number(p.orders || 0) }
  })
})

const linePath = computed(() =>
  points.value.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
)

const areaPath = computed(() => {
  if (!points.value.length) return ''
  const bottom = PAD.t + chartH
  const first = points.value[0]
  const last = points.value[points.value.length - 1]
  return `${linePath.value} L${last.x.toFixed(1)},${bottom} L${first.x.toFixed(1)},${bottom} Z`
})

const yTicks = computed(() => {
  const max = niceMax.value
  const count = 3
  return Array.from({ length: count + 1 }, (_, i) => {
    const value = (max / count) * i
    return {
      value,
      y: PAD.t + chartH - (value / max) * chartH,
      label: value >= 10000 ? (value / 10000).toFixed(1) + '万' : String(Math.round(value))
    }
  })
})

const hoverIndex = ref(-1)
const hoverPoint = computed(() => (hoverIndex.value >= 0 ? points.value[hoverIndex.value] : null))

// tooltip 限制在图表内，避免首尾点被裁掉
const tipLeft = computed(() => {
  const p = hoverPoint.value
  if (!p) return '0%'
  const pct = (p.x / 760) * 100
  return `${Math.min(90, Math.max(10, pct))}%`
})

const xTickIndexes = computed(() => {
  const n = points.value.length
  if (!n) return []
  const count = n <= 8 ? n : 7
  const gap = Math.max(1, Math.round((n - 1) / (count - 1)))
  const idx = []
  for (let i = 0; i < n; i += gap) idx.push(i)
  if (idx[idx.length - 1] !== n - 1) idx.push(n - 1)
  return idx
})

// ===== 支付方式环形图 =====
const SEG_COLORS = ['#2f6bff', '#5b7fff', '#8fb0ff', '#3f9d63', '#c77c12', '#9aa8c2']
const radius = 70
const strokeW = 24
const CIRC = 2 * Math.PI * radius

const paySegments = computed(() => {
  const list = (data.value.payTypes || []).filter((p) => Number(p.amount || 0) > 0)
  const total = list.reduce((s, p) => s + Number(p.amount || 0), 0)
  if (!total) return []
  let acc = 0
  return list.map((p, i) => {
    const amount = Number(p.amount || 0)
    const len = (amount / total) * CIRC
    const seg = {
      label: PAY_TYPE[p.payType] || '其他',
      amount,
      pct: ((amount / total) * 100).toFixed(1),
      color: SEG_COLORS[i % SEG_COLORS.length],
      dash: `${len} ${CIRC - len}`,
      offset: -acc
    }
    acc += len
    return seg
  })
})

const periodPayTotal = computed(() => Number(data.value.periodSales || 0))

// ===== 订单类型 =====
const orderTypeRows = computed(() => {
  const list = data.value.orderTypes || []
  const total = list.reduce((s, t) => s + Number(t.amount || 0), 0) || 1
  return list.map((t) => ({
    key: t.orderType,
    label: t.orderType === 2 ? '现制饮品' : '标品零食',
    orders: t.orders,
    amount: t.amount,
    pct: ((Number(t.amount || 0) / total) * 100).toFixed(1),
    color: t.orderType === 2 ? '#5b7fff' : '#2f6bff'
  }))
})

// ===== 热销榜 =====
const topList = computed(() => {
  const list = data.value.topProducts || []
  const max = Math.max(1, ...list.map((p) => Number(p.qty || 0)))
  return list.map((p) => ({
    name: p.name || '未知商品',
    qty: p.qty,
    amount: p.amount,
    width: Math.max(4, (Number(p.qty || 0) / max) * 100)
  }))
})

// ===== 状态分布 =====
const STATUS_COLORS = {
  1: '#c77c12', 2: '#2f6bff', 3: '#5b7fff', 4: '#3f9d63',
  5: '#7d8db1', 6: '#b9c3d6', 7: '#d5485f'
}
const statusRows = computed(() => {
  const list = data.value.statusDist || []
  const total = list.reduce((s, x) => s + Number(x.orders || 0), 0) || 1
  return list.map((s) => ({
    status: s.status,
    label: ORDER_STATUS[s.status]?.text || '未知',
    orders: s.orders,
    pct: Math.max(3, (Number(s.orders || 0) / total) * 100),
    color: STATUS_COLORS[s.status] || '#9aa8c2'
  }))
})

async function load() {
  loading.value = true
  try {
    data.value = await getSalesDashboard({ storeId: app.storeId, days: days.value })
    hoverIndex.value = -1
  } catch (e) {
    /* 已提示 */
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  app.loadStore()
  load()
})
</script>

<style scoped>
.admin-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.head-title {
  font-size: 17px;
  color: var(--ink);
  margin-right: 10px;
}

.head-sub {
  font-size: 12px;
  color: var(--muted);
}

.head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* KPI */
.kpi {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
}

.kpi-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(255, 255, 255, 0.68);
  box-shadow: 0 6px 18px rgba(47, 107, 255, 0.07);
}

.kpi-label {
  font-size: 12.5px;
  color: var(--muted);
}

.kpi-value {
  font-size: 26px;
  line-height: 1.2;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.kpi-value small {
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
}

.kpi-rate {
  font-size: 12.5px;
  font-weight: 600;
}

.kpi-rate small {
  font-weight: 400;
  color: var(--muted);
}

.kpi-rate.up {
  color: #d5485f;
}

.kpi-rate.down {
  color: #3f9d63;
}

.kpi-rate.flat,
.kpi-sub {
  color: var(--muted);
  font-size: 12.5px;
  font-weight: 400;
}

/* 卡片 */
.block-head {  display: flex;
  align-items: baseline;
  gap: 12px;
}

.block-sub {
  font-size: 12px;
  color: var(--muted);
}

/* 趋势图 */
.trend-wrap {
  position: relative;
}

.trend-svg {
  width: 100%;
  height: 260px;
  display: block;
}

.axis-text {
  font-size: 11px;
  fill: #8b98b3;
}

.chart-tip {
  position: absolute;
  top: 6px;
  transform: translateX(-50%);
  min-width: 132px;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(24, 43, 84, 0.92);
  color: #fff;
  font-size: 12px;
  pointer-events: none;
  box-shadow: 0 8px 20px rgba(20, 40, 80, 0.28);
}

.tip-date {
  font-size: 11.5px;
  opacity: 0.75;
  margin-bottom: 4px;
}

.tip-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  line-height: 1.7;
}

/* 两列 */
.two-col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

/* 环形图 */
.donut-wrap {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.donut {
  width: 178px;
  height: 178px;
  flex-shrink: 0;
}

.donut-label {
  font-size: 11px;
  fill: #8b98b3;
}

.donut-value {
  font-size: 16px;
  font-weight: 700;
  fill: #1a2b4a;
}

.legend {
  flex: 1;
  min-width: 180px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.lg-name {
  color: var(--ink-soft);
}

.lg-amount {
  margin-left: auto;
  font-weight: 600;
  color: var(--ink);
}

.lg-pct {
  width: 52px;
  text-align: right;
  color: var(--muted);
  font-size: 12px;
}

/* 订单类型 */
.type-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 6px 0;
}

.type-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
}

.type-name {
  font-weight: 600;
  color: var(--ink);
}

.type-num {
  color: var(--muted);
  font-size: 12.5px;
}

.type-bar {
  height: 10px;
  border-radius: 999px;
  background: #eef2fb;
  overflow: hidden;
}

.type-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}

.type-pct {
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--muted);
  text-align: right;
}

/* 热销榜 */
.top-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.top-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.rank {
  width: 22px;
  height: 22px;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-soft);
  background: #eef2fb;
  flex-shrink: 0;
}

.rank.hot {
  color: #fff;
  background: linear-gradient(135deg, #2f6bff, #5b7fff);
}

.top-main {
  flex: 1;
  min-width: 0;
}

.top-line {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  margin-bottom: 5px;
}

.top-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ink);
}

.top-num {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--muted);
}

.top-bar {
  height: 8px;
  border-radius: 999px;
  background: #eef2fb;
  overflow: hidden;
}

.top-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #2f6bff, #8fb0ff);
}

/* 状态分布 */
.status-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 6px 0;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.st-name {
  width: 62px;
  color: var(--ink-soft);
  flex-shrink: 0;
}

.st-bar {
  flex: 1;
  height: 9px;
  border-radius: 999px;
  background: #eef2fb;
  overflow: hidden;
}

.st-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.st-num {
  width: 40px;
  text-align: right;
  color: var(--ink);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* ===== 区间经营 ===== */
.kpi.biz {
  grid-template-columns: repeat(auto-fit, minmax(168px, 1fr));
}

.kpi.biz .kpi-value {
  font-size: 22px;
}

.kpi-value.warn {
  color: #d5485f;
}

/* ===== 时段分布 ===== */
.hour-wrap {
  position: relative;
}

.hour-svg {
  width: 100%;
  height: 210px;
  display: block;
}

/* ===== 分类结构 ===== */
.cat-list {
  gap: 14px;
}

/* ===== 会员与售后 ===== */
.member-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.mg-item {
  padding: 10px 12px;
  border-radius: 12px;
  background: #f6f9ff;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.mg-item span {
  font-size: 12px;
  color: var(--muted);
}

.mg-item b {
  font-size: 18px;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.mg-item b small {
  font-size: 12px;
  font-weight: 500;
  color: var(--muted);
}

.newold {
  margin-bottom: 12px;
}

.no-head {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--muted);
  margin-bottom: 6px;
}

.no-head b {
  color: var(--ink);
}

.no-bar {
  display: flex;
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: #eef2fb;
}

.no-new {
  background: linear-gradient(90deg, #2f6bff, #5b7fff);
  transition: width 0.3s ease;
}

.no-old {
  background: #c9d6ef;
  transition: width 0.3s ease;
}

.no-legend {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--ink-soft);
}

.no-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.dot.new {
  background: #2f6bff;
}

.dot.old {
  background: #c9d6ef;
}

.refund-line {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f6f9ff;
  font-size: 12.5px;
  color: var(--ink-soft);
  line-height: 1.7;
}

.refund-line b {
  color: var(--ink);
}

.refund-line span {
  display: block;
  color: var(--muted);
  font-size: 11.5px;
}
</style>
