package com.example.travelplanner.itinerary.service;

import com.example.travelplanner.auth.dto.MemberSummary;
import com.example.travelplanner.auth.service.MemberService;
import com.example.travelplanner.common.exception.BusinessException;
import com.example.travelplanner.common.exception.ErrorCode;
import com.example.travelplanner.itinerary.dto.AddDayRequest;
import com.example.travelplanner.itinerary.dto.CreateItineraryRequest;
import com.example.travelplanner.itinerary.dto.InviteParticipantRequest;
import com.example.travelplanner.itinerary.dto.ItineraryDayResponse;
import com.example.travelplanner.itinerary.dto.ItineraryDetailResponse;
import com.example.travelplanner.itinerary.dto.ItineraryItemRequest;
import com.example.travelplanner.itinerary.dto.ItineraryItemResponse;
import com.example.travelplanner.itinerary.dto.ItinerarySummaryResponse;
import com.example.travelplanner.itinerary.dto.ParticipantResponse;
import com.example.travelplanner.itinerary.dto.ShareLinkResponse;
import com.example.travelplanner.itinerary.dto.UpdateItineraryRequest;
import com.example.travelplanner.itinerary.entity.Itinerary;
import com.example.travelplanner.itinerary.entity.ItineraryDay;
import com.example.travelplanner.itinerary.entity.ItineraryItem;
import com.example.travelplanner.itinerary.entity.ItineraryParticipant;
import com.example.travelplanner.itinerary.entity.ParticipantRole;
import com.example.travelplanner.itinerary.repository.ItineraryDayRepository;
import com.example.travelplanner.itinerary.repository.ItineraryItemRepository;
import com.example.travelplanner.itinerary.repository.ItineraryParticipantRepository;
import com.example.travelplanner.itinerary.repository.ItineraryRepository;
import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

/**
 * 동시 편집 정책: 3단계(낙관적 락) 도입 전까지는 마지막 저장이 이긴다(정책 문서 2절).
 * 이 구현에서 그 이상의 락 로직을 임의로 추가하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItineraryServiceImpl implements ItineraryService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final ItineraryParticipantRepository itineraryParticipantRepository;
    private final MemberService memberService;
    private final PlaceService placeService;

    @Override
    @Transactional
    public ItinerarySummaryResponse create(Long ownerId, CreateItineraryRequest request) {
        validatePeriod(request.startDate(), request.endDate());
        Itinerary itinerary = Itinerary.create(
                ownerId, request.title(), request.city(), request.startDate(), request.endDate(), request.budget()
        );
        Itinerary saved = itineraryRepository.save(itinerary);

        ItineraryParticipant owner = ItineraryParticipant.create(saved.getId(), ownerId, ParticipantRole.OWNER);
        itineraryParticipantRepository.save(owner);

        return ItinerarySummaryResponse.of(saved, ParticipantRole.OWNER);
    }

    @Override
    public List<ItinerarySummaryResponse> getMyItineraries(Long memberId) {
        List<ItineraryParticipant> myParticipations = itineraryParticipantRepository.findByMemberId(memberId);
        List<Long> itineraryIds = myParticipations.stream().map(ItineraryParticipant::getItineraryId).toList();
        if (itineraryIds.isEmpty()) {
            return List.of();
        }

        List<Itinerary> itineraries = itineraryRepository.findAllById(itineraryIds);
        return itineraries.stream()
                .map(itinerary -> ItinerarySummaryResponse.of(itinerary, roleOf(myParticipations, itinerary.getId())))
                .toList();
    }

    @Override
    public ItineraryDetailResponse getDetail(Long itineraryId, Long memberId) {
        Itinerary itinerary = getItineraryOrThrow(itineraryId);
        requireParticipant(itineraryId, memberId);
        return buildDetail(itinerary, true);
    }

    @Override
    public ItineraryDetailResponse getSharedDetail(String shareToken) {
        Itinerary itinerary = itineraryRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_SHARE_TOKEN));
        return buildDetail(itinerary, false);
    }

    @Override
    @Transactional
    public ItineraryDetailResponse update(Long itineraryId, Long memberId, UpdateItineraryRequest request) {
        Itinerary itinerary = getItineraryOrThrow(itineraryId);
        requireEditPermission(itineraryId, memberId);
        validatePeriod(request.startDate(), request.endDate());

        boolean containsOutOfRangeDay = itineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itineraryId).stream()
                .anyMatch(day -> day.getDate().isBefore(request.startDate()) || day.getDate().isAfter(request.endDate()));
        if (containsOutOfRangeDay) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "기존 일자가 변경할 여행 기간을 벗어납니다.");
        }

        itinerary.update(
                request.title(), request.city(), request.startDate(), request.endDate(),
                request.budget(), request.status()
        );
        return buildDetail(itinerary, true);
    }

    @Override
    @Transactional
    public void delete(Long itineraryId, Long memberId) {
        getItineraryOrThrow(itineraryId);
        requireOwner(itineraryId, memberId);

        List<ItineraryDay> days = itineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itineraryId);
        for (ItineraryDay day : days) {
            itineraryItemRepository.deleteAll(itineraryItemRepository.findByItineraryDayIdOrderByVisitOrderAsc(day.getId()));
        }
        itineraryDayRepository.deleteAll(days);
        itineraryParticipantRepository.deleteAll(itineraryParticipantRepository.findByItineraryId(itineraryId));
        itineraryRepository.deleteById(itineraryId);
    }

    @Override
    @Transactional
    public ShareLinkResponse createShareLink(Long itineraryId, Long memberId) {
        Itinerary itinerary = getItineraryOrThrow(itineraryId);
        requireOwner(itineraryId, memberId);

        if (itinerary.getShareToken() == null) {
            itinerary.issueShareToken(generateShareToken());
        }
        return new ShareLinkResponse(itinerary.getShareToken());
    }

    @Override
    @Transactional
    public ParticipantResponse inviteParticipant(Long itineraryId, Long memberId, InviteParticipantRequest request) {
        getItineraryOrThrow(itineraryId);
        requireOwner(itineraryId, memberId);

        MemberSummary invitee = memberService.getSummaryByEmail(request.memberEmail());
        if (request.participantRole() == ParticipantRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_PARTICIPANT_ROLE);
        }
        if (itineraryParticipantRepository.existsByItineraryIdAndMemberId(itineraryId, invitee.id())) {
            throw new BusinessException(ErrorCode.PARTICIPANT_ALREADY_EXISTS);
        }

        ItineraryParticipant participant = ItineraryParticipant.create(itineraryId, invitee.id(), request.participantRole());
        ItineraryParticipant saved = itineraryParticipantRepository.save(participant);
        return ParticipantResponse.of(saved, invitee.nickname());
    }

    @Override
    @Transactional
    public void removeParticipant(Long itineraryId, Long memberId, Long participantId) {
        getItineraryOrThrow(itineraryId);
        requireOwner(itineraryId, memberId);

        ItineraryParticipant participant = itineraryParticipantRepository.findByIdAndItineraryId(participantId, itineraryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
        if (participant.isOwner()) {
            throw new BusinessException(ErrorCode.OWNER_PARTICIPANT_CANNOT_BE_REMOVED);
        }
        itineraryParticipantRepository.delete(participant);
    }

    @Override
    @Transactional
    public ItineraryDayResponse addDay(Long itineraryId, Long memberId, AddDayRequest request) {
        Itinerary itinerary = getItineraryOrThrow(itineraryId);
        requireEditPermission(itineraryId, memberId);

        int maxDayNumber = (int) java.time.temporal.ChronoUnit.DAYS.between(
                itinerary.getStartDate(), itinerary.getEndDate()
        ) + 1;
        LocalDate expectedDate = itinerary.getStartDate().plusDays(request.dayNumber() - 1L);
        if (request.dayNumber() > maxDayNumber || !request.date().equals(expectedDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "일자 번호와 날짜가 여행 기간에 맞지 않습니다.");
        }

        if (itineraryDayRepository.existsByItineraryIdAndDayNumber(itineraryId, request.dayNumber())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 존재하는 일자 번호입니다.");
        }

        ItineraryDay day = ItineraryDay.create(itineraryId, request.dayNumber(), request.date());
        ItineraryDay saved = itineraryDayRepository.save(day);
        return ItineraryDayResponse.of(saved, List.of());
    }

    @Override
    @Transactional
    public ItineraryItemResponse addItem(Long itineraryId, Long dayId, Long memberId, ItineraryItemRequest request) {
        getItineraryOrThrow(itineraryId);
        requireEditPermission(itineraryId, memberId);
        ItineraryDay day = getDayOrThrow(itineraryId, dayId);
        PlaceResponse place = placeService.getPlace(request.placeId());
        validateItemTime(request);
        if (itineraryItemRepository.existsByItineraryDayIdAndVisitOrder(dayId, request.visitOrder())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 사용 중인 방문 순서입니다.");
        }

        ItineraryItem item = ItineraryItem.create(
                day.getId(), request.placeId(), request.visitOrder(), request.startTime(), request.endTime(), request.memo()
        );
        ItineraryItem saved = itineraryItemRepository.save(item);
        return ItineraryItemResponse.of(saved, place.name());
    }

    @Override
    @Transactional
    public ItineraryItemResponse updateItem(Long itineraryId, Long dayId, Long itemId, Long memberId, ItineraryItemRequest request) {
        getItineraryOrThrow(itineraryId);
        requireEditPermission(itineraryId, memberId);
        getDayOrThrow(itineraryId, dayId);
        PlaceResponse place = placeService.getPlace(request.placeId());
        validateItemTime(request);
        if (itineraryItemRepository.existsByItineraryDayIdAndVisitOrderAndIdNot(dayId, request.visitOrder(), itemId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 사용 중인 방문 순서입니다.");
        }

        ItineraryItem item = itineraryItemRepository.findByIdAndItineraryDayId(itemId, dayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITINERARY_ITEM_NOT_FOUND));
        item.update(request.placeId(), request.visitOrder(), request.startTime(), request.endTime(), request.memo());

        return ItineraryItemResponse.of(item, place.name());
    }

    @Override
    @Transactional
    public void deleteItem(Long itineraryId, Long dayId, Long itemId, Long memberId) {
        getItineraryOrThrow(itineraryId);
        requireEditPermission(itineraryId, memberId);
        getDayOrThrow(itineraryId, dayId);

        ItineraryItem item = itineraryItemRepository.findByIdAndItineraryDayId(itemId, dayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITINERARY_ITEM_NOT_FOUND));
        itineraryItemRepository.delete(item);
    }

    // ---- 내부 헬퍼 ----

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    private void validateItemTime(ItineraryItemRequest request) {
        if (request.startTime() != null && request.endTime() != null
                && !request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "종료 시간은 시작 시간보다 늦어야 합니다.");
        }
    }

    private Itinerary getItineraryOrThrow(Long itineraryId) {
        return itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITINERARY_NOT_FOUND));
    }

    private ItineraryDay getDayOrThrow(Long itineraryId, Long dayId) {
        return itineraryDayRepository.findByIdAndItineraryId(dayId, itineraryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITINERARY_DAY_NOT_FOUND));
    }

    private ItineraryParticipant requireParticipant(Long itineraryId, Long memberId) {
        return itineraryParticipantRepository.findByItineraryIdAndMemberId(itineraryId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITINERARY_ACCESS_DENIED));
    }

    private void requireEditPermission(Long itineraryId, Long memberId) {
        ItineraryParticipant participant = requireParticipant(itineraryId, memberId);
        if (!participant.canEdit()) {
            throw new BusinessException(ErrorCode.ITINERARY_ACCESS_DENIED);
        }
    }

    private void requireOwner(Long itineraryId, Long memberId) {
        ItineraryParticipant participant = requireParticipant(itineraryId, memberId);
        if (!participant.isOwner()) {
            throw new BusinessException(ErrorCode.ITINERARY_ACCESS_DENIED);
        }
    }

    private ParticipantRole roleOf(List<ItineraryParticipant> participants, Long itineraryId) {
        return participants.stream()
                .filter(p -> p.getItineraryId().equals(itineraryId))
                .findFirst()
                .map(ItineraryParticipant::getParticipantRole)
                .orElse(ParticipantRole.VIEWER);
    }

    private ItineraryDetailResponse buildDetail(Itinerary itinerary, boolean includeParticipants) {
        List<ItineraryDay> days = itineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itinerary.getId());
        List<ItineraryDayResponse> dayResponses = days.stream()
                .map(day -> {
                    List<ItineraryItemResponse> items = itineraryItemRepository
                            .findByItineraryDayIdOrderByVisitOrderAsc(day.getId())
                            .stream()
                            .map(item -> ItineraryItemResponse.of(item, placeService.getPlace(item.getPlaceId()).name()))
                            .toList();
                    return ItineraryDayResponse.of(day, items);
                })
                .toList();

        List<ParticipantResponse> participantResponses = null;
        if (includeParticipants) {
            participantResponses = itineraryParticipantRepository.findByItineraryId(itinerary.getId()).stream()
                    .map(p -> ParticipantResponse.of(p, memberService.getSummary(p.getMemberId()).nickname()))
                    .toList();
        }

        return ItineraryDetailResponse.of(itinerary, dayResponses, participantResponses);
    }

    private String generateShareToken() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
