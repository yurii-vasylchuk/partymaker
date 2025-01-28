package org.yvasylchuk.partymaker.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yvasylchuk.partymaker.user.command.CreateParticipantUserCommand;
import org.yvasylchuk.partymaker.user.dto.UserDto;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserInternalController {
    private final UserService userService;

    public UserDto createGameUser(CreateParticipantUserCommand command) {
        return userService.createUser(command);
    }

    public List<UserDto> findByIdIn(Iterable<String> usersIds) {
        return userService.findByIdIn(usersIds);
    }

    public boolean isUserExists(String id) {
        return userService.isUserExists(id);
    }
}
