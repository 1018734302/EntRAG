package com.example.rag.service.dto;

import lombok.Data;

/**
 * 入库任务实时状态，供前端轮询展示进度。
 */
@Data
public class IngestStatus {
    private String taskId;
    private Long kbId;
    private Long docId;
    private String fileName;
    private String status; // PARSING / EMBEDDING / DONE / FAILED
    private int totalChunks;
    private int embeddedChunks;
    private String message;
}
