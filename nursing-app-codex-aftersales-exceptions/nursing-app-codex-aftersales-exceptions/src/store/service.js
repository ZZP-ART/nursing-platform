/**
 * 服务目录 Store。
 * 后端使用 id + 游标分页；页面内部继续使用 itemId/specId，统一在此归一化。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http, { resolveAssetUrl } from '@/utils/request.js'

function normalizeSpec(spec = {}) {
  const specId = spec.specId ?? spec.id
  return { ...spec, id: spec.id == null ? spec.id : String(spec.id), specId: specId == null ? specId : String(specId) }
}

function normalizeService(item = {}) {
  const specs = (item.specs || []).map(normalizeSpec)
  return {
    ...item,
    id: item.id == null ? item.id : String(item.id),
    itemId: item.itemId == null && item.id == null ? undefined : String(item.itemId ?? item.id),
    categoryId: item.categoryId == null ? item.categoryId : String(item.categoryId),
    coverImage: resolveAssetUrl(item.coverImage),
    images: (item.images || []).map(resolveAssetUrl),
    minPrice: item.minPrice ?? (specs.length ? Math.min(...specs.map((spec) => Number(spec.price))) : null),
    specs,
  }
}

function flattenCategories(nodes = [], depth = 0) {
  return nodes.flatMap((node) => [
    { ...node, categoryId: node.categoryId == null ? node.categoryId : String(node.categoryId), depth },
    ...flattenCategories(node.children || [], depth + 1),
  ])
}

export const useServiceStore = defineStore('service', () => {
  const categoryTree = ref([])
  const categories = ref([])
  const services = ref([])
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(20)
  const nextCursor = ref(null)
  const hasNext = ref(false)
  const currentService = ref(null)
  const searchKeyword = ref('')
  const activeCategoryId = ref(null)
  const loading = ref(false)

  const hasMore = computed(() => hasNext.value)

  async function fetchCategories() {
    const res = await http.get('/api/v1/categories')
    categoryTree.value = res.data || []
    categories.value = flattenCategories(categoryTree.value)
    return categories.value
  }

  async function fetchServices(params = {}) {
    loading.value = true
    try {
      const query = { size: params.size || pageSize.value }
      if (params.categoryId) query.categoryId = params.categoryId
      if (params.keyword?.trim()) query.keyword = params.keyword.trim()

      const res = await http.get('/api/v1/items', query)
      const data = res.data || {}
      services.value = (data.list || []).map(normalizeService)
      pageSize.value = data.size || query.size
      nextCursor.value = data.nextCursor || null
      hasNext.value = Boolean(data.hasNext)
      currentPage.value = 1
      total.value = services.value.length
      return { list: services.value, total: total.value, hasNext: hasNext.value }
    } finally {
      loading.value = false
    }
  }

  async function loadMore() {
    if (!hasNext.value || !nextCursor.value) return { list: [], total: total.value }
    loading.value = true
    try {
      const query = { cursor: nextCursor.value, size: pageSize.value }
      if (activeCategoryId.value) query.categoryId = activeCategoryId.value
      if (searchKeyword.value) query.keyword = searchKeyword.value

      const res = await http.get('/api/v1/items', query)
      const data = res.data || {}
      services.value = [...services.value, ...(data.list || []).map(normalizeService)]
      nextCursor.value = data.nextCursor || null
      hasNext.value = Boolean(data.hasNext)
      currentPage.value += 1
      total.value = services.value.length
      return { list: services.value, total: total.value, hasNext: hasNext.value }
    } finally {
      loading.value = false
    }
  }

  async function fetchServiceDetail(itemId) {
    loading.value = true
    try {
      const res = await http.get(`/api/v1/items/${itemId}`)
      currentService.value = normalizeService(res.data)
      return currentService.value
    } finally {
      loading.value = false
    }
  }

  async function searchServices(keyword, params = {}) {
    const normalizedKeyword = keyword.trim()
    searchKeyword.value = normalizedKeyword
    activeCategoryId.value = params.categoryId || null
    return fetchServices({
      keyword: normalizedKeyword,
      categoryId: params.categoryId,
      size: params.size || 20,
    })
  }

  function setActiveCategory(categoryId) {
    activeCategoryId.value = categoryId
    searchKeyword.value = ''
    nextCursor.value = null
    hasNext.value = false
  }

  function reset() {
    services.value = []
    total.value = 0
    currentPage.value = 1
    nextCursor.value = null
    hasNext.value = false
    searchKeyword.value = ''
    activeCategoryId.value = null
  }

  return {
    categoryTree,
    categories,
    services,
    total,
    currentPage,
    pageSize,
    nextCursor,
    currentService,
    searchKeyword,
    activeCategoryId,
    loading,
    hasMore,
    fetchCategories,
    fetchServices,
    loadMore,
    fetchServiceDetail,
    searchServices,
    setActiveCategory,
    reset,
  }
})
