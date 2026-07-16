package com.tftgogo.domain.content.repository;

import com.tftgogo.domain.content.entity.ContentRefreshJobStatus;
import com.tftgogo.domain.content.entity.ContentRefreshJobType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRefreshJobStatusRepository
        extends JpaRepository<ContentRefreshJobStatus, ContentRefreshJobType> {
}
