// 金额格式化：保留两位小数
export const fmtMoney = (n) => (Number(n || 0)).toFixed(2)

// 时间格式化：后端 LocalDateTime 形如 "2026-11-12T12:00:00"
export const fmtTime = (t) => {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 19)
}
