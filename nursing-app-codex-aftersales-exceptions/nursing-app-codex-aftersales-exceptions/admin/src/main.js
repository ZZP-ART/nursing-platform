import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElButton,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElLoading,
  ElPagination,
  ElSegmented,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router/index.js'
import './styles/index.scss'

const useMock = import.meta.env.VITE_USE_MOCK
  ? import.meta.env.VITE_USE_MOCK === 'true'
  : false

if (useMock) {
  await import('./mock/index.js')
}

const app = createApp(App)
const components = [
  ElButton,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElPagination,
  ElSegmented,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTimeline,
  ElTimelineItem,
]

components.forEach((component) => app.component(component.name, component))
app.directive('loading', ElLoading.directive)
app.use(createPinia()).use(router).mount('#app')
