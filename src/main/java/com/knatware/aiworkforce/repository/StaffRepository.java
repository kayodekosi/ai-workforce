package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {
}
