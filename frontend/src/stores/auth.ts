import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, register as apiRegister } from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('rag_token') || '')
  const username = ref(localStorage.getItem('rag_user') || '')

  async function login(usernameVal: string, password: string) {
    const res = await apiLogin(usernameVal, password)
    token.value = res.token
    username.value = res.username
    localStorage.setItem('rag_token', res.token)
    localStorage.setItem('rag_user', res.username)
  }

  async function register(usernameVal: string, password: string) {
    const res = await apiRegister(usernameVal, password)
    token.value = res.token
    username.value = res.username
    localStorage.setItem('rag_token', res.token)
    localStorage.setItem('rag_user', res.username)
  }

  function logout() {
    token.value = ''
    username.value = ''
    localStorage.removeItem('rag_token')
    localStorage.removeItem('rag_user')
  }

  return { token, username, login, register, logout }
})
