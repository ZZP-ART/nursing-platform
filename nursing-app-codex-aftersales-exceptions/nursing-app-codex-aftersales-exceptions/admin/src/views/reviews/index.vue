<template>
  <div class="review-list-page">
    <section class="page-intro"><div><span class="page-eyebrow">{{ typeMeta.label }}</span><h2>{{ typeMeta.label }}管理</h2><p>集中查看申请资料、审核记录和当前处理状态。</p></div><div class="intro-count"><strong>{{ total }}</strong><span>条记录</span></div></section>
    <section class="panel filter-panel"><el-segmented v-model="filters.status" :options="statusOptions" @change="loadReviews" /><el-input v-model="filters.keyword" clearable placeholder="搜索名称、申请人或编号" class="search-input" @keyup.enter="loadReviews"><template #prefix><el-icon><Search /></el-icon></template></el-input><el-button type="primary" @click="loadReviews">查询</el-button></section>
    <section class="panel table-panel"><el-table v-loading="reviewStore.loading" :data="reviews" row-class-name="clickable-row" @row-click="goDetail"><el-table-column prop="subjectName" :label="typeMeta.subjectLabel" min-width="210"><template #default="{ row }"><div class="subject-cell"><div class="subject-avatar" :style="{ background: typeMeta.color }">{{ row.subjectName.slice(0, 1) }}</div><div><strong>{{ row.subjectName }}</strong><span>{{ row.referenceNo }}</span></div></div></template></el-table-column><el-table-column prop="applicantName" label="申请人/所属商户" min-width="150" /><el-table-column prop="summary" label="摘要" min-width="230" show-overflow-tooltip /><el-table-column prop="submitTime" label="提交时间" width="175" /><el-table-column label="审核状态" width="120"><template #default="{ row }"><status-badge :status="row.status" /></template></el-table-column><el-table-column label="操作" width="105" fixed="right"><template #default="{ row }"><el-button link type="primary" @click.stop="goDetail(row)">查看详情</el-button></template></el-table-column></el-table><div class="table-footer"><span>共 {{ total }} 条记录</span><el-pagination background layout="prev, pager, next" :total="total" :page-size="filters.size" @current-change="changePage" /></div></section>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import StatusBadge from '../../components/status-badge.vue'
import { REVIEW_STATUS, getReviewTypeMeta } from '../../constants/review.js'
import { useReviewStore } from '../../stores/review.js'

const route = useRoute()
const router = useRouter()
const reviewStore = useReviewStore()
const reviews = computed(() => reviewStore.reviews)
const total = computed(() => reviewStore.total)
const typeMeta = computed(() => getReviewTypeMeta(route.params.type))
const statusOptions = [{ label: '全部', value: '' }, { label: '待审核', value: REVIEW_STATUS.PENDING_REVIEW }, { label: '已通过', value: REVIEW_STATUS.APPROVED }, { label: '已驳回', value: REVIEW_STATUS.REJECTED }]
const filters = reactive({ status: REVIEW_STATUS.PENDING_REVIEW, keyword: '', page: 1, size: 10 })
async function loadReviews() { await reviewStore.fetchReviews(route.params.type, { ...filters }) }
function goDetail(row) { router.push(`/reviews/${route.params.type}/${row.id}`) }
function changePage(page) { filters.page = page; loadReviews() }
watch(() => route.params.type, () => { filters.page = 1; filters.status = REVIEW_STATUS.PENDING_REVIEW; loadReviews() }, { immediate: true })
</script>
