package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.MailSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailSettingsRepository extends JpaRepository<MailSettings, Long> {
}
