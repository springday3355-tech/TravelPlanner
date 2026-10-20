package com.example.travelplanner.common.health;

import com.example.travelplanner.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 하네스(공통 골격 + 테스트 기반)가 정상 동작하는지 확인하기 위한 표본 엔드포인트.
 * 실제 도메인 컨트롤러 구현 시 참고용으로 남겨두고, 배포 상태 점검용으로도 사용한다.
 */
@RestController
public class HealthCheckController {

    @GetMapping("/api/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("OK");
    }
}
