package com.example.travelplanner.place.controller;

import com.example.travelplanner.common.response.ApiResponse;
import com.example.travelplanner.place.dto.PlaceRequest;
import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.dto.ExternalPlaceImportRequest;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.place.service.PlaceService;
import com.example.travelplanner.place.service.PlaceImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * docs/architecture/TravelPlanner-API명세.md 3절.
 * 등록/수정/삭제 권한 범위(관리자 전용 vs 사용자 개방)는 결정 대기 상태 —
 * SecurityConfig에서 인증만 요구하고, 세부 role 검사(ADMIN 한정)는 정책이 확정된 뒤 추가한다.
 */
@Validated
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final PlaceImportService placeImportService;

    @GetMapping
    public ApiResponse<List<PlaceResponse>> search(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) PlaceCategory category
    ) {
        return ApiResponse.success(placeService.search(region, category));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlaceResponse> getPlace(@PathVariable Long id) {
        return ApiResponse.success(placeService.getPlace(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlaceResponse>> create(@Valid @RequestBody PlaceRequest request) {
        PlaceResponse response = placeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> importPlaces(
            @Valid @RequestBody ExternalPlaceImportRequest request
    ) {
        List<PlaceResponse> response = placeImportService.importPlaces(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlaceResponse> update(@PathVariable Long id, @Valid @RequestBody PlaceRequest request) {
        return ApiResponse.success(placeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        placeService.delete(id);
        return ApiResponse.success();
    }
}
