<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UploadCloud, Eye, Trash2, FileText, ArrowLeft, Loader2 } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppShell from '@/components/AppShell.vue'
import { listDocs, uploadDoc, deleteDoc, queryIngest, previewChunks, listKb } from '@/api/http'
import { useKbStore } from '@/stores/kb'
import type { DocMeta, IngestStatus } from '@/api/types'

const route = useRoute()
const router = useRouter()
const kbStore = useKbStore()
const kbId = Number(route.params.id)

const docs = ref<DocMeta[]>([])
const uploading = ref(false)
const progress = ref<IngestStatus | null>(null)
const drawer = ref(false)
const previewData = ref<string[]>([])
const previewLoading = ref(false)
let timer: any = null

function statusType(s: string) {
  if (s === 'DONE') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'PENDING') return 'info'
  return 'warning'
}

function statusText(s: string) {
  return { PENDING: '排队中', PARSING: '解析中', EMBEDDING: '向量化中', DONE: '已入库', FAILED: '失败' }[s] || s
}

function fmtSize(b?: number) {
  if (!b) return '-'
  return b > 1024 * 1024 ? (b / 1024 / 1024).toFixed(1) + ' MB' : (b / 1024).toFixed(0) + ' KB'
}

async function loadDocs() {
  docs.value = await listDocs(kbId)
}

async function doUpload(option: any) {
  uploading.value = true
  try {
    const taskId = await uploadDoc(kbId, option.file)
    poll(taskId)
  } catch {
    uploading.value = false
  }
}

function poll(taskId: string) {
  clearInterval(timer)
  timer = setInterval(async () => {
    const st = await queryIngest(taskId)
    progress.value = st
    if (st.status === 'DONE' || st.status === 'FAILED') {
      clearInterval(timer)
      uploading.value = false
      progress.value = null
      await loadDocs()
      if (st.status === 'DONE') ElMessage.success('文档入库完成')
      else ElMessage.error('入库失败：' + (st.message || ''))
    }
  }, 1500)
}

async function remove(doc: DocMeta) {
  await ElMessageBox.confirm(`删除文档「${doc.fileName}」及其向量？`, '删除确认', { type: 'warning' })
  await deleteDoc(kbId, doc.id)
  ElMessage.success('已删除')
  await loadDocs()
}

async function openPreview(doc: DocMeta) {
  drawer.value = true
  previewLoading.value = true
  previewData.value = []
  try {
    previewData.value = await previewChunks(kbId, doc.id, 20)
  } finally {
    previewLoading.value = false
  }
}

onMounted(async () => {
  const all = await listKb()
  const found = all.find((k) => k.id === kbId)
  if (found) kbStore.setKb(found.id, found.name)
  await loadDocs()
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <AppShell>
    <div class="mb-4 flex items-center gap-3">
      <button class="text-ink-soft hover:text-brand flex items-center gap-1 text-sm" @click="router.push('/kb')">
        <ArrowLeft :size="16" /> 返回知识库
      </button>
      <h2 class="text-lg font-semibold text-ink">文档管理 · {{ kbStore.currentKbName || ('库 #' + kbId) }}</h2>
    </div>

    <!-- 上传区 -->
    <div class="glass rounded-2xl p-5 mb-5">
      <el-upload
        drag
        :auto-upload="true"
        :show-file-list="false"
        :http-request="doUpload"
        accept=".pdf,.doc,.docx,.md,.txt"
        class="w-full"
      >
        <div class="py-6 flex flex-col items-center gap-2 text-ink-soft">
          <UploadCloud :size="32" class="text-brand" />
          <p class="text-sm">将 PDF / Word / Markdown / TXT 拖拽到此处，或<em class="text-brand not-italic">点击上传</em></p>
        </div>
      </el-upload>

      <div v-if="uploading && progress" class="mt-3 flex items-center gap-3 text-sm text-ink-soft">
        <Loader2 :size="16" class="animate-spin text-brand" />
        <span>正在处理「{{ progress.fileName }}」：{{ statusText(progress.status) }}</span>
        <span v-if="progress.totalChunks">（{{ progress.embeddedChunks }}/{{ progress.totalChunks }} 块）</span>
      </div>
    </div>

    <!-- 文档列表 -->
    <div class="glass rounded-2xl p-5">
      <div v-if="!docs.length" class="text-center text-ink-soft py-10">
        <FileText :size="36" class="mx-auto mb-2 text-brand/40" /> 暂无文档，上传后自动解析入库
      </div>
      <div v-else class="divide-y divide-white/60">
        <div v-for="doc in docs" :key="doc.id" class="flex items-center gap-4 py-3">
          <div class="w-9 h-9 rounded-lg bg-brand/10 text-brand flex items-center justify-center shrink-0">
            <FileText :size="16" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm text-ink truncate">{{ doc.fileName }}</p>
            <p class="text-xs text-ink-soft">{{ fmtSize(doc.fileSize) }} · 更新 {{ (doc.updatedAt || '').replace('T', ' ').slice(0, 19) }}</p>
          </div>
          <el-tag :type="statusType(doc.status)" size="small" effect="light">{{ statusText(doc.status) }}</el-tag>
          <span v-if="doc.status === 'DONE'" class="text-xs text-ink-soft w-16 text-right">{{ doc.chunkCount }} 块</span>
          <div class="flex items-center gap-1">
            <el-button text :icon="Eye" size="small" aria-label="预览分块" :disabled="doc.status !== 'DONE'" @click="openPreview(doc)" />
            <el-button text :icon="Trash2" size="small" aria-label="删除文档" class="!text-rose-500" @click="remove(doc)" />
          </div>
        </div>
      </div>
    </div>

    <el-drawer v-model="drawer" title="分块预览" size="40%">
      <div v-if="previewLoading" class="text-sm text-ink-soft">加载中…</div>
      <div v-else-if="!previewData.length" class="text-sm text-ink-soft">暂无分块数据</div>
      <div v-else class="space-y-3">
        <div v-for="(chunk, i) in previewData" :key="i" class="rounded-xl border border-white/70 bg-white/60 p-3">
          <p class="text-[11px] text-brand mb-1 font-medium">#{{ i + 1 }}</p>
          <p class="text-sm text-ink leading-relaxed whitespace-pre-wrap">{{ chunk }}</p>
        </div>
      </div>
    </el-drawer>
  </AppShell>
</template>
