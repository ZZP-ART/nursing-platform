/**
 * Pinia — 评价管理
 *
 * 对齐 API v1.0（feedback-service / reviews）：
 * - 提交评价（一单一评）
 * - 评价列表（按服务项目，分页）
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import http, { createIdempotentKey } from '@/utils/request.js'

export const useReviewStore = defineStore('review', () => {
  // ===== 状态 =====
  const reviews = ref([])
  const total = ref(0)
  const loading = ref(false)

  // ===== 方法 =====

  /**
   * 提交评价
   * @param {Object} params - { orderId, rating, content, images }
   */
  async function submitReview(params) {
    const res = await http.post('/api/v1/reviews', params, {
      idempotentKey: createIdempotentKey('review'),
    })
    return res.data // { reviewId }
  }

  /**
   * 获取指定服务的评价列表
   * @param {number} itemId - 服务项目ID
   * @param {Object} params - { page, size }
   */
  async function fetchReviews(itemId, params = {}) {
    loading.value = true
    try {
      const query = { itemId, page: params.page || 1, size: params.size || 20 }
      const res = await http.get('/api/v1/reviews', query)
      const data = res.data
      reviews.value = data.list || []
      total.value = data.total || 0
      return { list: reviews.value, total: total.value }
    } finally {
      loading.value = false
    }
  }

  return {
    reviews,
    total,
    loading,
    submitReview,
    fetchReviews,
  }
})
