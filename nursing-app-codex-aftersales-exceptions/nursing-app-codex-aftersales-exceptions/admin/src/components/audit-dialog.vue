<template>
  <el-dialog v-model="visible" :title="action === 'approve' ? '确认审核通过' : '填写驳回意见'" width="520px" destroy-on-close>
    <div class="audit-dialog-copy"><span class="audit-symbol" :class="action">{{ action === 'approve' ? '✓' : '!' }}</span><div><strong>{{ action === 'approve' ? '确认资料真实、完整并符合平台规范？' : '请填写明确且可执行的修改意见' }}</strong><p>{{ action === 'approve' ? '通过后会立即更新申请状态，并记录当前管理员和处理时间。' : '驳回意见将展示给申请人，至少填写 10 个字符。' }}</p></div></div>
    <el-form label-position="top"><el-form-item :label="action === 'approve' ? '审核备注（选填）' : '驳回原因'"><el-input v-model="reason" type="textarea" :rows="4" maxlength="300" show-word-limit :placeholder="action === 'approve' ? '例如：资料核验无误，同意通过' : '例如：营业执照图片模糊，请重新上传原件扫描件'" /></el-form-item></el-form>
    <template #footer><el-button @click="visible = false">取消</el-button><el-button :type="action === 'approve' ? 'success' : 'danger'" :loading="loading" @click="confirm">确认{{ action === 'approve' ? '通过' : '驳回' }}</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['confirm'])
const visible = ref(false)
const action = ref('approve')
const reason = ref('')
const loading = ref(false)

function open(nextAction) { action.value = nextAction; reason.value = nextAction === 'approve' ? '资料核验无误，同意通过' : ''; visible.value = true }
async function confirm() { if (action.value === 'reject' && reason.value.trim().length < 10) return ElMessage.warning('驳回原因至少填写10个字符'); loading.value = true; try { await new Promise((resolve, reject) => emit('confirm', { action: action.value, reason: reason.value.trim(), resolve, reject })); visible.value = false } finally { loading.value = false } }
defineExpose({ open })
</script>
