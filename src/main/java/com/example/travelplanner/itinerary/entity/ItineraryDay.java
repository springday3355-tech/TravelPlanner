package com.example.travelplanner.itinerary.entity;

import com.example.travelplanner.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * docs/architecture/TravelPlanner-DB설계.md 2.3절과 정합.
 */
@Getter
@Entity
@Table(name = "itinerary_day", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"itinerary_id", "day_number"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryDay extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "itinerary_id", nullable = false)
    private Long itineraryId;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private LocalDate date;

    private ItineraryDay(Long itineraryId, Integer dayNumber, LocalDate date) {
        this.itineraryId = itineraryId;
        this.dayNumber = dayNumber;
        this.date = date;
    }

    public static ItineraryDay create(Long itineraryId, Integer dayNumber, LocalDate date) {
        return new ItineraryDay(itineraryId, dayNumber, date);
    }
}
