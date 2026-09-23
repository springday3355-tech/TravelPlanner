package com.example.travelplanner.itinerary.entity;

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

import java.time.LocalDate;

/**
 * docs/architecture/TravelPlanner-DB설계.md 2.2절과 정합.
 * owner_id는 auth 도메인 Member를 JPA 연관관계가 아닌 ID 참조로만 저장한다(도메인 간 직접 참조 금지, SA 문서 3절).
 * version(낙관적 락)은 3단계 전까지 추가하지 않는다(DB설계 문서 2.2절 비고).
 */
@Getter
@Entity
@Table(name = "itinerary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Itinerary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    private Integer budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItineraryStatus status;

    @Column(name = "share_token", unique = true, length = 64)
    private String shareToken;

    private Itinerary(Long ownerId, String title, String city, LocalDate startDate, LocalDate endDate, Integer budget) {
        this.ownerId = ownerId;
        this.title = title;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budget = budget;
        this.status = ItineraryStatus.DRAFT;
    }

    public static Itinerary create(Long ownerId, String title, String city, LocalDate startDate, LocalDate endDate, Integer budget) {
        return new Itinerary(ownerId, title, city, startDate, endDate, budget);
    }

    public void update(String title, String city, LocalDate startDate, LocalDate endDate, Integer budget, ItineraryStatus status) {
        this.title = title;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budget = budget;
        this.status = status;
    }

    public void issueShareToken(String token) {
        this.shareToken = token;
    }
}
