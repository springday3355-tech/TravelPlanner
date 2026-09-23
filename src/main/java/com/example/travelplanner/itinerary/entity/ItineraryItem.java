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

import java.time.LocalTime;

/**
 * docs/architecture/TravelPlanner-DB설계.md 2.4절과 정합.
 * place_id는 place 도메인 Place를 JPA 연관관계가 아닌 ID 참조로만 저장한다(도메인 간 직접 참조 금지).
 */
@Getter
@Entity
@Table(name = "itinerary_item", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"itinerary_day_id", "visit_order"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "itinerary_day_id", nullable = false)
    private Long itineraryDayId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "visit_order", nullable = false)
    private Integer visitOrder;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 500)
    private String memo;

    private ItineraryItem(Long itineraryDayId, Long placeId, Integer visitOrder, LocalTime startTime, LocalTime endTime, String memo) {
        this.itineraryDayId = itineraryDayId;
        this.placeId = placeId;
        this.visitOrder = visitOrder;
        this.startTime = startTime;
        this.endTime = endTime;
        this.memo = memo;
    }

    public static ItineraryItem create(Long itineraryDayId, Long placeId, Integer visitOrder, LocalTime startTime, LocalTime endTime, String memo) {
        return new ItineraryItem(itineraryDayId, placeId, visitOrder, startTime, endTime, memo);
    }

    public void update(Long placeId, Integer visitOrder, LocalTime startTime, LocalTime endTime, String memo) {
        this.placeId = placeId;
        this.visitOrder = visitOrder;
        this.startTime = startTime;
        this.endTime = endTime;
        this.memo = memo;
    }
}
