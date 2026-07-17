import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import http from '../api/request.js'

const TOKEN_KEY = 'adminToken'
const USER_KEY = 'adminUser'

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useSessionStore = defineStore('adminSession', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(readStoredUser())
  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(username, password) {
    const res = await http.post('/api/v1/admin/login', { username, password })
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    return user.value
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isAuthenticated, login, logout }
})
