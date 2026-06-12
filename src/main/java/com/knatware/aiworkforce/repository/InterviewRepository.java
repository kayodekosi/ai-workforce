package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    Optional<Interview> findFirstByCandidateIdOrderByCreatedAtDesc(Long candidateId);
    List<Interview> findByCandidateId(Long candidateId);
}
