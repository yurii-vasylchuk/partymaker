package org.yvasylchuk.partymaker.game.core;

import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;

public interface DistillableForPrincipleContext<T> {
    /// Returns context, contains data available for provided user only
    /// e.g. cleared from other users' answers
    ///
    /// @param principal recipient
    /// @return Some context contains only data relevant for provided user
    /// @throws org.yvasylchuk.partymaker.exception.PartymakerException in case if there is no data for provided user
    T getDistilledContext(PartymakerPrincipal principal);
}
