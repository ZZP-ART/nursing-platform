/**
 * Vitest 全局测试环境配置
 * Mock uni-app 的全局 API（uni, getApp, getCurrentPages）
 */
import { vi, beforeEach } from 'vitest'

// ========== Mock uni 全局对象 ==========
const mockUni = {
  // 存储
  storage: {},
  setStorageSync(key, value) {
    this.storage[key] = value
  },
  getStorageSync(key) {
    return this.storage[key] !== undefined ? this.storage[key] : ''
  },
  removeStorageSync(key) {
    delete this.storage[key]
  },
  clearStorageSync() {
    this.storage = {}
  },

  // UI
  showToast: vi.fn(),
  showLoading: vi.fn(),
  hideLoading: vi.fn(),
  showModal: vi.fn(),

  // 导航
  navigateTo: vi.fn(),
  redirectTo: vi.fn(),
  reLaunch: vi.fn(),
  switchTab: vi.fn(),
  navigateBack: vi.fn(),

  // 请求
  request: vi.fn(),
  uploadFile: vi.fn(),

  // 其他
  getSystemInfoSync: vi.fn(() => ({
    platform: 'devtools',
    model: 'test',
  })),
}

globalThis.uni = mockUni
globalThis.getApp = vi.fn(() => ({}))
globalThis.getCurrentPages = vi.fn(() => [])

// 每次测试前重置 storage
beforeEach(() => {
  mockUni.storage = {}
  vi.clearAllMocks()
})
