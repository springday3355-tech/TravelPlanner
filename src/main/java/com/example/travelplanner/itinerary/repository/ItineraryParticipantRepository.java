package com.example.travelplanner.itinerary.repository;

import com.example.travelplanner.itinerary.entity.ItineraryParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItineraryParticipantRepository extends JpaRepository<ItineraryParticipant, Long> {

    List<ItineraryParticipant> findByItineraryId(Long itineraryId);

    List<ItineraryParticipant> findByMemberId(Long memberId);

    Optional<ItineraryParticipant> findByItineraryIdAndMemberId(Long itineraryId, Long memberId);

    boolean existsByItineraryIdAndMemberId(Long itineraryId, Long memberId);

    Optional<ItineraryParticipant> findByIdAndItineraryId(Long id, Long itineraryId);
}
