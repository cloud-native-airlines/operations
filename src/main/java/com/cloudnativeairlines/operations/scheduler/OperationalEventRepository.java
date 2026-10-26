package com.cloudnativeairlines.operations.scheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface OperationalEventRepository
        extends JpaRepository<OperationalEvent, OperationalEvent.Key> {

    @Transactional
    long deleteByRunId(String runId);
}
