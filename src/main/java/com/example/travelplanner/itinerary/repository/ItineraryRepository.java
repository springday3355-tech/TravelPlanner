package com.example.travelplanner.itinerary.repository;

import com.example.travelplanner.itinerary.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    Optional<Itinerary> findByShareToken(String shareToken);
}
