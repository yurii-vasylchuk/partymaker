package org.yvasylchuk.partymaker.game.dto;

import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.game.core.Party;

import java.util.List;

public record PartyDto(
        String id,
        String name,
        String description,
        String status,
        Party.PartyContext context,
        Integer currentGameIdx,
        List<String> users
) {
    public static PartyDto of(Party party, PartymakerPrincipal principal) {
        return new PartyDto(
                party.getId(),
                party.getName(),
                party.getDescription(),
                party.getStatus().name(),
                party.getContext().getDistilledContext(principal),
                party.getCurrentGameIdx(),
                party.getUsers()
        );
    }
}
