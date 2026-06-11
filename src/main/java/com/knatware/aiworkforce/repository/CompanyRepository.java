package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
