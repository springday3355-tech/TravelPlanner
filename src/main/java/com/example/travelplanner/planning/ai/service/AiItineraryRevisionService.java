package com.example.travelplanner.planning.ai.service;

import com.example.travelplanner.common.exception.BusinessException;
import com.example.travelplanner.common.exception.ErrorCode;
import com.example.travelplanner.itinerary.dto.ItineraryBatchChangeResult;
import com.example.travelplanner.itinerary.dto.ItineraryDetailResponse;
import com.example.travelplanner.itinerary.service.ItineraryService;
import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.service.PlaceService;
import com.example.travelplanner.planning.ai.dto.AiItineraryRevisionResponse;
import com.example.travelplanner.planning.ai.provider.AiRevisionContext;
import com.example.travelplanner.planning.ai.provider.AiRevisionProposal;
import com.example.travelplanner.planning.ai.provider.ItineraryRevisionProvider;
import com.example.travelplanner.planning.ai.provider.openai.OpenAiRevisionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiItineraryRevisionService {
    private final ItineraryService itineraryService;
    private final PlaceService placeService;
    private final ItineraryRevisionProvider provider;
    private final OpenAiRevisionProperties properties;

    public AiItineraryRevisionResponse revise(Long itineraryId, Long memberId, String instruction) {
        if (!provider.isAvailable()) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        }

        ItineraryDetailResponse current = itineraryService.getEditableDetail(itineraryId, memberId);
        List<PlaceResponse> candidates = placeService.search(current.city(), null);
        AiRevisionProposal proposal = provider.propose(
                new AiRevisionContext(
                        current.id(), current.title(), current.city(), current.startDate(), current.endDate(),
                        current.budget(), current.days(), candidates
                ),
                instruction.strip()
        );
        validateProposal(proposal);

        ItineraryBatchChangeResult result = itineraryService.applyItemChanges(
                itineraryId, memberId, proposal.commands()
        );
        return new AiItineraryRevisionResponse(
                proposal.summary(), provider.name(), provider.model(), provider.promptVersion(),
                result.changes(), result.itinerary()
        );
    }

    private void validateProposal(AiRevisionProposal proposal) {
        if (proposal == null || proposal.summary() == null || proposal.summary().isBlank()
                || proposal.commands() == null) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_FAILED,
                    "AI 일정 수정 응답에 필수값이 없습니다."
            );
        }
        if (proposal.summary().length() > 1000) {
            throw new BusinessException(
                    ErrorCode.AI_REVISION_LIMIT_EXCEEDED,
                    "AI 일정 수정 요약이 허용된 길이를 초과했습니다."
            );
        }
        if (proposal.commands().size() > properties.getMaxCommands()) {
            throw new BusinessException(
                    ErrorCode.AI_REVISION_LIMIT_EXCEEDED,
                    "한 번에 변경할 수 있는 방문 항목 수를 초과했습니다."
            );
        }
    }
}
