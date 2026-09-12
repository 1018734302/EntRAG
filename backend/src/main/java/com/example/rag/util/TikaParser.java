package com.example.rag.util;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 基于 Apache Tika 的文档解析：将 PDF/Word/Markdown/TXT 统一抽取为纯文本。
 */
public class TikaParser {

    public static String parse(MultipartFile file) throws Exception {
        Tika tika = new Tika();
        Metadata metadata = new Metadata();
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        }
        try (InputStream in = file.getInputStream()) {
            return tika.parseToString(in, metadata);
        }
    }

    public static String parseBytes(byte[] content, String fileName) throws Exception {
        Tika tika = new Tika();
        Metadata metadata = new Metadata();
        if (fileName != null) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        }
        try (InputStream in = new java.io.ByteArrayInputStream(content)) {
            return tika.parseToString(in, metadata);
        }
    }
}
