package com.cloudnativeairlines.operations.scheduler;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface FlightInstanceRepository extends JpaRepository<FlightInstance, String> {

    boolean existsByRunId(String runId);

    long countByRunId(String runId);

    List<FlightInstance> findByRunIdOrderByScheduledDepartureAsc(String runId);

    List<FlightInstance> findByRunIdAndAircraftIdOrderByScheduledDepartureAsc(
            String runId, String aircraftId);

    @Transactional
    long deleteByRunId(String runId);
}
