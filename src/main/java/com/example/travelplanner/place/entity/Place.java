package com.example.travelplanner.place.entity;

import com.example.travelplanner.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 장소(POI). docs/architecture/TravelPlanner-DB설계.md 2.6절과 정합.
 * 1단계: 등록/수정/삭제는 ADMIN 권한만 허용 (정책 문서 3절, 권한 범위는 결정 대기 상태의 잠정값).
 */
@Getter
@Entity
@Table(
        name = "place",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_place_source_external_id",
                columnNames = {"source", "external_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlaceCategory category;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(length = 255)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "price_range", length = 50)
    private String priceRange;

    @Column(name = "open_hours", length = 1000)
    private String openHours;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(name = "estimated_cost")
    private Integer estimatedCost;

    @Column(name = "recommended_visit_minutes")
    private Integer recommendedVisitMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlaceSource source;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "place_opening_hour", joinColumns = @JoinColumn(name = "place_id"))
    private List<PlaceOpeningHour> openingHours = new ArrayList<>();

    private Place(String name, PlaceCategory category, String region, String address,
                   BigDecimal latitude, BigDecimal longitude, String priceRange, String openHours,
                   LocalTime openingTime, LocalTime closingTime, Integer estimatedCost,
                   Integer recommendedVisitMinutes, PlaceSource source, String externalId,
                   LocalDateTime lastSyncedAt, List<PlaceOpeningHour> openingHours) {
        this.name = name;
        this.category = category;
        this.region = region;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priceRange = priceRange;
        this.openHours = openHours;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.estimatedCost = estimatedCost;
        this.recommendedVisitMinutes = recommendedVisitMinutes;
        this.source = source;
        this.externalId = externalId;
        this.lastSyncedAt = lastSyncedAt;
        replaceOpeningHours(openingHours);
    }

    public static Place of(String name, PlaceCategory category, String region, String address,
                            BigDecimal latitude, BigDecimal longitude, String priceRange, String openHours,
                            LocalTime openingTime, LocalTime closingTime, Integer estimatedCost,
                            Integer recommendedVisitMinutes, List<PlaceOpeningHour> openingHours) {
        return new Place(name, category, region, address, latitude, longitude, priceRange, openHours,
                openingTime, closingTime, estimatedCost, recommendedVisitMinutes,
                PlaceSource.MANUAL, null, null, openingHours);
    }

    public static Place imported(
            String name, PlaceCategory category, String region, String address,
            BigDecimal latitude, BigDecimal longitude, String priceRange, String openHours,
            Integer estimatedCost, Integer recommendedVisitMinutes, PlaceSource source,
            String externalId, LocalDateTime lastSyncedAt, List<PlaceOpeningHour> openingHours
    ) {
        return new Place(name, category, region, address, latitude, longitude, priceRange, openHours,
                null, null, estimatedCost, recommendedVisitMinutes, source, externalId, lastSyncedAt, openingHours);
    }

    public void update(String name, PlaceCategory category, String region, String address,
                        BigDecimal latitude, BigDecimal longitude, String priceRange, String openHours,
                        LocalTime openingTime, LocalTime closingTime, Integer estimatedCost,
                        Integer recommendedVisitMinutes, List<PlaceOpeningHour> openingHours) {
        this.name = name;
        this.category = category;
        this.region = region;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priceRange = priceRange;
        this.openHours = openHours;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.estimatedCost = estimatedCost;
        this.recommendedVisitMinutes = recommendedVisitMinutes;
        replaceOpeningHours(openingHours);
    }

    public void synchronize(
            String name, PlaceCategory category, String region, String address,
            BigDecimal latitude, BigDecimal longitude, String priceRange, String openHours,
            Integer estimatedCost, Integer recommendedVisitMinutes, LocalDateTime lastSyncedAt,
            List<PlaceOpeningHour> openingHours
    ) {
        this.name = name;
        this.category = category;
        this.region = region;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priceRange = priceRange;
        this.openHours = openHours;
        this.estimatedCost = estimatedCost;
        this.recommendedVisitMinutes = recommendedVisitMinutes;
        this.lastSyncedAt = lastSyncedAt;
        replaceOpeningHours(openingHours);
    }

    private void replaceOpeningHours(List<PlaceOpeningHour> openingHours) {
        this.openingHours.clear();
        if (openingHours != null) {
            this.openingHours.addAll(openingHours);
        }
    }
}
