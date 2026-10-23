package com.example.travelplanner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 에러 코드 정의.
 * 도메인이 추가될 때마다(회원/일정/장소/실시간) 접두사를 늘려가며 확장한다.
 * 예: 회원 -> M0xx, 일정 -> P0xx, 장소 -> L0xx
 */
@Getter
public enum ErrorCode {

    // Common (C0xx)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),

    // Auth (A0xx)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않거나 만료된 토큰입니다."),

    // Member (M0xx)
    MEMBER_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "M001", "이미 가입된 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "존재하지 않는 회원입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "M003", "이메일 또는 비밀번호가 올바르지 않습니다."),

    // Itinerary (P0xx)
    ITINERARY_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 일정입니다."),
    ITINERARY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P002", "해당 일정에 대한 접근 권한이 없습니다."),
    ITINERARY_DAY_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "존재하지 않는 일자입니다."),
    ITINERARY_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "P004", "존재하지 않는 방문 항목입니다."),
    PARTICIPANT_ALREADY_EXISTS(HttpStatus.CONFLICT, "P005", "이미 참여 중인 동행자입니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "P006", "존재하지 않는 동행자입니다."),
    INVALID_SHARE_TOKEN(HttpStatus.NOT_FOUND, "P007", "유효하지 않은 공유 링크입니다."),
    INVALID_PARTICIPANT_ROLE(HttpStatus.BAD_REQUEST, "P008", "초대 가능한 역할은 EDITOR 또는 VIEWER입니다."),
    OWNER_PARTICIPANT_CANNOT_BE_REMOVED(HttpStatus.CONFLICT, "P009", "일정 소유자는 동행자에서 제거할 수 없습니다."),

    // Place (L0xx)
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "존재하지 않는 장소입니다."),
    PLACE_IN_USE(HttpStatus.CONFLICT, "L002", "일정에서 사용 중인 장소는 삭제할 수 없습니다."),
    PLACE_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "L003", "외부 장소 서비스가 설정되지 않았습니다."),
    PLACE_PROVIDER_FAILED(HttpStatus.BAD_GATEWAY, "L004", "외부 장소 서비스 호출에 실패했습니다."),

    // Planning (G0xx)
    PLANNING_PLACE_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "G001", "해당 도시에 일정으로 만들 수 있는 장소가 없습니다."),
    PLANNING_PLACE_REGION_MISMATCH(HttpStatus.BAD_REQUEST, "G002", "필수 장소가 여행 도시에 속하지 않습니다."),
    PLANNING_REQUIRED_PLACE_OVER_CAPACITY(HttpStatus.BAD_REQUEST, "G003", "필수 장소 수가 일정에 배치할 수 있는 수를 초과합니다."),
    PLANNING_REQUIRED_PLACE_UNSCHEDULABLE(HttpStatus.UNPROCESSABLE_ENTITY, "G004", "필수 장소를 영업시간과 일정 시간 안에 배치할 수 없습니다."),
    AI_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "G005", "AI 일정 수정 서비스가 설정되지 않았습니다."),
    AI_PROVIDER_FAILED(HttpStatus.BAD_GATEWAY, "G006", "AI 일정 수정 서비스 호출에 실패했습니다."),
    AI_REVISION_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "G007", "AI가 제안한 일정 변경을 적용할 수 없습니다."),
    AI_REVISION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "G008", "AI 일정 수정 요청이 허용 범위를 초과했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
