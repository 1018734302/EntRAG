export interface AuthResponse {
  token: string
  username: string
  userId: number
}

export interface KnowledgeBase {
  id: number
  name: string
  description: string
  ownerId: number
  embeddingModel: string
  createdAt: string
  updatedAt: string
}

export interface DocMeta {
  id: number
  kbId: number
  fileName: string
  fileSize: number
  contentType: string
  status: string
  chunkCount: number
  error: string
  createdAt: string
  updatedAt: string
}

export interface IngestStatus {
  taskId: string
  kbId: number
  docId: number
  fileName: string
  status: string
  totalChunks: number
  embeddedChunks: number
  message: string
}

export interface Reference {
  index: number
  fileName: string
  chunkIndex: number
  snippet: string
}

export interface ChatMessage {
  role: 'user' | 'ai'
  content: string
  references?: Reference[]
}
