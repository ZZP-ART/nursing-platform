import { ref } from 'vue'
import { defineStore } from 'pinia'
import http, { createIdempotencyKey } from '../api/request.js'

export const useFeedbackReviewStore = defineStore('adminFeedbackReview', () => {
  const reviews = ref([])
  const total = ref(0)
  const loading = ref(false)

  async function fetchReviews(params = {}) {
    loading.value = true
    try {
      const res = await http.get('/api/v1/admin/feedback/reviews', { params })
      reviews.value = res.data.list || []
      total.value = res.data.total || 0
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function moderate(id, status) {
    await http.post(`/api/v1/admin/feedback/reviews/${id}/moderate`, { status }, {
      headers: { 'Idempotency-Key': createIdempotencyKey('review-moderate') },
    })
  }

  return { reviews, total, loading, fetchReviews, moderate }
})
