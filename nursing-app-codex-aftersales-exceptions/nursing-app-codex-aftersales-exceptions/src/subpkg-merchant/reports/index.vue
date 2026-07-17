<template>
  <view class="page-shell">
    <view class="page-content">
      <view class="hero-card">
        <view><text class="eyebrow">经营数据中心</text><text class="hero-title">订单与服务质量</text><text class="hero-desc">统计口径为前端 Mock 演示，后续由后端报表服务提供</text></view>
        <u-icon name="level" size="40" color="#FFFFFF" />
      </view>

      <view class="range-tabs">
        <view v-for="item in ranges" :key="item.value" :class="['range-tab', { active: store.range === item.value }]" @click="changeRange(item.value)">{{ item.label }}</view>
      </view>

      <view v-if="report">
        <view class="section-head"><text>经营概览</text><text>{{ rangeLabel }}</text></view>
        <view class="overview-grid">
          <view class="metric-card primary"><text class="metric-label">订单数</text><text class="metric-value">{{ report.overview.orderCount }}</text><text class="metric-desc">已支付 {{ report.overview.paidOrderCount }} 单</text></view>
          <view class="metric-card success"><text class="metric-label">订单净额</text><text class="metric-value money">¥{{ money(report.overview.revenue) }}</text><text class="metric-desc">客单价 ¥{{ money(report.overview.averageOrderAmount) }}</text></view>
        </view>

        <view class="section-head"><text>服务质量</text><text>{{ report.quality.reviewCount }} 条评价</text></view>
        <view class="quality-card">
          <view v-for="item in qualityMetrics" :key="item.label" class="quality-item">
            <view class="quality-top"><text>{{ item.label }}</text><text :class="`tone-${item.tone}`">{{ item.value }}</text></view>
            <view class="progress-track"><view :class="['progress-value', item.tone]" :style="{ width: `${item.progress}%` }" /></view>
            <text class="quality-desc">{{ item.desc }}</text>
          </view>
        </view>

        <view class="section-head"><text>订单趋势</text><text>按创建日期</text></view>
        <scroll-view scroll-x class="trend-scroll">
          <view class="trend-chart">
            <view v-for="item in report.trend" :key="item.date" class="trend-column">
              <text class="trend-count">{{ item.orderCount }}</text>
              <view class="trend-bar-wrap"><view class="trend-bar" :style="{ height: `${trendHeight(item.orderCount)}%` }" /></view>
              <text class="trend-date">{{ item.date.slice(5) }}</text>
            </view>
          </view>
        </scroll-view>

        <view class="section-head"><text>热门服务</text><text>按订单量</text></view>
        <view class="list-card">
          <view v-for="item in report.topServices" :key="item.itemId" class="service-row">
            <view class="service-copy"><text class="service-name">{{ item.name }}</text><view class="service-track"><view class="service-value" :style="{ width: `${serviceWidth(item.orderCount)}%` }" /></view></view>
            <view class="service-meta"><text>{{ item.orderCount }} 单</text><text>¥{{ money(item.revenue) }}</text></view>
          </view>
          <text v-if="!report.topServices.length" class="empty-text">当前范围暂无服务订单</text>
        </view>

        <view class="section-head"><text>风险与异常监控</text><text :class="{ danger: report.alerts.total }">{{ report.alerts.total }} 项待处理</text></view>
        <view class="alert-summary"><view><text>{{ report.alerts.complaintCount }}</text><text>待处理投诉</text></view><view><text>{{ report.alerts.exceptionCount }}</text><text>异常工单</text></view></view>
        <view class="list-card risk-list">
          <view v-for="item in report.alerts.list" :key="`${item.type}-${item.id}`" class="risk-row" @click="openRisk(item)">
            <view :class="['risk-dot', item.tone]" /><view class="risk-copy"><text class="risk-title">{{ item.title }}</text><text class="risk-desc">{{ item.subtitle }}</text><text class="risk-time">{{ formatTime(item.time) }}</text></view><u-icon name="arrow-right" size="15" color="#C5CDD8" />
          </view>
          <text v-if="!report.alerts.list.length" class="empty-text">当前范围暂无待处理风险事项</text>
        </view>
      </view>

      <view v-else class="loading"><u-loading-icon size="30" color="#00A89D" /></view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { ROLES } from '@/constants/roles.js'
import { useMerchantReportStore } from '@/store/merchant-report.js'
import { requireRole } from '@/utils/permission.js'

const store = useMerchantReportStore()
const ranges = [{ label: '近7天', value: '7d' }, { label: '近30天', value: '30d' }, { label: '全部', value: 'all' }]
const report = computed(() => store.report)
const rangeLabel = computed(() => ranges.find((item) => item.value === store.range)?.label || '近30天')
const qualityMetrics = computed(() => {
  const quality = report.value?.quality
  if (!quality) return []
  return [
    { label: '订单完成率', value: `${quality.completionRate}%`, progress: quality.completionRate, tone: 'success', desc: '已完成订单 / 当前范围全部订单' },
    { label: '顾客好评率', value: `${quality.positiveRate}%`, progress: quality.positiveRate, tone: 'primary', desc: '4 星及以上评价占比' },
    { label: '平均评分', value: `${quality.averageRating || 0} 分`, progress: quality.averageRating * 20, tone: 'primary', desc: '已展示评价的平均星级' },
    { label: '投诉率', value: `${quality.complaintRate}%`, progress: quality.complaintRate, tone: 'danger', desc: '产生投诉的记录数 / 订单数' },
    { label: '异常率', value: `${quality.exceptionRate}%`, progress: quality.exceptionRate, tone: 'warning', desc: '异常工单数 / 订单数' },
  ]
})
const maxTrend = computed(() => Math.max(1, ...(report.value?.trend || []).map((item) => item.orderCount)))
const maxService = computed(() => Math.max(1, ...(report.value?.topServices || []).map((item) => item.orderCount)))

onShow(() => { if (requireRole(ROLES.MERCHANT_MEMBER)) store.fetchReport(store.range) })
function changeRange(value) { if (value !== store.range) store.fetchReport(value) }
function money(value) { return Number(value || 0).toFixed(2) }
function trendHeight(value) { return Math.max(8, Math.round((Number(value || 0) / maxTrend.value) * 100)) }
function serviceWidth(value) { return Math.max(8, Math.round((Number(value || 0) / maxService.value) * 100)) }
function formatTime(value) { return value?.replace('T', ' ').replace('+08:00', '').slice(0, 16) || '--' }
function openRisk(item) {
  const url = item.type === 'COMPLAINT' ? `/subpkg-merchant/complaint-detail/index?id=${item.id}` : `/subpkg-merchant/exception-detail/index?id=${item.id}`
  uni.navigateTo({ url })
}
</script>

<style lang="scss" scoped>
.page-shell{min-height:100vh;background:$page-gradient}.page-content{padding:24rpx $spacing-base 60rpx}.hero-card{display:flex;align-items:center;justify-content:space-between;padding:31rpx;border-radius:31rpx;color:#fff;background:linear-gradient(135deg,#116b68,#00a89d 55%,#38c6b5);box-shadow:$shadow-float}.eyebrow,.hero-title,.hero-desc,.metric-label,.metric-value,.metric-desc,.quality-desc,.trend-count,.trend-date,.service-name,.service-meta text,.risk-title,.risk-desc,.risk-time,.empty-text{display:block}.eyebrow{font-size:$font-size-xs;opacity:.75}.hero-title{margin-top:8rpx;font-size:35rpx;font-weight:800}.hero-desc{max-width:520rpx;margin-top:8rpx;font-size:20rpx;opacity:.78}.range-tabs{display:grid;grid-template-columns:repeat(3,1fr);gap:10rpx;margin-top:19rpx;padding:7rpx;border-radius:$radius-round;background:rgba(255,255,255,.72)}.range-tab{height:61rpx;border-radius:$radius-round;color:$text-color-secondary;font-size:$font-size-xs;line-height:61rpx;text-align:center}.range-tab.active{color:#fff;background:$primary-gradient;box-shadow:$shadow-sm}.section-head{display:flex;justify-content:space-between;margin:28rpx 4rpx 13rpx;color:$text-color;font-size:$font-size-md;font-weight:700}.section-head text:last-child{color:$text-color-hint;font-size:$font-size-xs;font-weight:400}.section-head .danger{color:$error-color}.overview-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:14rpx}.metric-card{padding:23rpx;border:$glass-border-soft;border-radius:25rpx;background:$surface-gradient;box-shadow:$shadow-sm}.metric-card.primary{border-top:5rpx solid $primary-color}.metric-card.success{border-top:5rpx solid $success-color}.metric-label{color:$text-color-secondary;font-size:$font-size-xs}.metric-value{margin-top:7rpx;color:$text-color;font-size:38rpx;font-weight:800}.metric-value.money{font-size:30rpx}.metric-desc{margin-top:7rpx;color:$text-color-hint;font-size:20rpx}.quality-card,.list-card{padding:5rpx 22rpx;border:$glass-border-soft;border-radius:26rpx;background:$surface-gradient;box-shadow:$shadow-sm}.quality-item{padding:20rpx 0;border-bottom:1rpx solid $divider-color}.quality-item:last-child{border-bottom:0}.quality-top{display:flex;justify-content:space-between;color:$text-color;font-size:$font-size-sm;font-weight:700}.tone-success{color:$success-color}.tone-primary{color:$primary-color}.tone-danger{color:$error-color}.tone-warning{color:$warning-color}.progress-track{height:10rpx;margin-top:12rpx;border-radius:$radius-round;background:#edf1f6;overflow:hidden}.progress-value{height:100%;border-radius:$radius-round}.progress-value.success{background:$success-color}.progress-value.primary{background:$primary-color}.progress-value.danger{background:$error-color}.progress-value.warning{background:$warning-color}.quality-desc{margin-top:8rpx;color:$text-color-hint;font-size:20rpx}.trend-scroll{padding:18rpx 0;border:$glass-border-soft;border-radius:26rpx;background:#fff}.trend-chart{display:flex;align-items:flex-end;gap:11rpx;min-width:max-content;height:230rpx;padding:0 20rpx}.trend-column{display:flex;flex-direction:column;align-items:center;width:48rpx;height:210rpx}.trend-count{height:29rpx;color:$text-color-secondary;font-size:18rpx}.trend-bar-wrap{display:flex;align-items:flex-end;width:22rpx;height:145rpx;border-radius:$radius-round;background:#edf7f5;overflow:hidden}.trend-bar{width:100%;border-radius:$radius-round;background:linear-gradient(180deg,#38c6b5,#00a89d)}.trend-date{margin-top:7rpx;color:$text-color-hint;font-size:17rpx;white-space:nowrap}.service-row{display:flex;align-items:center;gap:17rpx;padding:20rpx 0;border-bottom:1rpx solid $divider-color}.service-row:last-child{border-bottom:0}.service-copy{flex:1}.service-name{color:$text-color;font-size:$font-size-sm;font-weight:700}.service-track{height:9rpx;margin-top:10rpx;border-radius:$radius-round;background:#edf1f6;overflow:hidden}.service-value{height:100%;border-radius:$radius-round;background:$primary-gradient}.service-meta{text-align:right}.service-meta text:first-child{color:$text-color;font-size:$font-size-sm;font-weight:700}.service-meta text:last-child{margin-top:4rpx;color:$text-color-hint;font-size:20rpx}.alert-summary{display:grid;grid-template-columns:repeat(2,1fr);gap:13rpx;margin-bottom:13rpx}.alert-summary view{padding:18rpx;border-radius:22rpx;background:#fff3ed;text-align:center}.alert-summary text{display:block}.alert-summary text:first-child{color:$warning-color;font-size:29rpx;font-weight:800}.alert-summary text:last-child{margin-top:3rpx;color:$text-color-secondary;font-size:20rpx}.risk-row{display:flex;align-items:center;gap:14rpx;padding:20rpx 0;border-bottom:1rpx solid $divider-color}.risk-row:last-child{border-bottom:0}.risk-dot{width:14rpx;height:14rpx;border-radius:50%}.risk-dot.danger{background:$error-color}.risk-dot.warning{background:$warning-color}.risk-copy{flex:1;min-width:0}.risk-title{color:$text-color;font-size:$font-size-sm;font-weight:700}.risk-desc{margin-top:5rpx;color:$text-color-secondary;font-size:20rpx;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.risk-time{margin-top:5rpx;color:$text-color-hint;font-size:18rpx}.empty-text{padding:28rpx 0;color:$text-color-hint;font-size:$font-size-xs;text-align:center}.loading{display:flex;justify-content:center;padding:180rpx 0}
</style>
