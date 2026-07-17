/**
 * Pinia — 服务 Store 单元测试
 *
 * 覆盖：分类获取、列表分页、详情、搜索、分类筛选、重置
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useServiceStore } from '@/store/service.js'

// Mock request 模块
vi.mock('@/utils/request.js', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
  resolveAssetUrl: vi.fn((url) => url || ''),
}))

import http from '@/utils/request.js'

function mockCategories() {
  return [
    { categoryId: 1, name: '康复护理', icon: 'icon1' },
    { categoryId: 2, name: '母婴护理', icon: 'icon2' },
    { categoryId: 3, name: '中医理疗', icon: 'icon3' },
  ]
}

function mockServiceList(offset = 0, size = 20, total = 50) {
  const list = []
  for (let i = 0; i < Math.min(size, total - offset); i++) {
    list.push({
      id: offset + i + 1,
      name: `服务项目 ${offset + i + 1}`,
      minPrice: 100 + i * 10,
      coverImage: `/static/img${i}.jpg`,
      categoryId: 1,
      categoryName: '康复护理',
    })
  }
  const nextOffset = offset + list.length
  const hasNext = nextOffset < total
  return { list, size, hasNext, nextCursor: hasNext ? `cursor_${nextOffset}` : null }
}

describe('serviceStore — 分类', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchCategories 获取并存储分类列表', async () => {
    http.get.mockResolvedValueOnce({ code: 0, data: mockCategories() })
    const store = useServiceStore()
    const result = await store.fetchCategories()
    expect(result).toHaveLength(3)
    expect(store.categories[0].name).toBe('康复护理')
  })
})

describe('serviceStore — 服务列表分页', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchServices 初始加载第 1 页', async () => {
    const mockData = mockServiceList(0, 20, 50)
    http.get.mockResolvedValueOnce({ code: 0, data: mockData })

    const store = useServiceStore()
    const result = await store.fetchServices()

    expect(result.list).toHaveLength(20)
    expect(result.total).toBe(20)
    expect(store.total).toBe(20)
    expect(store.currentPage).toBe(1)
    expect(store.hasMore).toBe(true)
  })

  it('loadMore 追加第 2 页数据', async () => {
    // 先加载第 1 页
    http.get.mockResolvedValueOnce({ code: 0, data: mockServiceList(0, 20, 50) })
    const store = useServiceStore()
    await store.fetchServices()

    // 加载更多
    http.get.mockResolvedValueOnce({ code: 0, data: mockServiceList(20, 20, 50) })
    const result = await store.loadMore()

    expect(result.list).toHaveLength(40) // 20 + 20
    expect(store.services).toHaveLength(40)
    expect(store.currentPage).toBe(2)
    expect(store.hasMore).toBe(true)
  })

  it('数据全部加载完后 hasMore 为 false', async () => {
    http.get.mockResolvedValueOnce({ code: 0, data: mockServiceList(0, 10, 10) })
    const store = useServiceStore()
    await store.fetchServices({ page: 1, size: 10 })

    expect(store.hasMore).toBe(false)
  })

  it('loadMore 在无更多数据时直接返回空列表', async () => {
    http.get.mockResolvedValueOnce({ code: 0, data: mockServiceList(0, 5, 5) })
    const store = useServiceStore()
    await store.fetchServices({ page: 1, size: 5 })

    expect(store.hasMore).toBe(false)
    const result = await store.loadMore()
    expect(result.list).toEqual([]) // 无更多数据，返回空
    expect(result.total).toBe(5)
  })
})

describe('serviceStore — 搜索', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('searchServices 按关键词重置列表', async () => {
    const searchResult = {
      list: [
        { itemId: 10, name: '肩颈推拿', minPrice: 168, coverImage: '/img.jpg', categoryName: '中医理疗' },
      ],
      size: 20,
      hasNext: false,
      nextCursor: null,
    }
    http.get.mockResolvedValueOnce({ code: 0, data: searchResult })

    const store = useServiceStore()
    const result = await store.searchServices('肩颈')

    expect(result.list).toHaveLength(1)
    expect(store.searchKeyword).toBe('肩颈')
    expect(result.list[0].name).toBe('肩颈推拿')
  })
})

describe('serviceStore — 分类筛选与重置', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('setActiveCategory 设置当前分类', () => {
    const store = useServiceStore()
    store.setActiveCategory(2)
    expect(store.activeCategoryId).toBe(2)
  })

  it('reset 清空所有状态', () => {
    const store = useServiceStore()
    store.services = [{ itemId: 1, name: 'test' }]
    store.total = 100
    store.currentPage = 3
    store.searchKeyword = 'test'
    store.activeCategoryId = 5

    store.reset()

    expect(store.services).toEqual([])
    expect(store.total).toBe(0)
    expect(store.currentPage).toBe(1)
    expect(store.searchKeyword).toBe('')
    expect(store.activeCategoryId).toBeNull()
  })
})

describe('serviceStore — loading 状态', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchServices 开始时 loading=true，结束后 loading=false', async () => {
    http.get.mockResolvedValueOnce({ code: 0, data: mockServiceList(0, 20, 50) })
    const store = useServiceStore()

    const promise = store.fetchServices()
    expect(store.loading).toBe(true)
    await promise
    expect(store.loading).toBe(false)
  })

  it('fetchServices 即使失败 loading 也恢复为 false', async () => {
    http.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useServiceStore()

    await expect(store.fetchServices()).rejects.toBeDefined()
    expect(store.loading).toBe(false)
  })
})
