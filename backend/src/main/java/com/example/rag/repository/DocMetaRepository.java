package com.example.rag.repository;

import com.example.rag.model.entity.DocMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocMetaRepository extends JpaRepository<DocMeta, Long> {
    List<DocMeta> findByKbId(Long kbId);
    List<DocMeta> findByKbIdAndStatus(Long kbId, String status);
}
