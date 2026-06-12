package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findAllByOrderByAtDesc(Pageable pageable);
}
