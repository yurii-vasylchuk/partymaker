package org.yvasylchuk.partymaker.common.dto;

import org.yvasylchuk.partymaker.user.core.User;
import org.yvasylchuk.partymaker.user.dto.UserDto;

import java.util.List;

public record PartymakerPrincipal(
        String id,
        List<String> roles,
        String username) {
    public PartymakerPrincipal(User user) {
        this(user.getId(), user.getRoles().stream().map(Enum::name).toList(), user.getUsername());
    }

    public PartymakerPrincipal(UserDto user) {
        this(user.id(), user.roles(), user.username());
    }
}
