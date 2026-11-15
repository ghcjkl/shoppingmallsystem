<template>
  <div class="cart-page">
    <el-empty
      v-if="!cart.items.length"
      description="购物车还是空的，去逛逛吧"
    >
      <el-button type="primary" round @click="$router.push('/home')">去逛逛</el-button>
    </el-empty>

    <template v-else>
      <div class="sections">
        <section v-if="cart.standardItems.length" class="section">
          <div class="section-title">🍿 标品零食</div>
          <div class="list">
            <div v-for="item in cart.standardItems" :key="cart.keyOf(item)" class="row">
              <CartRow :item="item" />
            </div>
          </div>
        </section>

        <section v-if="cart.freshItems.length" class="section">
          <div class="section-title">🧋 现制饮品</div>
          <div class="list">
            <div v-for="item in cart.freshItems" :key="cart.keyOf(item)" class="row">
              <CartRow :item="item" />
            </div>
          </div>
        </section>
      </div>

      <div class="footer">
        <div class="footer-left">
          <el-button text type="danger" @click="clearCart">清空购物车</el-button>
        </div>
        <div class="footer-right">
          <span class="count">共 {{ cart.count }} 件</span>
          <span class="total">合计 ¥{{ fmtMoney(cart.totalAmount) }}</span>
          <el-button type="primary" size="large" round @click="$router.push('/checkout')">
            去结算
          </el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { defineComponent, h, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElImage, ElMessageBox } from 'element-plus'
import { useCartStore } from '../stores/cart'
import { fmtMoney } from '../utils/format'

const router = useRouter()
const cart = useCartStore()

// 购物车行组件（内联定义，便于复用增减逻辑）
const CartRow = defineComponent({
  name: 'CartRow',
  props: { item: { type: Object, required: true } },
  setup(props) {
    const c = useCartStore()
    const key = computed(() => c.keyOf(props.item))
    const ingredientText = computed(() =>
      (props.item.ingredients || []).map((i) => i.name).join(' / ')
    )
    const specText = computed(() => {
      const parts = [props.item.skuLabel]
      const opts = (props.item.specOptions || []).map((o) => o.name)
      if (opts.length) parts.push(opts.join('/'))
      return parts.join(' / ')
    })
    return () =>
      h('div', { class: 'cart-row' }, [
        h('div', { class: 'thumb', ['data-type']: props.item.productType }, [
          props.item.coverImg
            ? h(ElImage, {
                src: props.item.coverImg,
                fit: 'cover',
                style: 'width:100%;height:100%'
              })
            : h('span', { class: 'thumb-emoji' }, props.item.productType === 2 ? '🧋' : '🍿')
        ]),
        h('div', { class: 'row-main' }, [
          h('div', { class: 'row-name' }, props.item.productName),
          h('div', { class: 'row-spec' }, specText.value),
          ingredientText.value
            ? h('div', { class: 'row-ing' }, '加料：' + ingredientText.value)
            : null
        ]),
        h('div', { class: 'row-side' }, [
          h('div', { class: 'row-price' }, '¥' + fmtMoney(props.item.unitPrice)),
          h('div', { class: 'row-ops' }, [
            h(
              'button',
              {
                class: 'op-btn',
                onClick: () => c.changeQuantity(key.value, -1)
              },
              '−'
            ),
            h('span', { class: 'op-num' }, props.item.quantity),
            h(
              'button',
              {
                class: 'op-btn',
                onClick: () => c.changeQuantity(key.value, 1)
              },
              '＋'
            )
          ]),
          h(
            ElButton,
            {
              text: true,
              type: 'danger',
              size: 'small',
              onClick: () => c.remove(key.value)
            },
            () => '删除'
          )
        ])
      ])
  }
})

const clearCart = () => {
  ElMessageBox.confirm('确定清空购物车吗？', '提示', { type: 'warning' })
    .then(() => cart.clear())
    .catch(() => {})
}
</script>

<style scoped>
.cart-page {
  max-width: 900px;
  margin: 0 auto;
  padding-bottom: 90px;
}

.sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section {
  border-radius: 18px;
  background: var(--surface);
  border: 1px solid rgba(30, 79, 214, 0.08);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.section-title {
  padding: 14px 18px;
  font-size: 14px;
  font-weight: 700;
  border-bottom: 1px solid var(--line);
  background: #f4f7ff;
}

.list {
  display: flex;
  flex-direction: column;
}

.row {
  padding: 12px 18px;
}

.row + .row {
  border-top: 1px solid var(--line);
}

.footer {
  position: fixed;
  left: 232px;
  right: 20px;
  bottom: 16px;
  z-index: 25;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-radius: 18px;
  background: rgba(255, 253, 249, 0.96);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 34px rgba(26, 43, 74, 0.16);
  backdrop-filter: blur(18px);
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.count {
  font-size: 13px;
  color: var(--muted);
}

.total {
  font-size: 20px;
  font-weight: 800;
  color: var(--accent-deep);
}

/* 行样式（作用于内联渲染组件） */
:deep(.cart-row) {
  display: flex;
  align-items: center;
  gap: 14px;
}

:deep(.thumb) {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  border-radius: 12px;
  overflow: hidden;
  display: grid;
  place-items: center;
}

:deep(.thumb[data-type='1']) {
  background: linear-gradient(135deg, #dbe7ff, #aac5ff);
}

:deep(.thumb[data-type='2']) {
  background: linear-gradient(135deg, #dfe8ff, #c4d6ff);
}

:deep(.thumb-emoji) {
  font-size: 32px;
}

:deep(.row-main) {
  flex: 1;
  min-width: 0;
}

:deep(.row-name) {
  font-size: 14.5px;
  font-weight: 600;
}

:deep(.row-spec) {
  margin-top: 3px;
  font-size: 12px;
  color: var(--ink-soft);
}

:deep(.row-ing) {
  margin-top: 2px;
  font-size: 12px;
  color: var(--muted);
}

:deep(.row-side) {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

:deep(.row-price) {
  font-size: 15px;
  font-weight: 700;
  color: var(--accent-deep);
}

:deep(.row-ops) {
  display: flex;
  align-items: center;
  gap: 10px;
}

:deep(.op-btn) {
  width: 26px;
  height: 26px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}

:deep(.op-num) {
  min-width: 20px;
  text-align: center;
  font-size: 14px;
}

@media (max-width: 768px) {
  .footer {
    left: 10px;
    right: 10px;
    bottom: calc(78px + env(safe-area-inset-bottom));
  }

  .count {
    display: none;
  }
}
</style>
