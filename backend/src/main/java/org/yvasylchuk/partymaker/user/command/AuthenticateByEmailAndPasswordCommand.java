package org.yvasylchuk.partymaker.user.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record AuthenticateByEmailAndPasswordCommand(
        @Email @NotEmpty String email,
        @NotEmpty String password
) {
}
