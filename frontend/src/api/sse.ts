/**
 * SSE 流式问答客户端。
 *
 * <b>为什么不用 EventSource？</b>
 * EventSource 不支持自定义请求头，带不了 JWT，所以这里用 fetch + ReadableStream 手动解析。
 *
 * <b>后端事件序列（见 ChatController）：</b>
 * <pre>
 * event:references → 本次引用的文档片段
 * data:"token"     → 每个 token 一条（默认事件名）
 * event:done       → [DONE]
 * </pre>
 */

import type { Reference } from './types'

export interface ChatHandlers {
  onReferences?: (refs: Reference[]) => void
  onToken?: (token: string) => void
  onDone?: () => void
  onError?: (e: any) => void
}

interface SseEvent {
  event: string
  data: string
}

function parseSse(raw: string): SseEvent {
  let event = 'message'
  let data = ''
  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) data += line.slice(5).trim()
  }
  return { event, data }
}

/**
 * 通过 fetch 流式读取 SSE（相比 EventSource 可携带 JWT 请求头）。
 * 后端按 `event: references` / `data:`(token) / `event: done` 三段推送。
 */
export async function streamChat(kbId: number, question: string, handlers: ChatHandlers): Promise<void> {
  const token = localStorage.getItem('rag_token')
  const url = `/api/kb/${kbId}/chat?question=${encodeURIComponent(question)}`
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
      credentials: 'include'
    })
    if (!resp.ok || !resp.body) {
      handlers.onError?.(resp)
      return
    }
    // 逐块读取响应流：每读到一个 chunk 就立刻处理，不等整个响应结束
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      // stream: true 让多字节字符（如中文）跨 chunk 拼接时不会被截断成乱码
      buffer += decoder.decode(value, { stream: true })
      let idx: number
      // SSE 规范：事件之间以空行（\n\n）分隔
      while ((idx = buffer.indexOf('\n\n')) !== -1) {
        const raw = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        const ev = parseSse(raw)
        if (ev.event === 'references') {
          try {
            handlers.onReferences?.(JSON.parse(ev.data))
          } catch {
            /* ignore */
          }
        } else if (ev.event === 'done') {
          handlers.onDone?.()
        } else {
          try {
            handlers.onToken?.(JSON.parse(ev.data))
          } catch {
            handlers.onToken?.(ev.data)
          }
        }
      }
    }
    handlers.onDone?.()
  } catch (e) {
    handlers.onError?.(e)
  }
}
