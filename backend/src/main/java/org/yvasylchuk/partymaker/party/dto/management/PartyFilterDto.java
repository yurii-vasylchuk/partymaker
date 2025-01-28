package org.yvasylchuk.partymaker.party.dto.management;

import org.yvasylchuk.partymaker.party.dao.PartyFilter;

public record PartyFilterDto(PartyFilter.ParticipationLevel participationLevel,
                             String text,
                             String status) {

}
