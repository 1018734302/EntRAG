package com.example.rag.service;

import com.example.rag.service.dto.IngestStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 入库任务状态寄存器（内存中），与 DocMeta 持久化状态互补。
 */
@Component
public class IngestTaskRegistry {

    private final Map<String, IngestStatus> map = new ConcurrentHashMap<>();

    public void put(IngestStatus status) {
        map.put(status.getTaskId(), status);
    }

    public IngestStatus get(String taskId) {
        return map.get(taskId);
    }

    public void update(String taskId, Consumer<IngestStatus> consumer) {
        IngestStatus status = map.get(taskId);
        if (status != null) {
            consumer.accept(status);
        }
    }
}
