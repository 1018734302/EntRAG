<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Send, Plus, Sparkles, Bot, FileText, Boxes } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import { streamChat } from '@/api/sse'
import { useKbStore } from '@/stores/kb'
import type { ChatMessage, Reference } from '@/api/types'

const router = useRouter()
const kb = useKbStore()

interface Session {
  id: number
  title: string
  messages: ChatMessage[]
}
const sessions = ref<Session[]>([])
const activeId = ref(0)
const input = ref('')
const streaming = ref(false)
const seq = ref(0)
const scrollRef = ref<HTMLElement | null>(null)

const active = computed(() => sessions.value.find((s) => s.id === activeId.value) || null)
const hasKb = computed(() => kb.currentKbId !== null)

function newSession() {
  const id = ++seq.value
  sessions.value.push({ id, title: '新对话', messages: [] })
  activeId.value = id
  saveSessions()
}

/* 会话持久化：按「用户 + 知识库」维度存 localStorage，刷新/切页不丢历史 */
function storageKey() {
  const u = localStorage.getItem('rag_user') || 'anon'
  return `rag_chat_v1_${u}_${kb.currentKbId ?? 'none'}`
}

function saveSessions() {
  try {
    localStorage.setItem(
      storageKey(),
      JSON.stringify({ sessions: sessions.value, activeId: activeId.value, seq: seq.value })
    )
  } catch {
    /* 忽略存储配额或隐私模式异常 */
  }
}

function loadSessions(): boolean {
  try {
    const raw = localStorage.getItem(storageKey())
    if (!raw) return false
    const data = JSON.parse(raw)
    if (!data || !Array.isArray(data.sessions) || data.sessions.length === 0) return false
    sessions.value = data.sessions
    activeId.value = data.activeId ?? data.sessions[0].id
    seq.value = data.seq ?? data.sessions.length
    return true
  } catch {
    return false
  }
}

// 内容变化即落盘；切换知识库时载入该库对应的历史
watch(sessions, saveSessions, { deep: true })
watch(activeId, saveSessions)
watch(
  () => kb.currentKbId,
  () => {
    if (!loadSessions()) {
      sessions.value = []
      newSession()
    }
  }
)

async function send() {
  if (!hasKb.value || !input.value.trim() || streaming.value) return
  if (!active.value) newSession()
  const session = active.value!
  const question = input.value.trim()
  input.value = ''

  session.messages.push({ role: 'user', content: question })
  if (session.messages.length === 1) session.title = question.slice(0, 12)
  // 必须用 reactive：push 进数组后 Vue 仅在读取时才包代理，
  // 若用普通对象，onToken 里对原始对象的修改不会触发视图更新，导致答案一次性出现。
  const ai = reactive<ChatMessage>({ role: 'ai', content: '' })
  session.messages.push(ai)
  streaming.value = true

  await streamChat(kb.currentKbId!, question, {
    onReferences: (refs: Reference[]) => {
      ai.references = refs
    },
    onToken: (t: string) => {
      ai.content += t
      scrollBottom()
    },
    onDone: () => {
      streaming.value = false
    },
    onError: () => {
      streaming.value = false
      ai.content += '\n[生成中断，请重试]'
    }
  })
}

function scrollBottom() {
  nextTick(() => {
    if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  })
}

watch(
  () => active.value?.messages.length,
  () => scrollBottom()
)

onMounted(() => {
  if (!hasKb.value) return
  // 已有历史则恢复，只有首次进入才开新会话
  if (!loadSessions()) newSession()
})
</script>

<template>
  <AppShell>
    <div v-if="!hasKb" class="glass rounded-2xl p-12 text-center text-ink-soft">
      <Boxes :size="40" class="mx-auto mb-3 text-brand/50" />
      请先在「知识库」中选择一个库，再开始提问
      <div class="mt-4">
        <button class="btn-brand px-4 py-2" @click="router.push('/kb')">去选择知识库</button>
      </div>
    </div>

    <div v-else class="glass rounded-2xl flex" style="height: calc(100vh - 160px)">
      <!-- 会话列表 -->
      <aside class="w-56 shrink-0 border-r border-white/60 p-3 flex flex-col">
        <button class="btn-brand w-full py-2 flex items-center justify-center gap-2 mb-3" @click="newSession">
          <Plus :size="16" /> 新对话
        </button>
        <div class="flex-1 space-y-1 overflow-auto">
          <button
            v-for="s in sessions"
            :key="s.id"
            class="w-full text-left px-3 py-2 rounded-lg text-sm truncate transition"
            :class="s.id === activeId ? 'bg-brand/10 text-brand font-medium' : 'text-ink-soft hover:bg-brand/5'"
            @click="activeId = s.id"
          >
            {{ s.title }}
          </button>
        </div>
      </aside>

      <!-- 对话区 -->
      <section class="flex-1 flex flex-col">
        <div class="px-4 py-2.5 border-b border-white/60 flex items-center gap-2 text-sm">
          <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          本地模型 qwen3:4b · 检索增强已开启
          <span class="text-ink-soft">（库：{{ kb.currentKbName }}）</span>
        </div>

        <div ref="scrollRef" class="flex-1 overflow-auto p-4 space-y-4">
          <div v-if="!active || !active.messages.length" class="h-full flex flex-col items-center justify-center text-ink-soft">
            <Sparkles :size="36" class="text-brand/50 mb-2" />
            <p>向你的私有知识库提问吧，答案将附带引用来源</p>
          </div>

          <template v-for="(m, i) in active?.messages" :key="i">
            <!-- 用户 -->
            <div v-if="m.role === 'user'" class="flex justify-end">
              <div class="max-w-[75%] bg-gradient-to-br from-brand to-brand-light text-white rounded-2xl rounded-tr-sm px-4 py-2.5 text-sm whitespace-pre-wrap">
                {{ m.content }}
              </div>
            </div>
            <!-- AI -->
            <div v-else class="flex gap-3">
              <div class="w-8 h-8 rounded-lg bg-brand/10 text-brand flex items-center justify-center shrink-0">
                <Bot :size="16" />
              </div>
              <div class="max-w-[80%]">
                <div class="glass rounded-2xl rounded-tl-sm px-4 py-2.5 text-sm text-ink whitespace-pre-wrap leading-relaxed">
                  {{ m.content }}<span v-if="streaming && i === (active?.messages.length ?? 0) - 1" class="inline-block w-1.5 h-4 bg-brand align-middle animate-blink ml-0.5" />
                </div>
                <div v-if="m.references && m.references.length" class="mt-2 flex flex-wrap gap-1.5">
                  <span
                    v-for="(r, ri) in m.references"
                    :key="ri"
                    class="inline-flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full bg-white/70 border border-brand/20 text-brand"
                    :title="r.snippet"
                  >
                    <FileText :size="11" /> {{ r.fileName }} #{{ r.chunkIndex }}
                  </span>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 输入区 -->
        <div class="p-3 border-t border-white/60 flex items-end gap-2">
          <textarea
            v-model="input"
            rows="2"
            placeholder="输入你的问题，Enter 发送 / Shift+Enter 换行"
            class="flex-1 resize-none rounded-xl border border-white/70 bg-white/60 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand/40"
            @keydown.enter.exact.prevent="send"
          />
          <button class="btn-brand px-4 py-2.5 flex items-center gap-1" :disabled="streaming || !input.trim()" @click="send">
            <Send :size="16" /> 发送
          </button>
        </div>
      </section>
    </div>
  </AppShell>
</template>
