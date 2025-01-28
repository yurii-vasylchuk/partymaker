package org.yvasylchuk.partymaker.user.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateRegularUserCommand(
        @Email @NotEmpty String email,
        @NotBlank @Min(5) String username,
        @NotBlank @Min(8)String password
) {
}
