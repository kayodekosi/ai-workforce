package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findAllByOrderByStartTimeAsc();
}
