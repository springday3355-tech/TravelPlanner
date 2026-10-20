package com.example.travelplanner.place.repository;

import com.example.travelplanner.place.entity.Place;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.place.entity.PlaceSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByRegionAndCategory(String region, PlaceCategory category);

    List<Place> findByRegion(String region);

    List<Place> findByCategory(PlaceCategory category);

    Optional<Place> findBySourceAndExternalId(PlaceSource source, String externalId);
}
