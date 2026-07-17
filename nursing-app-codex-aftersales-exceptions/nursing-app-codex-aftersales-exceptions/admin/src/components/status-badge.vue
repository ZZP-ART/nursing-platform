<template><el-tag :type="meta.type" effect="light" round>{{ meta.label }}</el-tag></template>

<script setup>
import { computed } from 'vue'
import { getReviewStatusMeta } from '../constants/review.js'

const props = defineProps({ status: { type: String, required: true } })
const meta = computed(() => getReviewStatusMeta(props.status))
</script>
