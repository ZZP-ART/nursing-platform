import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import uviewPlus from 'uview-plus'
import UIcon from 'uview-plus/components/u-icon/u-icon.vue'

import App from './App.vue'
import './uni.scss'

// 默认保留 Mock 演示；联调真实后端时设置 VITE_USE_MOCK=false。
if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
  import('./mock/index.js')
}

export function createApp() {
  const app = createSSRApp(App)

  // Pinia 状态管理
  app.use(createPinia())

  // uView Plus UI 组件库
  app.use(uviewPlus)
  // uView's H5 internals render this compatibility tag dynamically.
  app.component('UpIcon', UIcon)

  return { app }
}
