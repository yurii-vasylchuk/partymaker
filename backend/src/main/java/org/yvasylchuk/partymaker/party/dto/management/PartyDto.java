package org.yvasylchuk.partymaker.party.dto.management;

import org.yvasylchuk.partymaker.party.core.Party;

import java.util.List;

public record PartyDto(
        String id,
        String name,
        String description,
        String status,
        String master,
        Party.PartyContext context,
        Integer currentGameIdx,
        Integer gamesCount,
        List<PartyParticipantDto> users,
        List<GameDto> games
) {
}
