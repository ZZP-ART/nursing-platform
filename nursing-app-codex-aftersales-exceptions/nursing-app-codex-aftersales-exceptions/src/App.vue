<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('[App] Launch — 智慧护理平台启动')
  // 检查登录态：尝试自动登录
  const token = uni.getStorageSync('token')
  if (token) {
    console.log('[App] 已检测到登录态，自动恢复')
  }
})

onShow(() => {
  console.log('[App] Show')
})

onHide(() => {
  console.log('[App] Hide')
})
</script>

<style lang="scss">
/* 全局基础样式 */
page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  font-size: $font-size-base;
  color: $text-color;
  background-color: $bg-color-grey;
  -webkit-font-smoothing: antialiased;
}

view,
text,
button,
input {
  box-sizing: border-box;
}

button,
.u-button {
  transition: transform $transition-fast, box-shadow $transition-fast;

  &:active {
    transform: scale(0.97);
  }
}

/* uView Plus 全局样式覆盖（按需） */
</style>
