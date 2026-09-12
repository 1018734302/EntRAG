<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useKbStore } from '@/stores/kb'
import { Boxes, MessageSquare, LogOut } from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const kb = useKbStore()

function go(path: string) {
  router.push(path)
}
function logout() {
  auth.logout()
  router.replace('/login')
}
function navClass(name: string) {
  const active = route.name === name || (name === 'kb' && route.name === 'docs')
  return active
    ? 'flex items-center gap-1 px-3 py-2 rounded-lg text-sm text-brand bg-brand/10 font-medium'
    : 'flex items-center gap-1 px-3 py-2 rounded-lg text-sm text-ink-soft hover:text-brand hover:bg-brand/5 transition'
}
</script>

<template>
  <div class="min-h-screen">
    <header class="sticky top-0 z-30 glass">
      <div class="mx-auto max-w-7xl px-6 h-16 flex items-center justify-between">
        <div class="flex items-center gap-8">
          <div class="flex items-center gap-2 cursor-pointer" @click="go('/kb')">
            <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-brand to-brand-light flex items-center justify-center text-white font-bold shadow-soft">智</div>
            <span class="font-semibold text-ink">智枢企业知识库</span>
          </div>
          <nav class="hidden md:flex items-center gap-1">
            <button :class="navClass('kb')" @click="go('/kb')"><Boxes :size="16" /> 知识库</button>
            <button :class="navClass('chat')" @click="go('/chat')"><MessageSquare :size="16" /> 问答</button>
          </nav>
        </div>
        <div class="flex items-center gap-3">
          <span v-if="kb.currentKbName" class="hidden sm:inline text-sm text-ink-soft">
            当前库：<b class="text-ink">{{ kb.currentKbName }}</b>
          </span>
          <el-dropdown trigger="click">
            <span class="flex items-center gap-2 cursor-pointer outline-none">
              <div class="w-8 h-8 rounded-full bg-brand/10 text-brand flex items-center justify-center font-medium">{{ auth.username.charAt(0) }}</div>
              <span class="text-sm text-ink">{{ auth.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">
                  <el-icon><LogOut :size="14" /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-7xl px-6 py-6">
      <slot />
    </main>
  </div>
</template>
