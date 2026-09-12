package com.example.rag.service;

import com.example.rag.model.entity.DocMeta;
import com.example.rag.service.dto.IngestStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentIngestService {

    /** 提交入库任务，返回 taskId（异步处理，前端轮询状态） */
    String ingest(Long kbId, MultipartFile file);

    /** 查询任务进度 */
    IngestStatus queryStatus(String taskId);

    /** 文档列表 */
    List<DocMeta> listDocs(Long kbId);

    /** 文档详情 */
    DocMeta getDoc(Long kbId, Long docId);

    /** 删除文档及其向量 */
    void deleteDocument(Long kbId, Long docId);

    /** 分块预览（按 docId 从向量库取回原文片段） */
    List<String> previewChunks(Long kbId, Long docId, int limit);
}
