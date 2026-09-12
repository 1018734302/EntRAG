<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Boxes, FolderOpen, Trash2, Database } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppShell from '@/components/AppShell.vue'
import { listKb, createKb, deleteKb } from '@/api/http'
import { useKbStore } from '@/stores/kb'
import type { KnowledgeBase } from '@/api/types'

const router = useRouter()
const kbStore = useKbStore()
const list = ref<KnowledgeBase[]>([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const form = ref({ name: '', description: '', embeddingModel: 'qwen3-embedding:4b' })

async function load() {
  loading.value = true
  try {
    list.value = await listKb()
  } finally {
    loading.value = false
  }
}

function openNew() {
  form.value = { name: '', description: '', embeddingModel: 'qwen3-embedding:4b' }
  dialog.value = true
}

async function save() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请填写知识库名称')
    return
  }
  saving.value = true
  try {
    await createKb(form.value.name.trim(), form.value.description.trim(), form.value.embeddingModel)
    ElMessage.success('创建成功')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(item: KnowledgeBase) {
  await ElMessageBox.confirm(`确认删除知识库「${item.name}」及其全部文档？`, '删除确认', {
    type: 'warning'
  })
  await deleteKb(item.id)
  ElMessage.success('已删除')
  await load()
}

function openDocs(item: KnowledgeBase) {
  kbStore.setKb(item.id, item.name)
  router.push(`/kb/${item.id}/docs`)
}

function fmtTime(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="flex gap-6">
      <!-- 左侧知识库列表 -->
      <aside class="w-64 shrink-0">
        <div class="glass rounded-2xl p-4">
          <button class="btn-brand w-full py-2 flex items-center justify-center gap-2 mb-4" @click="openNew">
            <Plus :size="16" /> 新建知识库
          </button>
          <p class="text-xs text-ink-soft mb-2 px-1">我的知识库</p>
          <div class="space-y-1 max-h-[60vh] overflow-auto">
            <button
              v-for="item in list"
              :key="item.id"
              class="w-full text-left px-3 py-2.5 rounded-xl hover:bg-brand/5 transition flex items-center gap-2 group"
              @click="openDocs(item)"
            >
              <Database :size="16" class="text-brand shrink-0" />
              <span class="truncate text-sm text-ink group-hover:text-brand">{{ item.name }}</span>
            </button>
            <p v-if="!list.length" class="text-xs text-ink-soft px-2 py-3">暂无知识库</p>
          </div>
        </div>
      </aside>

      <!-- 主内容卡片网格 -->
      <section class="flex-1">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-ink flex items-center gap-2">
            <Boxes :size="20" class="text-brand" /> 知识库概览
          </h2>
          <span class="text-sm text-ink-soft">共 {{ list.length }} 个</span>
        </div>

        <div v-if="loading" class="text-sm text-ink-soft">加载中…</div>
        <div v-else-if="!list.length" class="glass rounded-2xl p-12 text-center text-ink-soft">
          <FolderOpen :size="40" class="mx-auto mb-3 text-brand/50" />
          还没有知识库，点击左侧「新建知识库」开始构建你的私有知识库
        </div>

        <div v-else class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          <div
            v-for="item in list"
            :key="item.id"
            class="glass rounded-2xl p-5 hover:shadow-soft transition group"
          >
            <div class="flex items-start justify-between">
              <div class="w-10 h-10 rounded-xl bg-brand/10 text-brand flex items-center justify-center">
                <Database :size="18" />
              </div>
              <el-button text :icon="Trash2" size="small" aria-label="删除知识库" class="!text-rose-500" @click="remove(item)" />
            </div>
            <h3 class="mt-3 font-semibold text-ink truncate">{{ item.name }}</h3>
            <p class="text-xs text-ink-soft mt-1 h-8 overflow-hidden">{{ item.description || '暂无描述' }}</p>
            <div class="mt-3 flex items-center gap-2 flex-wrap">
              <el-tag size="small" type="info" effect="plain">{{ item.embeddingModel }}</el-tag>
            </div>
            <div class="mt-4 flex items-center justify-between">
              <span class="text-[11px] text-ink-soft">更新 {{ fmtTime(item.updatedAt) }}</span>
              <button class="text-sm text-brand font-medium hover:underline" @click="openDocs(item)">管理文档 →</button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <el-dialog v-model="dialog" title="新建知识库" width="460px">
      <div class="space-y-4">
        <div>
          <label class="text-sm text-ink-soft">名称</label>
          <el-input v-model="form.name" placeholder="如：产品手册库" class="mt-1" />
        </div>
        <div>
          <label class="text-sm text-ink-soft">描述</label>
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="这个知识库用来放什么资料？" class="mt-1" />
        </div>
        <div>
          <label class="text-sm text-ink-soft">嵌入模型</label>
          <el-select v-model="form.embeddingModel" class="w-full mt-1">
            <el-option label="qwen3-embedding:4b（本地）" value="qwen3-embedding:4b" />
          </el-select>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建</el-button>
      </template>
    </el-dialog>
  </AppShell>
</template>
