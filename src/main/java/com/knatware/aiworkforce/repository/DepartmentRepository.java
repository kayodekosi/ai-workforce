package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
