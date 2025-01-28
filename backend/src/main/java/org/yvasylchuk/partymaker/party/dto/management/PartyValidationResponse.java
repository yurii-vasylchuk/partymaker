package org.yvasylchuk.partymaker.party.dto.management;

import org.yvasylchuk.partymaker.party.core.Party;

import java.util.List;

public record PartyValidationResponse(
        boolean valid,
        List<Party.PartyConfigurationViolation> violations
) {
    public PartyValidationResponse(List<Party.PartyConfigurationViolation> violations) {
        this(violations == null || violations.isEmpty(), violations);
    }
}
