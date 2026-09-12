import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { AuthResponse, KnowledgeBase, DocMeta, IngestStatus } from './types'

// baseURL 为 '/api'：开发环境由 Vite 代理转发到 http://localhost:8080（见 vite.config.ts）
const http = axios.create({ baseURL: '/api', timeout: 60000 })

// 请求拦截：统一为已登录请求带上 JWT
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('rag_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/**
 * 响应拦截：把 AxiosResponse 解包成响应体（即后端的 Result 结构 `{ code, message, data }`）。
 *
 * ⚠️ 关键：这里<b>已经解了一层</b>，所以各业务方法里只能再取一次 `.data`，
 * 写成 `r.data.data` 会拿到 `undefined`。
 * 本项目曾因此导致登录时 `token.value = res.token` 抛 TypeError，
 * 且被页面的空 catch 静默吞掉，表现为"点登录完全没反应"。
 */
http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('rag_token')
      window.location.href = '/login'
    }
    // 注意：后端 500 的默认响应体不含 message 字段，此时会回退成下面的兜底文案。
    // 因此排查"网络请求失败"时，应优先查后端日志里的真实异常。
    const msg = err.response?.data?.message || '网络请求失败'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const r = await http.post<ApiResult<AuthResponse>>('/auth/login', { username, password })
  return r.data
}

export async function register(username: string, password: string): Promise<AuthResponse> {
  const r = await http.post<ApiResult<AuthResponse>>('/auth/register', { username, password })
  return r.data
}

export async function listKb(keyword?: string): Promise<KnowledgeBase[]> {
  const r = await http.get<ApiResult<KnowledgeBase[]>>('/kb', { params: { keyword } })
  return r.data
}

export async function createKb(name: string, description: string, embeddingModel: string): Promise<KnowledgeBase> {
  const r = await http.post<ApiResult<KnowledgeBase>>('/kb', { name, description, embeddingModel })
  return r.data
}

export async function deleteKb(id: number): Promise<void> {
  await http.delete(`/kb/${id}`)
}

export async function uploadDoc(kbId: number, file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  const r = await http.post<ApiResult<string>>(`/kb/${kbId}/docs`, form)
  return r.data
}

export async function listDocs(kbId: number): Promise<DocMeta[]> {
  const r = await http.get<ApiResult<DocMeta[]>>(`/kb/${kbId}/docs`)
  return r.data
}

export async function deleteDoc(kbId: number, docId: number): Promise<void> {
  await http.delete(`/kb/${kbId}/docs/${docId}`)
}

export async function queryIngest(taskId: string): Promise<IngestStatus> {
  const r = await http.get<ApiResult<IngestStatus>>(`/kb/ingest/status/${taskId}`)
  return r.data
}

export async function previewChunks(kbId: number, docId: number, limit = 10): Promise<string[]> {
  const r = await http.get<ApiResult<string[]>>(`/kb/${kbId}/docs/${docId}/chunks`, { params: { limit } })
  return r.data
}
