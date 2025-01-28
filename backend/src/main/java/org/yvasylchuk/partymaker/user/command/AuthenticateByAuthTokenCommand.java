package org.yvasylchuk.partymaker.user.command;

import jakarta.validation.constraints.NotEmpty;

public record AuthenticateByAuthTokenCommand(
        @NotEmpty String authToken
) {
}
