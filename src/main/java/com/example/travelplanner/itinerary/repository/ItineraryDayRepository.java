package com.example.travelplanner.itinerary.repository;

import com.example.travelplanner.itinerary.entity.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {

    List<ItineraryDay> findByItineraryIdOrderByDayNumberAsc(Long itineraryId);

    Optional<ItineraryDay> findByIdAndItineraryId(Long id, Long itineraryId);

    boolean existsByItineraryIdAndDayNumber(Long itineraryId, Integer dayNumber);
}
