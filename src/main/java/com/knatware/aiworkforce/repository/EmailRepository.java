package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailRepository extends JpaRepository<Email, Long> {
    List<Email> findAllByOrderByCreatedAtDesc();
}
