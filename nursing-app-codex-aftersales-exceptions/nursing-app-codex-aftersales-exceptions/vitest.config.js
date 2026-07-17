import { defineConfig } from 'vitest/config'
import path from 'path'

export default defineConfig({
  test: {
    // jsdom 模拟浏览器环境
    environment: 'jsdom',
    // 全局变量（uni, getApp 等）
    globals: true,
    // 测试环境初始化（mock uni 等全局对象）
    setupFiles: ['./src/__tests__/setup.js'],
    // 测试文件目录
    include: ['src/**/*.test.js'],
    // 排除
    exclude: ['node_modules', 'dist'],
    // 超时
    testTimeout: 10000,
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
})
