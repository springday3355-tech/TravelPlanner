package com.example.travelplanner.place.service;

import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.place.entity.PlaceSource;
import com.example.travelplanner.place.provider.ProviderOpeningHour;
import com.example.travelplanner.place.provider.ProviderPlace;
import com.example.travelplanner.place.repository.PlaceRepository;
import com.example.travelplanner.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(PlaceImportPersistenceService.class)
class PlaceImportPersistenceServiceTest extends RepositoryTestSupport {

    @Autowired
    private PlaceImportPersistenceService persistenceService;

    @Autowired
    private PlaceRepository placeRepository;

    @Test
    @DisplayName("같은 외부 장소를 다시 가져오면 중복 생성 없이 정보와 영업시간을 갱신한다")
    void upsertAll_updatesExistingExternalPlace() {
        ProviderPlace first = providerPlace("처음 이름", "09:00", "18:00");
        ProviderPlace updated = providerPlace("변경된 이름", "10:00", "20:00");

        persistenceService.upsertAll(PlaceSource.GOOGLE_PLACES, List.of(first));
        List<PlaceResponse> result = persistenceService.upsertAll(PlaceSource.GOOGLE_PLACES, List.of(updated));

        assertEquals(1, placeRepository.count());
        assertEquals("변경된 이름", result.getFirst().name());
        assertEquals("GOOGLE_PLACES", result.getFirst().source());
        assertEquals("google-id-1", result.getFirst().externalId());
        assertNotNull(result.getFirst().lastSyncedAt());
        assertEquals(LocalTime.of(10, 0), result.getFirst().openingHours().getFirst().openingTime());
    }

    private ProviderPlace providerPlace(String name, String openingTime, String closingTime) {
        return new ProviderPlace(
                "google-id-1", name, PlaceCategory.SIGHT, "서울", "서울특별시 테스트로 1",
                new BigDecimal("37.5665000"), new BigDecimal("126.9780000"),
                "PRICE_LEVEL_MODERATE", "월요일 영업", null, 90,
                List.of(new ProviderOpeningHour(
                        DayOfWeek.MONDAY, LocalTime.parse(openingTime), LocalTime.parse(closingTime), false
                ))
        );
    }
}
