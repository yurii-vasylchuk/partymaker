package org.yvasylchuk.partymaker.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yvasylchuk.partymaker.user.command.CreateUserCommand;
import org.yvasylchuk.partymaker.user.dto.UserDto;

@Component
@RequiredArgsConstructor
public class UserInternalController {
    private final UserService userService;

    public UserDto createUser(CreateUserCommand command) {
        return userService.createUser(command);
    }

    public UserDto findById(String id) {
        return userService.findById(id);
    }
}
