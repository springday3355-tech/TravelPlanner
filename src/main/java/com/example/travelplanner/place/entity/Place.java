package com.example.travelplanner.place.entity;

import com.example.travelplanner.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 장소(POI). docs/architecture/TravelPlanner-DB설계.md 2.6절과 정합.
 * 1단계: 등록/수정/삭제는 ADMIN 권한만 허용 (정책 문서 3절, 권한 범위는 결정 대기 상태의 잠정값).
 */
@Getter
@Entity
@Table(name = "place")
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

    @Column(name = "price_range", length = 20)
    private String priceRange;

    @Column(name = "open_hours", length = 255)
    private String openHours;

    private Place(String name, PlaceCategory category, String region, String address,
                   BigDecimal latitude, BigDecimal longitude, String priceRange, String openHours) {
        this.name = name;
        this.category = category;
        this.region = region;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priceRange = priceRange;
        this.openHours = openHours;
    }

    public static Place of(String name, PlaceCategory category, String region, String address,
                            BigDecimal latitude, BigDecimal longitude, String priceRange, String openHours) {
        return new Place(name, category, region, address, latitude, longitude, priceRange, openHours);
    }

    public void update(String name, PlaceCategory category, String region, String address,
                        BigDecimal latitude, BigDecimal longitude, String priceRange, String openHours) {
        this.name = name;
        this.category = category;
        this.region = region;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priceRange = priceRange;
        this.openHours = openHours;
    }
}
