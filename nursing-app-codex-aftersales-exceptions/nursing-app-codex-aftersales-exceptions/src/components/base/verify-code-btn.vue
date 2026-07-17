<template>
  <view class="code-btn-wrap">
    <u-button
      v-if="countdown > 0"
      type="primary"
      :plain="true"
      size="small"
      disabled
      class="code-btn"
    >
      {{ countdown }}s
    </u-button>
    <u-button
      v-else
      type="primary"
      :plain="true"
      size="small"
      class="code-btn"
      @click="handleClick"
    >
      获取验证码
    </u-button>
  </view>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'

const emit = defineEmits(['send'])

const countdown = ref(0)
let timer = null

function start() {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

function handleClick() {
  emit('send')
  start()
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

defineExpose({ start })
</script>

<style lang="scss" scoped>
.code-btn-wrap {
  flex-shrink: 0;
}

.code-btn {
  min-width: 180rpx;
  border-radius: $radius-round !important;
  border-color: rgba(58, 123, 247, 0.28) !important;
  color: $primary-color !important;
  background: rgba(237, 244, 254, 0.82) !important;
}
</style>
