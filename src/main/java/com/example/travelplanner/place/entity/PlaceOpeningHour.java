package com.example.travelplanner.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceOpeningHour {

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @Column(name = "closes_next_day", nullable = false)
    private boolean closesNextDay;

    private PlaceOpeningHour(
            DayOfWeek dayOfWeek,
            LocalTime openingTime,
            LocalTime closingTime,
            boolean closesNextDay
    ) {
        this.dayOfWeek = dayOfWeek;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.closesNextDay = closesNextDay;
    }

    public static PlaceOpeningHour of(
            DayOfWeek dayOfWeek,
            LocalTime openingTime,
            LocalTime closingTime,
            boolean closesNextDay
    ) {
        return new PlaceOpeningHour(dayOfWeek, openingTime, closingTime, closesNextDay);
    }
}
