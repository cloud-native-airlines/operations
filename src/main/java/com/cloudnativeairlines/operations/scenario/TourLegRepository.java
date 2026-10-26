package com.cloudnativeairlines.operations.scenario;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourLegRepository extends JpaRepository<TourLeg, TourLeg.Key> {

    List<TourLeg> findByTourIdOrderBySeqAsc(String tourId);
}
