<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, ShieldCheck, ArrowRight } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)
const mode = ref<'login' | 'register'>('login')

async function submit() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') await auth.login(username.value, password.value)
    else await auth.register(username.value, password.value)
    ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
    const redirect = (route.query.redirect as string) || '/kb'
    router.replace(redirect)
  } catch {
    /* 错误信息由拦截器统一提示 */
  } finally {
    loading.value = false
  }
}

function toggle() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center px-4 relative overflow-hidden">
    <div class="absolute -top-24 -left-24 w-96 h-96 rounded-full bg-brand/20 blur-3xl animate-floaty" />
    <div class="absolute -bottom-24 -right-24 w-96 h-96 rounded-full bg-brand-light/20 blur-3xl animate-floaty" />

    <div class="glass rounded-3xl w-full max-w-md p-8 relative z-10">
      <div class="flex items-center gap-3 mb-2">
        <div class="w-11 h-11 rounded-2xl bg-gradient-to-br from-brand to-brand-light flex items-center justify-center text-white text-xl font-bold shadow-soft">智</div>
        <div>
          <h1 class="text-xl font-semibold text-ink leading-tight">智枢企业知识库</h1>
          <p class="text-xs text-ink-soft">全本地私有化部署 · 数据不出域</p>
        </div>
      </div>

      <p class="text-sm text-ink-soft mb-6 mt-2">
        {{ mode === 'login' ? '登录以管理你的私有知识库' : '创建账号，开启本地 RAG 之旅' }}
      </p>

      <div class="space-y-4">
        <div class="relative">
          <el-icon class="absolute left-3 top-1/2 -translate-y-1/2 text-ink-soft"><User :size="16" /></el-icon>
          <input v-model="username" placeholder="账号" aria-label="账号" class="w-full pl-9 pr-3 py-2.5 rounded-xl border border-white/70 bg-white/60 outline-none focus:ring-2 focus:ring-brand/40 text-sm" />
        </div>
        <div class="relative">
          <el-icon class="absolute left-3 top-1/2 -translate-y-1/2 text-ink-soft"><Lock :size="16" /></el-icon>
          <input v-model="password" type="password" placeholder="密码" aria-label="密码" class="w-full pl-9 pr-3 py-2.5 rounded-xl border border-white/70 bg-white/60 outline-none focus:ring-2 focus:ring-brand/40 text-sm" @keyup.enter="submit" />
        </div>

        <div class="flex items-center justify-between text-xs text-ink-soft">
          <label class="flex items-center gap-1.5 cursor-pointer">
            <input type="checkbox" class="accent-brand" checked /> 记住我
          </label>
          <a class="hover:text-brand cursor-pointer" @click="toggle">{{ mode === 'login' ? '注册新账号' : '返回登录' }}</a>
        </div>

        <button class="btn-brand w-full py-2.5 flex items-center justify-center gap-2" :disabled="loading" @click="submit">
          <span v-if="loading" class="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
          <span>{{ mode === 'login' ? '登录' : '注册并登录' }}</span>
          <ArrowRight v-if="!loading" :size="16" />
        </button>
      </div>

      <div class="mt-6 flex items-center gap-2 text-xs text-ink-soft justify-center">
        <el-icon><ShieldCheck :size="14" /></el-icon>
        模型本地运行 · 零 API 成本 · 私有化交付
      </div>
      <p class="mt-3 text-center text-[11px] text-ink-soft/70">演示账号：admin / admin123</p>
    </div>
  </div>
</template>
