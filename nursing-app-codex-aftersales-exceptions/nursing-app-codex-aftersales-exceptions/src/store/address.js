/**
 * Pinia — 地址管理
 *
 * 对齐 API v1.0（order-service / addresses）：
 * - 列表、新增、编辑、删除、设置默认
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http, { createIdempotentKey } from '@/utils/request.js'

export const useAddressStore = defineStore('address', () => {
  // ===== 状态 =====
  const addresses = ref([])
  const loading = ref(false)

  // ===== 计算属性 =====
  const defaultAddress = computed(() => {
    return addresses.value.find((a) => a.isDefault === 1) || addresses.value[0] || null
  })

  // ===== 方法 =====

  /** 获取地址列表 */
  async function fetchAddresses() {
    loading.value = true
    try {
      const res = await http.get('/api/v1/addresses')
      addresses.value = (res.data || []).map((item) => ({
        ...item,
        addressId: item.addressId == null ? item.addressId : String(item.addressId),
      }))
      return addresses.value
    } finally {
      loading.value = false
    }
  }

  /** 新增地址 */
  async function addAddress(data) {
    const res = await http.post('/api/v1/addresses', data, {
      idempotentKey: createIdempotentKey('address-create'),
    })
    await fetchAddresses() // 刷新列表
    return res.data
  }

  /** 编辑地址 */
  async function updateAddress(addressId, data) {
    await http.patch(`/api/v1/addresses/${addressId}`, data, {
      idempotentKey: createIdempotentKey('address-update'),
    })
    await fetchAddresses()
  }

  /** 删除地址 */
  async function removeAddress(addressId) {
    await http.delete(`/api/v1/addresses/${addressId}`, null, {
      idempotentKey: createIdempotentKey('address-delete'),
    })
    await fetchAddresses()
  }

  /** 设置默认地址 */
  async function setDefault(addressId) {
    await http.put(`/api/v1/addresses/${addressId}/default`, null, {
      idempotentKey: createIdempotentKey('address-default'),
    })
    await fetchAddresses()
  }

  return {
    addresses,
    loading,
    defaultAddress,
    fetchAddresses,
    addAddress,
    updateAddress,
    removeAddress,
    setDefault,
  }
})
