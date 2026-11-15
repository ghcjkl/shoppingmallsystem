import { defineStore } from 'pinia'

const STORAGE_KEY = 'shopping_cart'

// 购物车条目结构：
// { productId, productType, productName, coverImg, skuId, skuLabel,
//   skuPrice, ingredients: [{id,name,price}], specOptions: [{id,name,priceDelta,groupName}],
//   unitPrice, quantity }
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
  }),
  getters: {
    count: (s) => s.items.reduce((n, i) => n + i.quantity, 0),
    totalAmount: (s) => s.items.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0),
    standardItems: (s) => s.items.filter((i) => i.productType === 1),
    freshItems: (s) => s.items.filter((i) => i.productType === 2),
    // 某商品在购物车中的总件数（含各种规格/加料组合），首页卡片步进器直接读它
    quantityOf: (s) => (productId) =>
      s.items
        .filter((i) => i.productId === productId)
        .reduce((n, i) => n + Number(i.quantity || 0), 0)
  },
  actions: {
    persist() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.items))
    },
    // 唯一键：商品 + SKU + 加料组合 + 制作选项组合（不同冷热/甜度不能合并成一行）
    keyOf(item) {
      const ing = (item.ingredients || []).map((i) => i.id).sort((a, b) => a - b).join(',')
      const spec = (item.specOptions || []).map((o) => o.id).sort((a, b) => a - b).join(',')
      return `${item.productId}|${item.skuId || ''}|${ing}|${spec}`
    },
    add(item) {
      const key = this.keyOf(item)
      const exist = this.items.find((i) => this.keyOf(i) === key)
      if (exist) {
        exist.quantity += item.quantity
      } else {
        this.items.push({ ...item })
      }
      this.persist()
    },
    changeQuantity(key, delta) {
      const it = this.items.find((i) => this.keyOf(i) === key)
      if (!it) return
      it.quantity += delta
      if (it.quantity <= 0) this.remove(key)
      else this.persist()
    },
    // 首页卡片「−」：优先减默认规格那一行，没有则减该商品最后加入的一行
    decreaseByProduct(productId) {
      const quickKey = `${productId}||`
      let target = this.items.find((i) => this.keyOf(i) === quickKey)
      if (!target) {
        target = [...this.items].reverse().find((i) => i.productId === productId)
      }
      if (!target) return
      this.changeQuantity(this.keyOf(target), -1)
    },
    remove(key) {
      this.items = this.items.filter((i) => this.keyOf(i) !== key)
      this.persist()
    },
    clear() {
      this.items = []
      this.persist()
    }
  }
})
