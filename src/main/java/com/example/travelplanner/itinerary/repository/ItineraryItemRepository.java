package com.example.travelplanner.itinerary.repository;

import com.example.travelplanner.itinerary.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

    List<ItineraryItem> findByItineraryDayIdOrderByVisitOrderAsc(Long itineraryDayId);

    Optional<ItineraryItem> findByIdAndItineraryDayId(Long id, Long itineraryDayId);

    boolean existsByItineraryDayIdAndVisitOrder(Long itineraryDayId, Integer visitOrder);

    boolean existsByItineraryDayIdAndVisitOrderAndIdNot(Long itineraryDayId, Integer visitOrder, Long id);

    boolean existsByPlaceId(Long placeId);
}
