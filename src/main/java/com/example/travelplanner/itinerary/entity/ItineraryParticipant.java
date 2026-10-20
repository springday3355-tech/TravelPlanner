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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * docs/architecture/TravelPlanner-DB설계.md 2.5절과 정합.
 */
@Getter
@Entity
@Table(name = "itinerary_participant", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"itinerary_id", "member_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "itinerary_id", nullable = false)
    private Long itineraryId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false, length = 20)
    private ParticipantRole participantRole;

    private ItineraryParticipant(Long itineraryId, Long memberId, ParticipantRole participantRole) {
        this.itineraryId = itineraryId;
        this.memberId = memberId;
        this.participantRole = participantRole;
    }

    public static ItineraryParticipant create(Long itineraryId, Long memberId, ParticipantRole participantRole) {
        return new ItineraryParticipant(itineraryId, memberId, participantRole);
    }

    public boolean canEdit() {
        return participantRole == ParticipantRole.OWNER || participantRole == ParticipantRole.EDITOR;
    }

    public boolean isOwner() {
        return participantRole == ParticipantRole.OWNER;
    }
}
