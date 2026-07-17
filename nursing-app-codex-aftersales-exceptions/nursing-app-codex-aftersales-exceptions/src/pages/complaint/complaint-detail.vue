<template>
  <view>
    <view v-if="detail" class="page-shell">
      <view class="status-card">
        <u-icon name="chat-fill" size="34" color="#FFFFFF" />
        <view>
          <text class="status-title">{{ detail.statusText }}</text>
          <text class="status-desc">投诉编号 {{ detail.complaintId }}</text>
        </view>
      </view>
      <view class="summary-card"><text class="section-title">投诉内容</text><text class="summary-type">{{ detail.typeText }} · {{ detail.order?.serviceItemName }}</text><text class="summary-content">{{ detail.content }}</text><view v-if="detail.decision" class="decision"><text>{{ detail.decision.label }}</text><text v-if="detail.decision.refundAmount">退款 ¥{{ detail.decision.refundAmount }}</text><text>{{ detail.decision.remark }}</text></view></view>
      <view class="timeline-card">
        <text class="section-title">处理进度</text>
        <view class="timeline">
          <view v-for="(track, index) in detail.tracks" :key="track.trackId" class="timeline-item">
            <view class="mark"><view class="dot active" /><view v-if="index < detail.tracks.length - 1" class="line" /></view>
            <view class="copy"><text class="title">{{ track.operator }}</text><text class="desc">{{ track.content }}</text><text class="time">{{ formatTime(track.createTime) }}</text></view>
          </view>
          <view v-if="!detail.tracks.length" class="waiting"><u-icon name="clock" size="20" color="#FF8A5C" /><text>客服正在核实情况，请耐心等待</text></view>
        </view>
        <view class="support"><u-icon name="server-man" size="22" color="#00B8A9" /><text>{{ detail.status === 4 ? '平台正在核验双方材料并作出仲裁。' : '如有补充信息，请联系在线客服。' }}</text></view>
      </view>
    </view>
    <view v-else class="loading-page"><u-loading-icon size="30" color="#3A7BF7" /></view>
  </view>
</template>
<script setup>import { ref } from 'vue'; import { onLoad } from '@dcloudio/uni-app'; import { useComplaintStore } from '@/store/complaint.js'; const complaintStore=useComplaintStore(); const detail=ref(null); onLoad(async(options)=>{detail.value=await complaintStore.fetchComplaintTracks(String(options.id || ''))}); function formatTime(value){return value?.replace('T',' ').replace('+08:00','').slice(0,16)||'--'}</script>
<style lang="scss" scoped>.page-shell{min-height:100vh;padding:30rpx $spacing-base 50rpx;background:$page-gradient}.status-card{display:flex;align-items:center;gap:18rpx;padding:28rpx;border-radius:28rpx;background:linear-gradient(135deg,#ff9a6c,#ff7b55);box-shadow:$shadow-float;color:#fff}.status-title{display:block;font-size:34rpx;font-weight:700}.status-desc{display:block;margin-top:5rpx;font-size:$font-size-xs;opacity:.8}.summary-card,.timeline-card{margin-top:22rpx;padding:28rpx 24rpx;border:$glass-border-soft;border-radius:30rpx;background:$surface-gradient;box-shadow:$shadow-sm}.section-title{display:block;margin-bottom:20rpx;color:$text-color;font-size:$font-size-md;font-weight:700}.summary-type,.summary-content,.decision text{display:block}.summary-type{color:$warning-color;font-size:$font-size-sm;font-weight:600}.summary-content{margin-top:12rpx;color:$text-color-secondary;font-size:$font-size-sm;line-height:1.65}.decision{margin-top:20rpx;padding:18rpx;border-radius:20rpx;background:#e9fbf7;color:$success-color;font-size:$font-size-xs;line-height:1.7}.timeline-item{display:flex;min-height:116rpx}.mark{position:relative;width:38rpx;flex-shrink:0}.dot{width:20rpx;height:20rpx;margin-top:3rpx;border-radius:50%;background:$border-color}.dot.active{border:5rpx solid rgba(58,123,247,.18);background:$primary-color}.line{position:absolute;left:9rpx;top:28rpx;bottom:0;width:2rpx;background:$border-color}.copy{flex:1;padding-bottom:26rpx}.title{display:block;color:$text-color;font-size:$font-size-sm;font-weight:600}.desc{display:block;margin-top:7rpx;color:$text-color-secondary;font-size:$font-size-sm;line-height:1.6}.time{display:block;margin-top:7rpx;color:$text-color-disabled;font-size:$font-size-xs}.waiting,.support{display:flex;align-items:center;gap:10rpx;padding:20rpx;border-radius:20rpx;font-size:$font-size-xs}.waiting{background:#fff2eb;color:$text-color-secondary}.support{margin-top:22rpx;background:#e9fbf7;color:$text-color-secondary}.loading-page{min-height:100vh;display:flex;align-items:center;justify-content:center;background:$page-gradient}</style>
