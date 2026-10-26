package com.cloudnativeairlines.operations.scheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface RunRepository extends JpaRepository<Run, String> {

    @Transactional
    long deleteByRunId(String runId);
}
