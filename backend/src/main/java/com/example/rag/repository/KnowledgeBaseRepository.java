package com.example.rag.repository;

import com.example.rag.model.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    List<KnowledgeBase> findByOwnerId(Long ownerId);
    List<KnowledgeBase> findByOwnerIdAndNameContaining(Long ownerId, String name);
}
