package com.cloudnativeairlines.operations.scenario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AircraftRepository extends JpaRepository<Aircraft, String> {
}
