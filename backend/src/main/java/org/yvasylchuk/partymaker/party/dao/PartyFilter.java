package org.yvasylchuk.partymaker.party.dao;

import lombok.Builder;
import org.yvasylchuk.partymaker.party.core.Party;

@Builder
public record PartyFilter(
        String userId,
        ParticipationLevel participationLevel,
        String text,
        Party.PartyStatus status
) {
    public enum ParticipationLevel {
        PARTICIPANT, MASTER
    }
}
