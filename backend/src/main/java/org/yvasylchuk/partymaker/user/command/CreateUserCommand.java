package org.yvasylchuk.partymaker.user.command;

public record CreateUserCommand(
        String username,
        String token
) {
}
