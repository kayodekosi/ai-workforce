package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findAllByOrderByAppliedAtDesc();
}
