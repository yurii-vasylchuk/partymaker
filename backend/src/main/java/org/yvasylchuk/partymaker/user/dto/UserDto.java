package org.yvasylchuk.partymaker.user.dto;

import org.yvasylchuk.partymaker.user.core.User;

import java.util.List;

public record UserDto(
        String id,
        String username,
        List<String> roles
) {
    public static UserDto of(User u) {
        return new UserDto(u.getId(),
                           u.getUsername(),
                           u.getRoles().stream().map(Enum::name).toList());
    }
}
