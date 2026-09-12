import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const STORAGE_KEY = 'rag_current_kb'

function load(): { id: number; name: string } | null {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch {
    return null
  }
}

export const useKbStore = defineStore('kb', () => {
  const saved = load()
  const currentKbId = ref<number | null>(saved?.id ?? null)
  const currentKbName = ref<string>(saved?.name ?? '')

  function setKb(id: number, name: string) {
    currentKbId.value = id
    currentKbName.value = name
  }

  function clearKb() {
    currentKbId.value = null
    currentKbName.value = ''
  }

  // 持久化「当前知识库」，刷新页面后不丢失（问答历史存储 key 也依赖它）
  watch([currentKbId, currentKbName], () => {
    try {
      if (currentKbId.value == null) {
        localStorage.removeItem(STORAGE_KEY)
      } else {
        localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify({ id: currentKbId.value, name: currentKbName.value })
        )
      }
    } catch {
      /* 忽略存储异常 */
    }
  })

  return { currentKbId, currentKbName, setKb, clearKb }
})
